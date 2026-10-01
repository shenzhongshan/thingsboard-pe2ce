/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.snmp.service;

import lombok.RequiredArgsConstructor;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.Optional;

@Service
@TbSnmpTransportComponent
@RequiredArgsConstructor
public class SnmpAuthService {
    private final SnmpTransportService snmpTransportService;

    @Value("${transport.snmp.underlying_protocol}")
    private String snmpUnderlyingProtocol;

    public Target setUpSnmpTarget(SnmpDeviceProfileTransportConfiguration profileTransportConfig, SnmpDeviceTransportConfiguration deviceTransportConfig) {
        AbstractTarget target;

        SnmpProtocolVersion protocolVersion = deviceTransportConfig.getProtocolVersion();
        switch (protocolVersion) {
            case V1:
                CommunityTarget communityTargetV1 = new CommunityTarget();
                communityTargetV1.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv1);
                communityTargetV1.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                communityTargetV1.setCommunity(new OctetString(deviceTransportConfig.getCommunity()));
                target = communityTargetV1;
                break;
            case V2C:
                CommunityTarget communityTargetV2 = new CommunityTarget();
                communityTargetV2.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv2c);
                communityTargetV2.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                communityTargetV2.setCommunity(new OctetString(deviceTransportConfig.getCommunity()));
                target = communityTargetV2;
                break;
            case V3:
                OctetString username = new OctetString(deviceTransportConfig.getUsername());
                OctetString securityName = new OctetString(deviceTransportConfig.getSecurityName());
                OctetString engineId = parseEngineId(deviceTransportConfig.getEngineId());

                OID authenticationProtocol = new OID(deviceTransportConfig.getAuthenticationProtocol().getOid());
                OctetString authenticationPassphrase = Optional.ofNullable(SecurityProtocols.getInstance().passwordToKey(authenticationProtocol,
                                new OctetString(deviceTransportConfig.getAuthenticationPassphrase()), engineId.getValue()))
                        .map(OctetString::new)
                        .orElseThrow(() -> new UnsupportedOperationException("Authentication protocol " + deviceTransportConfig.getAuthenticationProtocol() + " is not supported"));

                OID privacyProtocol = new OID(deviceTransportConfig.getPrivacyProtocol().getOid());
                OctetString privacyPassphrase = Optional.ofNullable(SecurityProtocols.getInstance().passwordToKey(privacyProtocol,
                                authenticationProtocol, new OctetString(deviceTransportConfig.getPrivacyPassphrase()), engineId.getValue()))
                        .map(OctetString::new)
                        .orElseThrow(() -> new UnsupportedOperationException("Privacy protocol " + deviceTransportConfig.getPrivacyProtocol() + " is not supported"));

                USM usm = snmpTransportService.getSnmp().getUSM();
                if (usm.hasUser(engineId, securityName)) {
                    usm.removeAllUsers(username, engineId);
                }
                UsmUser usmUser = new UsmUser(username, authenticationProtocol, authenticationPassphrase, privacyProtocol, privacyPassphrase, engineId);
                usm.addUser(username, engineId, usmUser);

                UserTarget userTarget = new UserTarget();
                userTarget.setSecurityName(securityName);
                userTarget.setAuthoritativeEngineID(engineId.getValue());
                userTarget.setSecurityModel(SecurityModel.SECURITY_MODEL_USM);
                userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);
                target = userTarget;
                break;
            default:
                throw new UnsupportedOperationException("SNMP protocol version " + protocolVersion + " is not supported");
        }

        Address address = GenericAddress.parse(snmpUnderlyingProtocol + ":" + deviceTransportConfig.getHost() + "/" + deviceTransportConfig.getPort());
        target.setAddress(Optional.ofNullable(address).orElseThrow(() -> new IllegalArgumentException("Address of the SNMP device is invalid")));
        target.setTimeout(profileTransportConfig.getTimeoutMs());
        target.setRetries(profileTransportConfig.getRetries());
        target.setVersion(protocolVersion.getCode());

        return target;
    }

    public void cleanUpSnmpAuthInfo(DeviceSessionContext sessionContext) {
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = sessionContext.getDeviceTransportConfiguration();
        if (deviceTransportConfiguration.getProtocolVersion() == SnmpProtocolVersion.V3) {
            OctetString username = new OctetString(deviceTransportConfiguration.getUsername());
            OctetString engineId = parseEngineId(deviceTransportConfiguration.getEngineId());
            snmpTransportService.getSnmp().getUSM().removeAllUsers(username, engineId);
        }
    }

    /**
     * Accepts both the plain hex format SNMP4J expects ("8000137001c0a8...") and the
     * colon/space/0x-delimited hex formats most real SNMP tooling prints engine IDs in
     * ("80:00:13:70:01:c0:a8...", "0x8000137001c0a8..."), normalizing to the former before
     * delegating to SNMP4J. A genuinely malformed value fails with a clear validation message
     * instead of an unhandled NumberFormatException.
     */
    private static OctetString parseEngineId(String engineId) {
        String normalized = engineId.replaceAll("(?i)^0x|[:\\s-]", "");
        try {
            return OctetString.fromString(normalized, 16);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("SNMP V3 engine ID is not a valid hex string: '" + engineId + "'", e);
        }
    }

}
