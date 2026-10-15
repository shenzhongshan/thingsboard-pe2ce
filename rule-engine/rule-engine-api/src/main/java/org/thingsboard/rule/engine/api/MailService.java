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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.mail.javamail.JavaMailSender;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

public interface MailService {

    void updateMailConfiguration();

    void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException;

    /**
     * @param tenantId the tenant whose settings are being tested, {@code SYS_TENANT_ID} for the
     *                 system mail server. Scopes {@code ${secret:...}} resolution inside the config.
     */
    void sendTestMail(TenantId tenantId, JsonNode config, String email) throws ThingsboardException;

    /*
     * The user-facing account mails below are tenant-scoped: {@code tenantId} selects both the mail
     * sender (tenant SMTP settings, falling back to the system ones) and the MAIL_TEMPLATES white
     * labeling used for subject/body. Pass {@code SYS_TENANT_ID} for system-level mail.
     */

    void sendActivationEmail(TenantId tenantId, String activationLink, long ttlMs, String email) throws ThingsboardException;

    void sendAccountActivatedEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException;

    void sendResetPasswordEmail(TenantId tenantId, String passwordResetLink, long ttlMs, String email) throws ThingsboardException;

    void sendResetPasswordEmailAsync(TenantId tenantId, String passwordResetLink, long ttlMs, String email);

    void sendPasswordWasResetEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException;

    void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException;

    void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException;

    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException;

    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException;

    void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException;

    void testConnection(TenantId tenantId) throws Exception;

    boolean isConfigured(TenantId tenantId);

}
