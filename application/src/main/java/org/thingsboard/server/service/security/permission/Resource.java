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
    // PE parity: per-group-type resources, so a GENERIC role can grant "READ device groups" without
    // also granting "READ user groups". Resolve them with groupResourceFromGroupType(), never with
    // of() — they deliberately carry no entityTypes so that of(ENTITY_GROUP) stays unambiguous and
    // keeps returning the ENTITY_GROUP umbrella below. (PE maps all seven to EntityType.ENTITY_GROUP
    // and then has to special-case ENTITY_GROUP out of its resourceByEntityType loop.)
    CUSTOMER_GROUP,
    DEVICE_GROUP,
    ASSET_GROUP,
    USER_GROUP,
    ENTITY_VIEW_GROUP,
    EDGE_GROUP,
    DASHBOARD_GROUP,
    ROLE(EntityType.ROLE),
    GROUP_PERMISSION(EntityType.GROUP_PERMISSION),
    REPORT_TEMPLATE(EntityType.REPORT_TEMPLATE),
    REPORT(EntityType.REPORT),
    BLOB_ENTITY(EntityType.BLOB_ENTITY),
    SECRET(EntityType.SECRET),
    AUDIT_LOG,
    CUSTOM_MENU,
    PROFILE,
    QUEUE_STATS(EntityType.QUEUE_STATS);

    /**
     * PE parity: static vocabulary of which operations are meaningful for each resource.
     * Purely descriptive — it drives UI role-editor choices and is exposed via
     * /api/permissions/allowedPermissions. Actual enforcement lives in the Permissions classes.
     */
    public static final Map<Resource, Set<Operation>> operationsByResource;

    /** Entity group member type (the group's {@code type} column) to its per-type resource. */
    private static final Map<EntityType, Resource> groupResourceByGroupType;

    static {
        Map<EntityType, Resource> groupMap = new EnumMap<>(EntityType.class);
        groupMap.put(EntityType.CUSTOMER, CUSTOMER_GROUP);
        groupMap.put(EntityType.DEVICE, DEVICE_GROUP);
        groupMap.put(EntityType.ASSET, ASSET_GROUP);
        groupMap.put(EntityType.USER, USER_GROUP);
        groupMap.put(EntityType.ENTITY_VIEW, ENTITY_VIEW_GROUP);
        groupMap.put(EntityType.EDGE, EDGE_GROUP);
        groupMap.put(EntityType.DASHBOARD, DASHBOARD_GROUP);
        groupResourceByGroupType = Collections.unmodifiableMap(groupMap);

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
        Set<Operation> userOps = new java.util.HashSet<>(Operation.defaultEntityOperations);
        userOps.add(Operation.IMPERSONATE);
        map.put(USER, Set.copyOf(userOps));
        map.put(WIDGETS_BUNDLE, Operation.crudOperations);
        map.put(WIDGET_TYPE, Operation.crudOperations);
        map.put(TB_RESOURCE, Operation.defaultEntityOperations);
        map.put(CONVERTER, Operation.defaultEntityOperations);
        map.put(INTEGRATION, Operation.defaultEntityOperations);
        map.put(SCHEDULER_EVENT, Operation.defaultEntityOperations);
        // ENTITY_GROUP is the umbrella resource; the seven per-type resources below narrow it.
        // A grant on ENTITY_GROUP implies all seven (see MergedUserPermissions.hasGenericPermission),
        // which keeps roles written before the per-type split working unchanged.
        map.put(ENTITY_GROUP, Operation.defaultEntityGroupOperations);
        for (Resource groupResource : groupResourceByGroupType.values()) {
            map.put(groupResource, Operation.defaultEntityGroupOperations);
        }
        map.put(AUDIT_LOG, Set.of(Operation.ALL, Operation.READ));
        map.put(CUSTOM_MENU, Set.of(Operation.ALL, Operation.READ, Operation.WRITE, Operation.DELETE));
        map.put(PROFILE, Set.of(Operation.ALL, Operation.WRITE));
        map.put(QUEUE_STATS, Set.of(Operation.ALL, Operation.READ));
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

    /**
     * The per-type resource guarding entity groups whose members are {@code groupType}, e.g.
     * {@link #DEVICE_GROUP} for a group of devices. Falls back to the {@link #ENTITY_GROUP} umbrella
     * for group types that have no dedicated resource, so callers never have to null-check.
     */
    public static Resource groupResourceFromGroupType(EntityType groupType) {
        return groupType == null ? ENTITY_GROUP : groupResourceByGroupType.getOrDefault(groupType, ENTITY_GROUP);
    }

    /** The seven per-type group resources implied by a grant on the {@link #ENTITY_GROUP} umbrella. */
    public static Set<Resource> groupResources() {
        return Set.copyOf(groupResourceByGroupType.values());
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

    /**
     * Like {@link #of(EntityType)} but null instead of throwing, for callers that sweep every
     * EntityType. Several types (RULE_NODE, JOB, ...) deliberately have no Resource.
     */
    public static Resource ofOrNull(EntityType entityType) {
        for (Resource resource : Resource.values()) {
            if (resource.getEntityTypes().contains(entityType)) {
                return resource;
            }
        }
        return null;
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
