package org.thingsboard.server.common.data.selfregistration;

import java.util.List;
import org.thingsboard.server.common.data.selfregistration.CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.SignUpField;

public record SignUpSelfRegistrationParams(String title, CaptchaParams captcha, List<SignUpField> fields, Boolean showPrivacyPolicy, Boolean showTermsOfUse) {
}

