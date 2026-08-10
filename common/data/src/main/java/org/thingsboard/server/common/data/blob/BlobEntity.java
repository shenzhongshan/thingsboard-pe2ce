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
package org.thingsboard.server.common.data.blob;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.BlobEntityId;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Optional;

/**
 * A stored binary blob together with its payload. The payload is transported as base64 over
 * JSON but held as a {@link ByteBuffer} in memory.
 */
@Schema
@EqualsAndHashCode(callSuper = true)
public class BlobEntity extends BlobEntityInfo {

    private static final long serialVersionUID = 2807343057809549363L;

    @Getter @Setter
    private transient ByteBuffer data;

    public BlobEntity() {
        super();
    }

    public BlobEntity(BlobEntityId id) {
        super(id);
    }

    public BlobEntity(BlobEntity blobEntity) {
        super(blobEntity);
        this.data = blobEntity.getData();
    }

    @Schema(description = "Base64-encoded blob entity data", accessMode = Schema.AccessMode.READ_WRITE)
    @JsonGetter("data")
    public String getEncodedData() {
        return Optional.ofNullable(data).map(ByteBuffer::array).map(Base64.getEncoder()::encodeToString).orElse(null);
    }

    @JsonSetter("data")
    public void setEncodedData(String data) {
        this.data = Optional.ofNullable(data).map(Base64.getDecoder()::decode).map(ByteBuffer::wrap).orElse(null);
    }

    @Override
    public String toString() {
        return "BlobEntity [name=" + getName() + ", type=" + getType() + ", contentType=" + getContentType() + "]";
    }

}
