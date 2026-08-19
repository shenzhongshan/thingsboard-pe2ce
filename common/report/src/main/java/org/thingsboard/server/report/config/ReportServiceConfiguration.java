/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.context.annotation.Configuration
 *  org.thingsboard.common.util.SsrfProtectionValidator
 */
package org.thingsboard.server.report.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.common.util.SsrfProtectionValidator;

@Configuration
@ConditionalOnProperty(name={"service.type"}, havingValue="tb-report")
public class ReportServiceConfiguration {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ReportServiceConfiguration.class);
    @Value(value="${reports.ssrf_protection_enabled:false}")
    private boolean ssrfProtectionEnabled;
    @Value(value="${reports.ssrf_additional_blocked_hosts:}")
    private List<String> ssrfAdditionalBlockedHosts;
    @Value(value="${reports.ssrf_allowed_hosts:}")
    private List<String> ssrfAllowedHosts;

    @PostConstruct
    public void init() {
        SsrfProtectionValidator.setEnabled((boolean)this.ssrfProtectionEnabled);
        SsrfProtectionValidator.setAdditionalBlockedHosts(this.ssrfAdditionalBlockedHosts);
        SsrfProtectionValidator.setAllowedHosts(this.ssrfAllowedHosts);
        if (!this.ssrfProtectionEnabled) {
            log.warn("SSRF protection for report service is DISABLED. This allows report generation to access internal/private network addresses including cloud metadata endpoints. It is strongly recommended to enable SSRF protection by setting SSRF_PROTECTION_ENABLED=true. If your reports need to access resources on local networks, use SSRF_ALLOWED_HOSTS to whitelist specific addresses or ranges.");
        }
    }
}

