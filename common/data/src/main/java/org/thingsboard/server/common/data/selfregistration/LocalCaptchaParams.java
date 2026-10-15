package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.oauth2.PlatformType;

/**
 * Self-hosted, server-generated image captcha - an alternative to Google reCAPTCHA for deployments
 * that cannot reliably reach Google's captcha services. Carries no configuration: which underlying
 * library actually renders the image (Kaptcha, Hutool, ...) is a system-wide deployment choice
 * (see {@code security.captcha.local.provider} in thingsboard.yml), invisible to the tenant.
 */
@Schema
public class LocalCaptchaParams
implements CaptchaParams {

    @Override
    public String getVersion() {
        return CaptchaVersion.LOCAL.getName();
    }

    @Override
    public CaptchaParams toInfo(PlatformType platformType) {
        return this; // no secret to strip
    }
}
