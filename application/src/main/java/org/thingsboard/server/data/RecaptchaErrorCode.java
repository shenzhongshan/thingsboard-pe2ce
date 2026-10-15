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
package org.thingsboard.server.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The {@code error-codes} entries returned by the reCAPTCHA siteverify endpoint.
 */
public enum RecaptchaErrorCode {

    MISSING_SECRET_KEY("missing-input-secret"),
    INVALID_SECRET_KEY("invalid-input-secret"),
    MISSING_USER_CAPTCHA_RESPONSE("missing-input-response"),
    INVALID_USER_CAPTCHA_RESPONSE("invalid-input-response"),
    MISSING_USERNAME_REQUEST_PARAMETER("missing-username-request-parameter"),
    MISSING_CAPTCHA_RESPONSE_PARAMETER("missing-captcha-response-parameter"),
    VALIDATION_HTTP_ERROR("validation-http-error");

    private final String text;

    RecaptchaErrorCode(String text) {
        this.text = text;
    }

    /**
     * Unknown codes degrade to {@link #VALIDATION_HTTP_ERROR} rather than throwing: Google returns
     * codes outside this list (e.g. {@code timeout-or-duplicate}, {@code bad-request}), and a throw
     * here would surface from inside Jackson as an opaque 500 instead of the intended
     * "Invalid reCaptcha response!" validation error.
     */
    @JsonCreator
    static RecaptchaErrorCode fromValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "missing-input-secret" -> MISSING_SECRET_KEY;
            case "invalid-input-secret" -> INVALID_SECRET_KEY;
            case "missing-input-response" -> MISSING_USER_CAPTCHA_RESPONSE;
            case "invalid-input-response" -> INVALID_USER_CAPTCHA_RESPONSE;
            case "missing-username-request-parameter" -> MISSING_USERNAME_REQUEST_PARAMETER;
            case "missing-captcha-response-parameter" -> MISSING_CAPTCHA_RESPONSE_PARAMETER;
            default -> VALIDATION_HTTP_ERROR;
        };
    }

    @JsonValue
    public String getText() {
        return this.text;
    }
}
