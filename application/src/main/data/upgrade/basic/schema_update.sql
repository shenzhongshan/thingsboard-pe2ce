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

-- EXISTING CUSTOMER USER PERMISSIONS BACKFILL START
--
-- 新版用户权限从“用户所属 USER 实体组 + 组角色授权”计算。新建用户会在
-- BaseEntityGroupService.ensureDefaultUserRoleMembership 中自动补齐这两类关系，
-- 但升级前已经存在的客户用户不会重新执行创建流程，因此必须在升级时补齐。
-- 本段只处理客户用户默认组和默认角色，并通过 NOT EXISTS 保证重复执行安全。

-- 历史角色记录可能在 customer_id 字段加入前已经存在；租户级角色统一使用
-- ThingsBoard 的 NULL_UUID 哨兵值，避免后续按 customer_id 等值查询时匹配失败。
UPDATE role
SET customer_id = '13814000-1dd2-11b2-8080-808080808080'
WHERE customer_id IS NULL;

-- 为每个客户的 Customer Users 默认用户组授予同租户的 Customer User 通用角色。
-- md5 结果是稳定的 32 位十六进制值，可直接转换为 UUID；这样脚本即使重复执行，
-- 也不会因为随机主键产生重复授权。
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

-- 把升级前已经存在的客户用户加入其所属客户的 Customer Users 默认用户组。
-- relation 的方向与 ensureDefaultUserRoleMembership 保持一致：用户组 Contains 用户。
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

-- EXISTING CUSTOMER USER PERMISSIONS BACKFILL END
