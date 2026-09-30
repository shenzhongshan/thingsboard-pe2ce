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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response body of the reCAPTCHA siteverify endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecaptchaValidationResult {

    private final boolean success;
    private final List<RecaptchaErrorCode> errorCodes;

    @JsonCreator
    public RecaptchaValidationResult(@JsonProperty("success") boolean success,
                                     @JsonProperty("error-codes") List<RecaptchaErrorCode> errorCodes) {
        this.success = success;
        this.errorCodes = errorCodes == null ? new ArrayList<>() : errorCodes;
    }

    public boolean isSuccess() {
        return success;
    }

    @JsonIgnore
    public boolean isFailure() {
        return !success;
    }

    public List<RecaptchaErrorCode> getErrorCodes() {
        return Collections.unmodifiableList(errorCodes);
    }

    public boolean hasError(RecaptchaErrorCode error) {
        return errorCodes.contains(error);
    }

    @Override
    public String toString() {
        return "RecaptchaValidationResult{success=" + success + ", errorCodes=" + errorCodes + "}";
    }
}
