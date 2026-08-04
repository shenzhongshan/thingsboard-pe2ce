--
-- Copyright © 2016-2026 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- CALCULATED FIELD ADDITIONAL INFO ADDITION START

ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS additional_info varchar;

-- CALCULATED FIELD ADDITIONAL INFO ADDITION END

-- RULE CHAIN NOTES MIGRATION START

ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS notes varchar(1000000);

-- RULE CHAIN NOTES MIGRATION END

-- SECRET TABLE ADDITION START

CREATE TABLE IF NOT EXISTS secret (
    id           UUID          NOT NULL CONSTRAINT secret_pkey PRIMARY KEY,
    created_time BIGINT        NOT NULL,
    tenant_id    UUID          NOT NULL,
    name         VARCHAR(255)  NOT NULL,
    type         VARCHAR(32)   NOT NULL,
    description  VARCHAR(255),
    value        BYTEA,
    CONSTRAINT secret_unq_key UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS encryption_key (
    id           UUID          NOT NULL CONSTRAINT encryption_key_pkey PRIMARY KEY,
    created_time BIGINT        NOT NULL,
    tenant_id    UUID          NOT NULL,
    password     VARCHAR(255)  NOT NULL,
    salt         VARCHAR(255)  NOT NULL,
    CONSTRAINT encryption_key_tenant_unq_key UNIQUE (tenant_id)
);

-- SECRET TABLE ADDITION END
