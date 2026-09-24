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

-- LTS cumulative schema update file.
-- All statements must be idempotent (use IF NOT EXISTS, ADD COLUMN IF NOT EXISTS, DO $$ ... END $$ guards, etc.).
-- This file is executed by SystemPatchApplier on every version increase within the LTS family.

-- CALCULATED FIELD ADDITIONAL INFO ADDITION START

ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS additional_info varchar;

-- CALCULATED FIELD ADDITIONAL INFO ADDITION END

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

-- ROLE CUSTOMER_ID AND EXISTING CUSTOMER USER PERMISSIONS BACKFILL START
--
-- LTS 升级路径也必须包含这段迁移：历史客户用户需要加入 Customer Users 默认组，
-- 并获得 Customer User 通用角色，否则 DefaultUserPermissionsService 会计算为空权限。
-- 所有语句均可重复执行，保证 basic 和 LTS 两条升级路径结果一致。

ALTER TABLE role ADD COLUMN IF NOT EXISTS customer_id UUID;

UPDATE role
SET customer_id = '13814000-1dd2-11b2-8080-808080808080'
WHERE customer_id IS NULL;

INSERT INTO group_permission (
    id,
    created_time,
    tenant_id,
    role_id,
    user_group_id,
    entity_group_id,
    entity_group_type,
    is_public
)
SELECT
    md5(customer_users.id::text || ':' || customer_user_role.id::text || ':default-customer-user-role')::uuid,
    (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::bigint,
    customer_users.tenant_id,
    customer_user_role.id,
    customer_users.id,
    NULL,
    NULL,
    FALSE
FROM entity_group customer_users
JOIN role customer_user_role
  ON customer_user_role.tenant_id = customer_users.tenant_id
 AND customer_user_role.customer_id = '13814000-1dd2-11b2-8080-808080808080'
 AND customer_user_role.name = 'Customer User'
 AND customer_user_role.type = 'GENERIC'
WHERE customer_users.type = 'USER'
  AND customer_users.name = 'Customer Users'
  AND customer_users.owner_id <> '13814000-1dd2-11b2-8080-808080808080'
  AND NOT EXISTS (
      SELECT 1
      FROM group_permission existing_permission
      WHERE existing_permission.user_group_id = customer_users.id
        AND existing_permission.role_id = customer_user_role.id
        AND existing_permission.entity_group_id IS NULL
  );

INSERT INTO relation (
    from_id,
    from_type,
    to_id,
    to_type,
    relation_type_group,
    relation_type,
    additional_info,
    version
)
SELECT
    customer_users.id,
    'ENTITY_GROUP',
    customer_user.id,
    'USER',
    'FROM_ENTITY_GROUP',
    'Contains',
    NULL,
    0
FROM tb_user customer_user
JOIN entity_group customer_users
  ON customer_users.tenant_id = customer_user.tenant_id
 AND customer_users.owner_id = customer_user.customer_id
 AND customer_users.type = 'USER'
 AND customer_users.name = 'Customer Users'
WHERE customer_user.authority = 'CUSTOMER_USER'
  AND NOT EXISTS (
      SELECT 1
      FROM relation existing_membership
      WHERE existing_membership.from_id = customer_users.id
        AND existing_membership.from_type = 'ENTITY_GROUP'
        AND existing_membership.to_id = customer_user.id
        AND existing_membership.to_type = 'USER'
        AND existing_membership.relation_type_group = 'FROM_ENTITY_GROUP'
        AND existing_membership.relation_type = 'Contains'
  );

-- ROLE CUSTOMER_ID AND EXISTING CUSTOMER USER PERMISSIONS BACKFILL END
