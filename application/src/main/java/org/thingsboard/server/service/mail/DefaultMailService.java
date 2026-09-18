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
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.RateLimitExceededException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMailService implements MailService {

    private static final String TARGET_EMAIL = "targetEmail";
    private static final String UTF_8 = "UTF-8";
    private static final long DEFAULT_TIMEOUT = 10_000;

    private final ScheduledExecutorService timeoutScheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("mail-service-watchdog");

    private final MessageSource messages;
    private final Configuration freemarkerConfig;
    private final AdminSettingsService adminSettingsService;
    private final TbApiUsageReportClient apiUsageClient;
    @Lazy
    private final TbApiUsageStateService apiUsageStateService;
    private final MailSenderInternalExecutorService mailExecutorService;
    private final PasswordResetExecutorService passwordResetExecutorService;
    private final TbMailContextComponent ctx;
    private final RateLimitService rateLimitService;
    @Lazy
    private final org.thingsboard.server.dao.wl.WhiteLabelingService whiteLabelingService;

    @Value("${mail.per_tenant_rate_limits:}")
    private String perTenantRateLimitConfig;

    @Value("${actors.rule.allow_system_mail_service:true}")
    private boolean allowSystemMailService;

    private TbMailSender mailSender;

    private String mailFrom;

    private long timeout;

    /** The mail configuration actually in force for a tenant, and whether it is the system one. */
    private record ConfigEntry(JsonNode jsonConfig, boolean isSystem) {}

    /**
     * Resolves which mail server a tenant's outgoing mail goes through.
     *
     * <p>A tenant may keep its own SMTP configuration under its own {@code mail} admin-settings row.
     * The row carries a {@code useSystemMailSettings} flag: when it is absent or true the tenant has
     * opted back into the platform's mail server, so the tenant row is ignored entirely. This
     * mirrors PE's {@code DefaultMailService#getConfig}.
     *
     * @param allowSystem whether falling back to the system mail server is permitted for this call
     *                    site (rule-engine sends honour {@code actors.rule.allow_system_mail_service})
     */
    private ConfigEntry getConfig(TenantId tenantId, boolean allowSystem) throws ThingsboardException {
        try {
            JsonNode jsonConfig = null;
            if (tenantId != null && !tenantId.isNullUid() && !tenantId.isSysTenantId()) {
                AdminSettings tenantSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, "mail");
                if (tenantSettings != null) {
                    jsonConfig = tenantSettings.getJsonValue();
                    JsonNode useSystem = jsonConfig.get("useSystemMailSettings");
                    if (useSystem == null || useSystem.asBoolean()) {
                        jsonConfig = null;
                    }
                }
            }
            boolean isSystem = false;
            if (jsonConfig == null) {
                if (!allowSystem) {
                    throw new ThingsboardException("Access to System Mail Service is forbidden!", ThingsboardErrorCode.GENERAL);
                }
                AdminSettings systemSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
                if (systemSettings != null) {
                    jsonConfig = systemSettings.getJsonValue();
                    isSystem = true;
                }
            }
            if (jsonConfig == null) {
                throw new IncorrectParameterException("Failed to get mail configuration. Settings not found!");
            }
            return new ConfigEntry(jsonConfig, isSystem);
        } catch (ThingsboardException e) {
            throw e;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /** The sender + envelope for a tenant, built fresh per send the way PE does. */
    private TbMailSender senderFor(ConfigEntry entry, TenantId tenantId) {
        // secrets and OAuth2 refresh tokens must resolve against whichever tenant owns the config
        return new TbMailSender(ctx, entry.isSystem() ? TenantId.SYS_TENANT_ID : tenantId, entry.jsonConfig());
    }

    private String mailFromOf(JsonNode jsonConfig) {
        JsonNode node = jsonConfig.get("mailFrom");
        return node == null ? mailFrom : node.asText();
    }

    private long timeoutOf(JsonNode jsonConfig) {
        JsonNode node = jsonConfig.get("timeout");
        return node == null ? DEFAULT_TIMEOUT : node.asLong(DEFAULT_TIMEOUT);
    }

    @PostConstruct
    private void init() {
        updateMailConfiguration();
    }

    @PreDestroy
    public void destroy() {
        timeoutScheduler.shutdownNow();
    }

    /**
     * Rebuilds the cached <b>system</b> sender. Tenant-scoped sends resolve their configuration per
     * call in {@link #getConfig(TenantId, boolean)} instead, so a tenant saving its own mail
     * settings does not go through here and cannot disturb the system sender.
     */
    @Override
    public void updateMailConfiguration() {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
        if (settings != null) {
            JsonNode jsonConfig = settings.getJsonValue();
            mailSender = new TbMailSender(ctx, TenantId.SYS_TENANT_ID, jsonConfig);
            mailFrom = mailFromOf(jsonConfig);
            timeout = timeoutOf(jsonConfig);
        } else {
            throw new IncorrectParameterException("Failed to update mail configuration. Settings not found!");
        }
    }

    @Override
    public void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException {
        ConfigEntry entry = getConfig(tenantId, true);
        JsonNode jsonConfig = entry.jsonConfig();
        sendMail(senderFor(entry, tenantId), mailFromOf(jsonConfig), email, subject, message, timeoutOf(jsonConfig));
    }

    @Override
    public void sendTestMail(TenantId tenantId, JsonNode jsonConfig, String email) throws ThingsboardException {
        // built from the config the caller is testing, not from what is persisted - but still
        // scoped to the tenant so ${secret:...} placeholders in it resolve against their owner
        TbMailSender testMailSender = new TbMailSender(ctx, tenantId, jsonConfig);
        String mailFrom = mailFromOf(jsonConfig);
        String subject = messages.getMessage("test.message.subject", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put(TARGET_EMAIL, email);

        String message = mergeTemplateIntoString("test.ftl", model);

        sendMail(testMailSender, mailFrom, email, subject, message, timeoutOf(jsonConfig));
    }

    @Override
    public void sendActivationEmail(String activationLink, long ttlMs, String email) throws ThingsboardException {
        Map<String, Object> model = new HashMap<>();
        model.put("activationLink", activationLink);
        model.put("activationLinkTtlInHours", (int) Math.ceil(ttlMs / 3600000.0));
        model.put(TARGET_EMAIL, email);

        String subject = wlSubject(TenantId.SYS_TENANT_ID, MailTemplates.ACTIVATION, messages.getMessage("activation.subject", null, Locale.US));
        String message = wlBody(TenantId.SYS_TENANT_ID, MailTemplates.ACTIVATION, "activation.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendAccountActivatedEmail(String loginLink, String email) throws ThingsboardException {

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String subject = wlSubject(TenantId.SYS_TENANT_ID, MailTemplates.ACCOUNT_ACTIVATED, messages.getMessage("account.activated.subject", null, Locale.US));
        String message = wlBody(TenantId.SYS_TENANT_ID, MailTemplates.ACCOUNT_ACTIVATED, "account.activated.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendResetPasswordEmail(String passwordResetLink, long ttlMs, String email) throws ThingsboardException {

        Map<String, Object> model = new HashMap<>();
        model.put("passwordResetLink", passwordResetLink);
        model.put("passwordResetLinkTtlInHours", (int) Math.ceil(ttlMs / 3600000.0));
        model.put(TARGET_EMAIL, email);

        String subject = wlSubject(TenantId.SYS_TENANT_ID, MailTemplates.RESET_PASSWORD, messages.getMessage("reset.password.subject", null, Locale.US));
        String message = wlBody(TenantId.SYS_TENANT_ID, MailTemplates.RESET_PASSWORD, "reset.password.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendResetPasswordEmailAsync(String passwordResetLink, long ttlMs, String email) {
        passwordResetExecutorService.execute(() -> {
            try {
                this.sendResetPasswordEmail(passwordResetLink, ttlMs, email);
            } catch (Exception e) {
                log.error("Error occurred: {} ", e.getMessage());
            }
        });
    }

    @Override
    public void sendPasswordWasResetEmail(String loginLink, String email) throws ThingsboardException {

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String subject = wlSubject(TenantId.SYS_TENANT_ID, MailTemplates.PASSWORD_WAS_RESET, messages.getMessage("password.was.reset.subject", null, Locale.US));
        String message = wlBody(TenantId.SYS_TENANT_ID, MailTemplates.PASSWORD_WAS_RESET, "password.was.reset.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException {
        // the rule-engine "send email" node: a tenant with its own mail server uses it, and whether
        // it may otherwise borrow the platform's one is what allow_system_mail_service governs
        ConfigEntry entry = getConfig(tenantId, allowSystemMailService);
        JsonNode jsonConfig = entry.jsonConfig();
        sendMail(tenantId, customerId, tbEmail, senderFor(entry, tenantId), mailFromOf(jsonConfig), timeoutOf(jsonConfig));
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException {
        // caller supplied its own SMTP connection, so no settings lookup - only the envelope's
        // default "from" still comes from whatever configuration applies to this tenant
        sendMail(tenantId, customerId, tbEmail, javaMailSender, mailFromOf(getConfig(tenantId, true).jsonConfig()), timeout);
    }

    private void sendMail(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender,
                          String mailFrom, long timeout) throws ThingsboardException {
        if (apiUsageStateService.getApiUsageState(tenantId).isEmailSendEnabled()) {
            if (tenantId != null && !tenantId.isSysTenantId() && StringUtils.isNotEmpty(perTenantRateLimitConfig) &&
                    !rateLimitService.checkRateLimit(LimitedApi.EMAILS, (Object) tenantId, perTenantRateLimitConfig)) {
                throw new RateLimitExceededException(LimitedApi.EMAILS);
            }
            try {
                MimeMessage mailMsg = javaMailSender.createMimeMessage();
                boolean multipart = (tbEmail.getImages() != null && !tbEmail.getImages().isEmpty());
                MimeMessageHelper helper = new MimeMessageHelper(mailMsg, multipart, "UTF-8");
                helper.setFrom(StringUtils.isBlank(tbEmail.getFrom()) ? mailFrom : tbEmail.getFrom());
                helper.setTo(tbEmail.getTo().split("\\s*,\\s*"));
                if (!StringUtils.isBlank(tbEmail.getCc())) {
                    helper.setCc(tbEmail.getCc().split("\\s*,\\s*"));
                }
                if (!StringUtils.isBlank(tbEmail.getBcc())) {
                    helper.setBcc(tbEmail.getBcc().split("\\s*,\\s*"));
                }
                helper.setSubject(tbEmail.getSubject());
                helper.setText(tbEmail.getBody(), tbEmail.isHtml());

                if (multipart) {
                    for (String imgId : tbEmail.getImages().keySet()) {
                        String imgValue = tbEmail.getImages().get(imgId);
                        String value = imgValue.replaceFirst("^data:image/[^;]*;base64,?", "");
                        byte[] bytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(value);
                        String contentType = helper.getFileTypeMap().getContentType(imgId);
                        InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                        helper.addInline(imgId, iss, contentType);
                    }
                }
                sendMailWithTimeout(javaMailSender, helper.getMimeMessage(), timeout);
                apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.EMAIL_EXEC_COUNT, 1);
            } catch (Exception e) {
                throw handleException(e);
            }
        } else {
            throw new RuntimeException("Email sending is disabled due to API limits!");
        }
    }

    @Override
    public void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException {
        Map<String, Object> model = new HashMap<>();
        model.put("lockoutAccount", lockoutEmail);
        model.put("maxFailedLoginAttempts", maxFailedLoginAttempts);
        model.put(TARGET_EMAIL, email);

        String subject = wlSubject(TenantId.SYS_TENANT_ID, MailTemplates.ACCOUNT_LOCKOUT, messages.getMessage("account.lockout.subject", null, Locale.US));
        String message = wlBody(TenantId.SYS_TENANT_ID, MailTemplates.ACCOUNT_LOCKOUT, "account.lockout.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException {
        Map<String, Object> model = Map.of(
                TARGET_EMAIL, email,
                "code", verificationCode,
                "expirationTimeSeconds", expirationTimeSeconds
        );

        String subject = wlSubject(TenantId.SYS_TENANT_ID, MailTemplates.TWO_FA_VERIFICATION, messages.getMessage("2fa.verification.code.subject", null, Locale.US));
        String message = wlBody(TenantId.SYS_TENANT_ID, MailTemplates.TWO_FA_VERIFICATION, "2fa.verification.code.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException {
        String subject = messages.getMessage("api.usage.state", null, Locale.US);

        Map<String, Object> model = new HashMap<>();
        model.put("apiFeature", apiFeature.getLabel());
        model.put(TARGET_EMAIL, email);

        String message = switch (stateValue) {
            case ENABLED -> {
                model.put("apiLabel", toEnabledValueLabel(apiFeature));
                yield mergeTemplateIntoString("state.enabled.ftl", model);
            }
            case WARNING -> {
                model.put("apiValueLabel", toDisabledValueLabel(apiFeature) + " " + toWarningValueLabel(recordState));
                yield mergeTemplateIntoString("state.warning.ftl", model);
            }
            case DISABLED -> {
                model.put("apiLimitValueLabel", toDisabledValueLabel(apiFeature) + " " + toDisabledValueLabel(recordState));
                yield mergeTemplateIntoString("state.disabled.ftl", model);
            }
        };

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void testConnection(TenantId tenantId) throws Exception {
        ConfigEntry entry = getConfig(tenantId, true);
        senderFor(entry, tenantId).testConnection();
    }

    @Override
    public boolean isConfigured(TenantId tenantId) {
        try {
            ConfigEntry entry = getConfig(tenantId, allowSystemMailService);
            senderFor(entry, tenantId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String toEnabledValueLabel(ApiFeature apiFeature) {
        return switch (apiFeature) {
            case DB -> "save";
            case TRANSPORT -> "receive";
            case JS -> "invoke";
            case RE -> "process";
            case EMAIL, SMS -> "send";
            case ALARM -> "create";
            default -> throw new RuntimeException("Not implemented!");
        };
    }

    private String toDisabledValueLabel(ApiFeature apiFeature) {
        return switch (apiFeature) {
            case DB -> "saved";
            case TRANSPORT -> "received";
            case JS -> "invoked";
            case RE -> "processed";
            case EMAIL, SMS -> "sent";
            case ALARM -> "created";
            default -> throw new RuntimeException("Not implemented!");
        };
    }

    private String toWarningValueLabel(ApiUsageRecordState recordState) {
        String valueInM = recordState.getValueAsString();
        String thresholdInM = recordState.getThresholdAsString();
        return switch (recordState.getKey()) {
            case STORAGE_DP_COUNT, TRANSPORT_DP_COUNT -> valueInM + " out of " + thresholdInM + " allowed data points";
            case TRANSPORT_MSG_COUNT -> valueInM + " out of " + thresholdInM + " allowed messages";
            case JS_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed JavaScript functions";
            case TBEL_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed Tbel functions";
            case RE_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed Rule Engine messages";
            case EMAIL_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed Email messages";
            case SMS_EXEC_COUNT -> valueInM + " out of " + thresholdInM + " allowed SMS messages";
            default -> throw new RuntimeException("Not implemented!");
        };
    }

    private String toDisabledValueLabel(ApiUsageRecordState recordState) {
        return switch (recordState.getKey()) {
            case STORAGE_DP_COUNT, TRANSPORT_DP_COUNT -> recordState.getValueAsString() + " data points";
            case TRANSPORT_MSG_COUNT -> recordState.getValueAsString() + " messages";
            case JS_EXEC_COUNT -> "JavaScript functions " + recordState.getValueAsString() + " times";
            case TBEL_EXEC_COUNT -> "TBEL functions " + recordState.getValueAsString() + " times";
            case RE_EXEC_COUNT -> recordState.getValueAsString() + " Rule Engine messages";
            case EMAIL_EXEC_COUNT -> recordState.getValueAsString() + " Email messages";
            case SMS_EXEC_COUNT -> recordState.getValueAsString() + " SMS messages";
            default -> throw new RuntimeException("Not implemented!");
        };
    }

    private void sendMail(JavaMailSenderImpl mailSender, String mailFrom, String email,
                          String subject, String message, long timeout) throws ThingsboardException {
        try {
            MimeMessage mimeMsg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, UTF_8);
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message, true);

            sendMailWithTimeout(mailSender, helper.getMimeMessage(), timeout);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void sendMailWithTimeout(JavaMailSender mailSender, MimeMessage msg, long timeout) throws ThingsboardException {
        var submittedMail = Futures.withTimeout(
                mailExecutorService.submit(() -> mailSender.send(msg)),
                timeout, TimeUnit.MILLISECONDS, timeoutScheduler);
        try {
            submittedMail.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout!");
        } catch (Exception e) {
            throw new ThingsboardException("Unable to send mail", ExceptionUtils.getRootCause(e), ThingsboardErrorCode.GENERAL);
        }
    }

    private String mergeTemplateIntoString(String templateLocation,
                                           Map<String, Object> model) throws ThingsboardException {
        try {
            Template template = freemarkerConfig.getTemplate(templateLocation);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (Exception e) {
            log.warn("Failed to process mail template: {}", ExceptionUtils.getRootCauseMessage(e));
            throw new ThingsboardException("Failed to process mail template: " + e.getMessage(), e, ThingsboardErrorCode.GENERAL);
        }
    }

    // White labeling mail templates: use the tenant/system MAIL_TEMPLATES white-labeling
    // configuration when present, otherwise fall back to the built-in .ftl / message-source defaults.
    private JsonNode wlMailTemplates(TenantId tenantId) {
        try {
            return whiteLabelingService.getMergedTenantMailTemplates(tenantId == null ? TenantId.SYS_TENANT_ID : tenantId);
        } catch (Exception e) {
            return null;
        }
    }

    private String wlSubject(TenantId tenantId, String templateKey, String defaultSubject) {
        JsonNode templates = wlMailTemplates(tenantId);
        try {
            JsonNode node = templates == null ? null : MailTemplates.getTemplate(templates, templateKey);
            if (node != null && node.hasNonNull(MailTemplates.SUBJECT)) {
                return MailTemplates.subject(templates, templateKey);
            }
        } catch (Exception e) {
            log.debug("Failed to resolve white-labeling subject for '{}': {}", templateKey, e.getMessage());
        }
        return defaultSubject;
    }

    private String wlBody(TenantId tenantId, String templateKey, String ftlFallback, Map<String, Object> model) throws ThingsboardException {
        JsonNode templates = wlMailTemplates(tenantId);
        try {
            JsonNode node = templates == null ? null : MailTemplates.getTemplate(templates, templateKey);
            if (node != null && node.hasNonNull(MailTemplates.BODY)) {
                return MailTemplates.body(templates, templateKey, model);
            }
        } catch (Exception e) {
            log.debug("Failed to resolve white-labeling body for '{}': {}", templateKey, e.getMessage());
        }
        return mergeTemplateIntoString(ftlFallback, model);
    }

    protected ThingsboardException handleException(Throwable exception) {
        if (exception instanceof ThingsboardException thingsboardException) {
            return thingsboardException;
        }
        if (exception instanceof NestedRuntimeException) {
            exception = ((NestedRuntimeException) exception).getMostSpecificCause();
        }
        log.warn("Unable to send mail: {}", exception.getMessage());
        return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
    }

}
