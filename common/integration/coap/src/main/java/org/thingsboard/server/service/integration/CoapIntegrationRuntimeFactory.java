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
package org.thingsboard.server.service.integration;

import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Set;

public class CoapIntegrationRuntimeFactory implements IntegrationRuntimeFactory {

    /** Null when this process runs no CoAP server; the runtime then only supports legacy port mode. */
    private final CoapServerService coapServerService;

    public CoapIntegrationRuntimeFactory(CoapServerService coapServerService) {
        this.coapServerService = coapServerService;
    }

    @Override
    public Set<IntegrationType> getSupportedTypes() {
        return Set.of(IntegrationType.COAP);
    }

    @Override
    public CoapIntegrationServer create(Integration integration, IntegrationUplinkSink uplinkSink) {
        return new CoapIntegrationServer(integration, uplinkSink, coapServerService);
    }

}
