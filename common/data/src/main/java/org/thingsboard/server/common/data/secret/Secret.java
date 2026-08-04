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
package org.thingsboard.server.common.data.secret;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.SecretId;

import java.util.Base64;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema
public class Secret extends SecretInfo {

    @Schema(description = "Secret value. Write-only: never returned by the API.", example = "SecretValue")
    private String value;

    @JsonIgnore
    private byte[] encryptedValue;

    public Secret(SecretId id) {
        super(id);
    }

    public Secret(Secret secret) {
        super(secret);
        this.value = secret.getValue();
        this.encryptedValue = secret.getEncryptedValue();
    }

    public Secret(SecretInfo secretInfo) {
        super(secretInfo);
        this.value = null;
        this.encryptedValue = null;
    }

    public Secret(SecretInfo secretInfo, byte[] encryptedValue) {
        super(secretInfo);
        this.encryptedValue = encryptedValue;
    }

    @JsonGetter("encryptedValue")
    public String getEncryptedValueBase64() {
        return encryptedValue != null ? Base64.getEncoder().encodeToString(encryptedValue) : null;
    }

    @JsonSetter("encryptedValue")
    public void setEncryptedValueBase64(String value) {
        this.encryptedValue = value != null ? Base64.getDecoder().decode(value) : null;
    }

}
