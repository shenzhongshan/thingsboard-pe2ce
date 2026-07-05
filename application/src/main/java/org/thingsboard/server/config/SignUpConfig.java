package org.thingsboard.server.config;

import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value={"classpath:recaptcha.properties"})
public class SignUpConfig {
    @Value(value="${recaptcha.verification_url}")
    private String recaptchaVerificationUrl;

    @Generated
    public String getRecaptchaVerificationUrl() {
        return this.recaptchaVerificationUrl;
    }

    @Generated
    public void setRecaptchaVerificationUrl(String recaptchaVerificationUrl) {
        this.recaptchaVerificationUrl = recaptchaVerificationUrl;
    }
}

