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
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.EntityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum Resource {

    ALL,
    ADMIN_SETTINGS(EntityType.ADMIN_SETTINGS),
    ALARM(EntityType.ALARM),
    DEVICE(EntityType.DEVICE),
    ASSET(EntityType.ASSET),
    CUSTOMER(EntityType.CUSTOMER),
    DASHBOARD(EntityType.DASHBOARD),
    ENTITY_VIEW(EntityType.ENTITY_VIEW),
    TENANT(EntityType.TENANT),
    RULE_CHAIN(EntityType.RULE_CHAIN),
    USER(EntityType.USER),
    WIDGETS_BUNDLE(EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EntityType.WIDGET_TYPE),
    OAUTH2_CLIENT(EntityType.OAUTH2_CLIENT),
    DOMAIN(EntityType.DOMAIN),
    MOBILE_APP(EntityType.MOBILE_APP),
    MOBILE_APP_BUNDLE(EntityType.MOBILE_APP_BUNDLE),
    OAUTH2_CONFIGURATION_TEMPLATE(),
    TENANT_PROFILE(EntityType.TENANT_PROFILE),
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(EntityType.ASSET_PROFILE),
    API_USAGE_STATE(EntityType.API_USAGE_STATE),
    TB_RESOURCE(EntityType.TB_RESOURCE),
    OTA_PACKAGE(EntityType.OTA_PACKAGE),
    EDGE(EntityType.EDGE),
    RPC(EntityType.RPC),
    QUEUE(EntityType.QUEUE),
    VERSION_CONTROL,
    NOTIFICATION(EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_TEMPLATE,
            EntityType.NOTIFICATION_REQUEST, EntityType.NOTIFICATION_RULE),
    MOBILE_APP_SETTINGS,
    JOB(EntityType.JOB),
    AI_MODEL(EntityType.AI_MODEL),
    API_KEY(EntityType.API_KEY),
    WHITE_LABELING,
    SCHEDULER_EVENT,
    CONVERTER(EntityType.CONVERTER),
    INTEGRATION(EntityType.INTEGRATION),
    ENTITY_GROUP(EntityType.ENTITY_GROUP),
    ROLE(EntityType.ROLE),
    GROUP_PERMISSION(EntityType.GROUP_PERMISSION),
    REPORT_TEMPLATE(EntityType.REPORT_TEMPLATE),
    REPORT(EntityType.REPORT),
    BLOB_ENTITY(EntityType.BLOB_ENTITY),
    SECRET(EntityType.SECRET);

    /**
     * PE parity: static vocabulary of which operations are meaningful for each resource.
     * Purely descriptive — it drives UI role-editor choices and is exposed via
     * /api/permissions/allowedPermissions. Actual enforcement lives in the Permissions classes.
     */
    public static final Map<Resource, Set<Operation>> operationsByResource;

    static {
        Map<Resource, Set<Operation>> map = new EnumMap<>(Resource.class);
        map.put(ALL, Set.of(Operation.values()));
        map.put(ADMIN_SETTINGS, Set.of(Operation.ALL, Operation.READ, Operation.WRITE));
        map.put(OAUTH2_CLIENT, Operation.crudOperations);
        map.put(DOMAIN, Operation.crudOperations);
        map.put(MOBILE_APP, Operation.crudOperations);
        map.put(MOBILE_APP_BUNDLE, Operation.crudOperations);
        map.put(OAUTH2_CONFIGURATION_TEMPLATE, Operation.crudOperations);
        map.put(ALARM, Set.of(Operation.ALL, Operation.READ, Operation.WRITE, Operation.CREATE));
        map.put(DEVICE, Set.of(Operation.ALL, Operation.READ, Operation.WRITE, Operation.CREATE, Operation.DELETE,
                Operation.RPC_CALL, Operation.READ_CREDENTIALS, Operation.WRITE_CREDENTIALS,
                Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY,
                Operation.WRITE_TELEMETRY, Operation.CLAIM_DEVICES, Operation.CHANGE_OWNER,
                Operation.ASSIGN_TO_TENANT, Operation.READ_CALCULATED_FIELD, Operation.WRITE_CALCULATED_FIELD));
        map.put(DEVICE_PROFILE, Operation.defaultCFEntityOperations);
        map.put(ASSET_PROFILE, Operation.defaultCFEntityOperations);
        map.put(OTA_PACKAGE, Operation.defaultEntityOperations);
        map.put(ASSET, Operation.defaultCFEntityOperations);
        map.put(CUSTOMER, Operation.defaultCFEntityOperations);
        map.put(DASHBOARD, Operation.defaultEntityOperations);
        map.put(ENTITY_VIEW, Operation.defaultEntityOperations);
        map.put(EDGE, Operation.defaultEntityOperations);
        map.put(TENANT, Operation.defaultEntityOperations);
        map.put(TENANT_PROFILE, Operation.defaultEntityOperations);
        map.put(API_USAGE_STATE, Set.of(Operation.ALL, Operation.READ, Operation.READ_TELEMETRY));
        map.put(RULE_CHAIN, Operation.defaultEntityOperations);
        map.put(USER, Operation.defaultEntityOperations);
        map.put(WIDGETS_BUNDLE, Operation.crudOperations);
        map.put(WIDGET_TYPE, Operation.crudOperations);
        map.put(TB_RESOURCE, Operation.defaultEntityOperations);
        map.put(CONVERTER, Operation.defaultEntityOperations);
        map.put(INTEGRATION, Operation.defaultEntityOperations);
        map.put(SCHEDULER_EVENT, Operation.defaultEntityOperations);
        // CE models entity groups with a single ENTITY_GROUP resource rather than PE's
        // per-type CUSTOMER_GROUP/DEVICE_GROUP/... resources.
        map.put(ENTITY_GROUP, Operation.defaultEntityGroupOperations);
        map.put(ROLE, Operation.defaultEntityOperations);
        map.put(GROUP_PERMISSION, Operation.crudOperations);
        map.put(WHITE_LABELING, Set.of(Operation.ALL, Operation.READ, Operation.WRITE));
        map.put(QUEUE, Set.of(Operation.ALL, Operation.READ));
        map.put(VERSION_CONTROL, Set.of(Operation.ALL, Operation.READ, Operation.WRITE, Operation.DELETE));
        map.put(NOTIFICATION, Operation.crudOperations);
        map.put(MOBILE_APP_SETTINGS, Set.of(Operation.ALL, Operation.READ, Operation.WRITE));
        map.put(JOB, Operation.crudOperations);
        map.put(SECRET, Operation.crudOperations);
        map.put(REPORT_TEMPLATE, Operation.crudOperations);
        map.put(REPORT, Operation.crudOperations);
        map.put(BLOB_ENTITY, Operation.defaultEntityOperations);
        map.put(AI_MODEL, Operation.crudOperations);
        map.put(API_KEY, Operation.crudOperations);
        map.put(RPC, Operation.crudOperations);
        operationsByResource = Collections.unmodifiableMap(map);
    }

    public static Set<Operation> operationsForResource(Resource resource) {
        return operationsByResource.getOrDefault(resource, Set.of());
    }

    private final Set<EntityType> entityTypes;

    Resource() {
        this.entityTypes = Collections.emptySet();
    }

    Resource(EntityType... entityTypes) {
        this.entityTypes = Set.of(entityTypes);
    }

    public Set<EntityType> getEntityTypes() {
        return entityTypes;
    }

    public static Resource of(EntityType entityType) {
        for (Resource resource : Resource.values()) {
            if (resource.getEntityTypes().contains(entityType)) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Unknown EntityType: " + entityType.name());
    }

}
