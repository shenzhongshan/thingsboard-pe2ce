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

-- VC SUPPORT FOR PE-PORTED ENTITIES START

-- external_id backs version-control export/import; version backs JPA optimistic locking. The
-- entities now extend BaseVersionedEntity, so the version column must exist before startup.
-- scheduler_event gets the columns for schema parity only — SchedulerEventId is not an EntityId in
-- CE, so scheduler events cannot take part in version control yet.
ALTER TABLE converter       ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE converter       ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE integration     ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE integration     ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE role            ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE role            ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE report_template ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE report_template ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE scheduler_event ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE scheduler_event ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_converter_external_id       ON converter(external_id);
CREATE INDEX IF NOT EXISTS idx_integration_external_id     ON integration(external_id);
CREATE INDEX IF NOT EXISTS idx_role_external_id            ON role(external_id);
CREATE INDEX IF NOT EXISTS idx_report_template_external_id ON report_template(external_id);
CREATE INDEX IF NOT EXISTS idx_scheduler_event_external_id ON scheduler_event(external_id);

-- Indexes missed by the original ports. See the matching block in schema-entities.sql for why the
-- remaining PE indexes were deliberately not brought over.
CREATE INDEX IF NOT EXISTS idx_group_permission_tenant_id  ON group_permission(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custom_menu                 ON custom_menu(tenant_id, scope, assignee_type);
CREATE INDEX IF NOT EXISTS idx_api_key_tenant_id_user_id   ON api_key(tenant_id, user_id);

-- VC SUPPORT FOR PE-PORTED ENTITIES END

-- ALARM OWNER-HIERARCHY PROPAGATION START
-- Alarms can now propagate up the whole customer chain, not just to the immediate owner.
-- The create_or_update_active_alarm / update_alarm functions gained a matching parameter; they are
-- recreated from schema-functions.sql, which DROPs them first because the signature changed.
ALTER TABLE alarm ADD COLUMN IF NOT EXISTS propagate_to_owner_hierarchy BOOLEAN;
-- ALARM OWNER-HIERARCHY PROPAGATION END

-- RESOURCE CUSTOMER_ID ADDITION START

ALTER TABLE resource ADD COLUMN IF NOT EXISTS customer_id uuid;

-- RESOURCE CUSTOMER_ID ADDITION END

-- CREATE TABLE IF NOT EXISTS is a no-op on an existing database, so a new column needs its own
-- ALTER to reach one. Without this line only a fresh install would get dashboard.customer_id.
ALTER TABLE dashboard ADD COLUMN IF NOT EXISTS customer_id uuid;

-- Owner of the role. Follows PE: tenant-level roles store the NULL_UUID sentinel rather than SQL
-- NULL, because the name-uniqueness lookup is an equality match and "= NULL" never matches.
ALTER TABLE role ADD COLUMN IF NOT EXISTS customer_id UUID;
-- Which entity group this edge event belongs to. PE's edge sync is group-centric:
-- DeviceUpdateMsg and friends carry entityGroupIdMSB/LSB and the value is threaded from here.
-- NULL means the event is not group-scoped (rule chains, admin settings, ...).
ALTER TABLE edge_event ADD COLUMN IF NOT EXISTS entity_group_id uuid;
