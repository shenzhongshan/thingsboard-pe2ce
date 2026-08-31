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
package org.thingsboard.server.service.solutions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.solutions.data.EntityIdRef;
import org.thingsboard.server.service.solutions.data.SolutionInstallContext;
import org.thingsboard.server.service.solutions.data.SolutionInstallContext.EmulatorTarget;
import org.thingsboard.server.service.solutions.data.definition.AssetDefinition;
import org.thingsboard.server.service.solutions.data.definition.CustomerDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceDefinition;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.solutions.data.definition.GroupRoleDefinition;
import org.thingsboard.server.service.solutions.data.definition.ReferenceableEntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.RelationDefinition;
import org.thingsboard.server.service.solutions.data.definition.RoleDefinition;
import org.thingsboard.server.service.solutions.data.definition.SchedulerEventDefinition;
import org.thingsboard.server.service.solutions.data.definition.TenantDefinition;
import org.thingsboard.server.service.solutions.data.definition.UserDefinition;
import org.thingsboard.server.service.solutions.data.definition.UserGroupDefinition;
import org.thingsboard.server.service.solutions.data.solution.SolutionDescriptor;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;
import org.thingsboard.server.service.solutions.data.solution.SolutionTemplate;
import org.thingsboard.server.service.solutions.data.solution.SolutionTemplateDetails;
import org.thingsboard.server.service.solutions.data.solution.SolutionTemplateInfo;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateDetails;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInfo;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultSolutionService implements SolutionService {

    public static final String SOLUTIONS_DIR = "solutions";
    private static final String JSON_DIR = "json";
    private static final String DESCRIPTORS_FILE = "solutions.json";
    private static final int EMULATOR_TIMEOUT_MIN = 30;
    private static final long SIX_MONTHS_MS = TimeUnit.DAYS.toMillis(183);
    private static final List<String> FIRST_NAMES = List.of("Olivia", "William", "Lucas", "Isabella", "Henry", "Mia", "Theodore", "Evelyn");
    private static final List<String> LAST_NAMES = List.of("Smith", "Harris", "Brown", "Jones", "Davis", "Garcia", "Moore", "Clark");

    private final InstallScripts installScripts;
    private final AttributesService attributesService;
    private final EntityServiceRegistry entityServiceRegistry;
    private final RoleService roleService;
    private final RuleChainService ruleChainService;
    private final DeviceProfileService deviceProfileService;
    private final AssetProfileService assetProfileService;
    private final CustomerService customerService;
    private final UserService userService;
    private final AssetService assetService;
    private final DeviceService deviceService;
    private final DashboardService dashboardService;
    private final ImageService imageService;
    private final ResourceService resourceService;
    private final EntityGroupService entityGroupService;
    private final GroupPermissionService groupPermissionService;
    private final RelationService relationService;
    private final SchedulerEventService schedulerEventService;
    private final CalculatedFieldService calculatedFieldService;
    private final PasswordEncoder passwordEncoder;
    private final SolutionEmulatorLauncher emulatorLauncher;

    /** Catalogue order follows solutions.json. */
    private final List<SolutionTemplateInfo> solutions = new ArrayList<>();
    private final Map<String, SolutionTemplateDetails> solutionsMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        Path descriptorsFile = resolve(DESCRIPTORS_FILE);
        if (!Files.exists(descriptorsFile)) {
            // Solution templates are optional data; without them the API simply reports none.
            log.info("Solution templates descriptor not found at {}, solution templates are unavailable", descriptorsFile);
            return;
        }
        try {
            List<SolutionDescriptor> descriptors = JacksonUtil.readValue(descriptorsFile.toFile(),
                    new TypeReference<List<SolutionDescriptor>>() {});
            if (descriptors == null) {
                return;
            }
            for (SolutionDescriptor descriptor : descriptors) {
                solutions.add(new SolutionTemplateInfo(descriptor));
                solutionsMap.put(descriptor.getId(), new SolutionTemplateDetails(descriptor,
                        readFile(resolve(descriptor.getId(), "highlights.md")),
                        readFile(resolve(descriptor.getId(), "description.md"))));
            }
            log.info("Loaded {} solution templates", solutions.size());
        } catch (Exception e) {
            log.error("Failed to load solution templates from {}", descriptorsFile, e);
        }
    }

    @Override
    public List<TenantSolutionTemplateInfo> getSolutionInfos(TenantId tenantId) throws ThingsboardException {
        if (solutions.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> statusKeys = solutions.stream().map(s -> toStatusKey(s.getId())).toList();
            List<AttributeKvEntry> stateList = attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, statusKeys).get();
            List<TenantSolutionTemplateInfo> result = new ArrayList<>(solutions.size());
            for (SolutionTemplateInfo solution : solutions) {
                result.add(new TenantSolutionTemplateInfo(solution, isInstalled(stateList, solution.getId())));
            }
            return result;
        } catch (Exception e) {
            log.error("[{}] Failed to fetch solution list", tenantId, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public TenantSolutionTemplateDetails getSolutionDetails(TenantId tenantId, String solutionId) throws ThingsboardException {
        SolutionTemplateDetails details = solutionsMap.get(solutionId);
        if (details == null) {
            throw new ThingsboardException("Solution template not found: " + solutionId, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        try {
            Optional<AttributeKvEntry> state = attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, toStatusKey(solutionId)).get();
            boolean installed = state.flatMap(AttributeKvEntry::getBooleanValue).orElse(Boolean.FALSE);
            return new TenantSolutionTemplateDetails(details, installed);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to fetch solution details", tenantId, solutionId, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public TenantSolutionTemplateInstructions getSolutionInstructions(TenantId tenantId, String solutionId) throws ThingsboardException {
        if (!solutionsMap.containsKey(solutionId)) {
            throw new ThingsboardException("Solution template not found: " + solutionId, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        try {
            Optional<AttributeKvEntry> state = attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, toInstructionsKey(solutionId)).get();
            String json = state.flatMap(AttributeKvEntry::getJsonValue).orElse(null);
            if (json == null) {
                throw new ThingsboardException("Solution template is not installed: " + solutionId, ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            return JacksonUtil.fromString(json, TenantSolutionTemplateInstructions.class);
        } catch (ThingsboardException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}][{}] Failed to fetch solution instructions", tenantId, solutionId, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public SolutionInstallResponse installSolution(User user, TenantId tenantId, String solutionId) throws ThingsboardException {
        if (!solutionsMap.containsKey(solutionId)) {
            throw new ThingsboardException("Solution template not found: " + solutionId, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        SolutionInstallContext ctx = new SolutionInstallContext(tenantId, solutionId, user);
        try {
            // Order matters: profiles and rule chains must exist before the entities that
            // reference them; users come after dashboards because their group roles grant
            // permissions over dashboard groups; relations are last because both ends must exist.
            provisionRoles(ctx);
            provisionTenantDetails(ctx);
            provisionRuleChains(ctx);
            provisionDeviceProfiles(ctx);
            provisionAssetProfiles(ctx);
            provisionCustomers(ctx);
            provisionAssets(ctx);
            provisionDevices(ctx);
            // System-scoped, not tenant-scoped: dashboard JSON hardcodes links like
            // "/api/images/system/...", so these have to exist under the system tenant before the
            // dashboards that reference them are saved, matching PE's own bundled resource keys.
            provisionResources(ctx);
            provisionDashboards(ctx);
            provisionUsers(ctx);
            provisionRelations(ctx);
            provisionSchedulerEvents(ctx);
            provisionCalculatedFields(ctx);
            // Last: the generated history flows through the rule chains and calculated fields
            // provisioned above, so those have to be in place before any of it is published.
            runEmulators(ctx);
            ctx.getSolutionInstructions().setDetails(readFile(resolve(solutionId, "instructions.md")));
            saveInstallState(ctx);
            log.info("[{}][{}] Installed solution template, {} entities created", tenantId, solutionId, ctx.getCreatedEntities().size());
            return new SolutionInstallResponse(ctx.getSolutionInstructions(), true);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to install solution template, rolling back", tenantId, solutionId, e);
            rollback(ctx);
            throw new ThingsboardException("Failed to install solution template: " + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public void uninstallSolution(TenantId tenantId, String solutionId) throws ThingsboardException {
        if (!solutionsMap.containsKey(solutionId)) {
            throw new ThingsboardException("Solution template not found: " + solutionId, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        try {
            String eventsJson = findStringAttribute(tenantId, toCreatedSchedulerEventsKey(solutionId));
            if (eventsJson != null) {
                deleteSchedulerEvents(tenantId, JacksonUtil.fromString(eventsJson, new TypeReference<List<UUID>>() {})
                        .stream().map(SchedulerEventId::new).toList());
            }
            String entitiesJson = findStringAttribute(tenantId, toCreatedEntitiesKey(solutionId));
            if (entitiesJson != null) {
                deleteEntities(tenantId, JacksonUtil.fromString(entitiesJson, new TypeReference<List<EntityIdRef>>() {})
                        .stream().map(EntityIdRef::toEntityId).toList());
            }
            attributesService.removeAll(tenantId, tenantId, AttributeScope.SERVER_SCOPE,
                    List.of(toCreatedEntitiesKey(solutionId), toCreatedSchedulerEventsKey(solutionId),
                            toStatusKey(solutionId), toInstructionsKey(solutionId))).get();
            log.info("[{}][{}] Uninstalled solution template", tenantId, solutionId);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to uninstall solution template", tenantId, solutionId, e);
            throw new ThingsboardException("Failed to uninstall solution template: " + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }
    }

    private String findStringAttribute(TenantId tenantId, String key) throws Exception {
        return attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, key).get()
                .flatMap(AttributeKvEntry::getStrValue).orElse(null);
    }

    private void saveInstallState(SolutionInstallContext ctx) throws Exception {
        long ts = System.currentTimeMillis();
        List<EntityIdRef> refs = ctx.getCreatedEntities().stream().map(EntityIdRef::of).toList();
        List<UUID> eventIds = ctx.getCreatedSchedulerEvents().stream().map(SchedulerEventId::getId).toList();
        attributesService.save(ctx.getTenantId(), ctx.getTenantId(), AttributeScope.SERVER_SCOPE, List.of(
                new BaseAttributeKvEntry(new StringDataEntry(toCreatedEntitiesKey(ctx.getSolutionId()), JacksonUtil.toString(refs)), ts),
                new BaseAttributeKvEntry(new StringDataEntry(toCreatedSchedulerEventsKey(ctx.getSolutionId()), JacksonUtil.toString(eventIds)), ts),
                new BaseAttributeKvEntry(new JsonDataEntry(toInstructionsKey(ctx.getSolutionId()), JacksonUtil.toString(ctx.getSolutionInstructions())), ts),
                new BaseAttributeKvEntry(new BooleanDataEntry(toStatusKey(ctx.getSolutionId()), true), ts)
        )).get();
    }

    /** Deletes newest-first so children go before the parents they reference. */
    private void rollback(SolutionInstallContext ctx) {
        deleteSchedulerEvents(ctx.getTenantId(), ctx.getCreatedSchedulerEvents());
        deleteEntities(ctx.getTenantId(), ctx.getCreatedEntities());
    }

    private void deleteSchedulerEvents(TenantId tenantId, List<SchedulerEventId> eventIds) {
        for (SchedulerEventId eventId : eventIds) {
            try {
                schedulerEventService.deleteSchedulerEvent(tenantId, eventId);
            } catch (Exception e) {
                log.warn("[{}] Failed to delete scheduler event {} during solution cleanup", tenantId, eventId.getId(), e);
            }
        }
    }

    private void deleteEntities(TenantId tenantId, List<EntityId> entities) {
        List<EntityId> reversed = new ArrayList<>(entities);
        Collections.reverse(reversed);
        for (EntityId entityId : reversed) {
            try {
                deleteEntity(tenantId, entityId);
            } catch (Exception e) {
                // Best effort: one failure must not strand the remaining entities.
                log.warn("[{}] Failed to delete {} {} during solution cleanup", tenantId, entityId.getEntityType(), entityId.getId(), e);
            }
        }
    }

    /**
     * Deletion order matters beyond plain reverse-creation: a role cannot be deleted while a group
     * permission still references it, and the permission types below have no
     * {@code EntityDaoService.deleteEntity} implementation, so they are dispatched directly.
     */
    private void deleteEntity(TenantId tenantId, EntityId entityId) {
        switch (entityId.getEntityType()) {
            case USER -> userService.deleteUser(tenantId, userService.findUserById(tenantId, (UserId) entityId));
            case ROLE -> roleService.deleteRole(tenantId, (RoleId) entityId);
            case GROUP_PERMISSION -> groupPermissionService.deleteGroupPermission(tenantId, (GroupPermissionId) entityId);
            case ENTITY_GROUP -> entityGroupService.deleteEntityGroup(tenantId, (EntityGroupId) entityId);
            default -> entityServiceRegistry.getServiceByEntityType(entityId.getEntityType()).deleteEntity(tenantId, entityId, true);
        }
    }

    // ---------------------------------------------------------------- provisioning steps

    private void provisionRoles(SolutionInstallContext ctx) {
        for (RoleDefinition def : loadList(ctx, "roles.json", new TypeReference<List<RoleDefinition>>() {})) {
            Role role = new Role();
            role.setTenantId(ctx.getTenantId());
            role.setName(def.getName());
            role.setType(def.getType());
            role.setPermissions(def.getOperations());
            Role saved = roleService.saveRole(ctx.getTenantId(), role);
            ctx.registerCreated(saved.getId());
            ctx.putId(ctx.getTenantId(), EntityType.ROLE, saved.getName(), saved.getId());
        }
    }

    /** Tenant-level attributes and user groups declared by the template; the tenant already exists. */
    private void provisionTenantDetails(SolutionInstallContext ctx) throws Exception {
        TenantDefinition def = loadOne(ctx, "tenant.json", TenantDefinition.class);
        if (def == null) {
            return;
        }
        saveAttributes(ctx, ctx.getTenantId(), def.getAttributes(), def.getSharedAttributes());
        for (UserGroupDefinition groupDef : def.getUserGroups()) {
            findOrCreateGroup(ctx, ctx.getTenantId(), EntityType.USER, groupDef.getName());
        }
    }

    /**
     * Rule chain files hold {@code {ruleChain, metadata}} — the same shape the export/import API
     * uses. Every chain row is created before any metadata is saved, because flow nodes routinely
     * point at sibling chains and all of those ids have to be resolvable by then.
     */
    private void provisionRuleChains(SolutionInstallContext ctx) {
        Map<RuleChainId, JsonNode> metaDataByChain = new LinkedHashMap<>();
        for (ReferenceableEntityDefinition def : loadList(ctx, "rule_chains.json", new TypeReference<List<ReferenceableEntityDefinition>>() {})) {
            JsonNode json = JacksonUtil.toJsonNode(resolve(ctx.getSolutionId(), "rule_chains", def.getFile()).toFile());
            RuleChain ruleChain = bind(json.get("ruleChain"), RuleChain.class);
            ruleChain.setId(null);
            ruleChain.setTenantId(ctx.getTenantId());
            ruleChain.setName(def.getName() != null ? def.getName() : ruleChain.getName());
            ruleChain.setRoot(false);
            ruleChain.setFirstRuleNodeId(null);
            RuleChain saved = ruleChainService.saveRuleChain(ruleChain);
            ctx.registerCreated(saved.getId());
            ctx.registerJsonId(def.getJsonId(), saved.getId());
            ctx.putId(ctx.getTenantId(), EntityType.RULE_CHAIN, saved.getName(), saved.getId());
            metaDataByChain.put(saved.getId(), json.get("metadata"));
        }
        for (Map.Entry<RuleChainId, JsonNode> entry : metaDataByChain.entrySet()) {
            RuleChainMetaData metaData = bind(replaceIds(ctx, entry.getValue()), RuleChainMetaData.class);
            if (metaData != null) {
                metaData.setRuleChainId(entry.getKey());
                ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), metaData, Function.identity());
            }
        }
    }

    private void provisionDeviceProfiles(SolutionInstallContext ctx) {
        for (JsonNode node : loadJsonFromDirectory(ctx.getSolutionId(), "device_profiles")) {
            String jsonId = takeJsonId(node);
            DeviceProfile profile = bind(node, DeviceProfile.class);
            DeviceProfile existing = deviceProfileService.findDeviceProfileByName(ctx.getTenantId(), profile.getName());
            if (existing != null) {
                // Reuse a profile of the same name rather than failing on the unique constraint.
                registerProfile(ctx, EntityType.DEVICE_PROFILE, jsonId, existing.getName(), existing.getId(), false);
                continue;
            }
            profile.setId(null);
            profile.setTenantId(ctx.getTenantId());
            profile.setDefault(false);
            profile.setExternalId(null);
            profile.setFirmwareId(null);
            profile.setSoftwareId(null);
            // Edge is out of scope for this port, so the template's edge chain would dangle.
            profile.setDefaultEdgeRuleChainId(null);
            remapRuleChain(ctx, profile.getDefaultRuleChainId(), id -> profile.setDefaultRuleChainId(id != null ? new RuleChainId(id) : null));
            DeviceProfile saved = deviceProfileService.saveDeviceProfile(profile);
            registerProfile(ctx, EntityType.DEVICE_PROFILE, jsonId, saved.getName(), saved.getId(), true);
        }
    }

    private void provisionAssetProfiles(SolutionInstallContext ctx) {
        for (JsonNode node : loadJsonFromDirectory(ctx.getSolutionId(), "asset_profiles")) {
            String jsonId = takeJsonId(node);
            AssetProfile profile = bind(node, AssetProfile.class);
            AssetProfile existing = assetProfileService.findAssetProfileByName(ctx.getTenantId(), profile.getName());
            if (existing != null) {
                registerProfile(ctx, EntityType.ASSET_PROFILE, jsonId, existing.getName(), existing.getId(), false);
                continue;
            }
            profile.setId(null);
            profile.setTenantId(ctx.getTenantId());
            profile.setDefault(false);
            profile.setExternalId(null);
            profile.setDefaultEdgeRuleChainId(null);
            remapRuleChain(ctx, profile.getDefaultRuleChainId(), id -> profile.setDefaultRuleChainId(id != null ? new RuleChainId(id) : null));
            AssetProfile saved = assetProfileService.saveAssetProfile(profile);
            registerProfile(ctx, EntityType.ASSET_PROFILE, jsonId, saved.getName(), saved.getId(), true);
        }
    }

    /**
     * Resolves the profile for a device/asset type, creating it when the template ships no profile
     * file for that type. Doing it here rather than letting {@code saveDevice}/{@code saveAsset}
     * auto-create one by type name is what makes the profile part of the install's own footprint,
     * so uninstall takes it away again.
     */
    private DeviceProfileId ensureDeviceProfile(SolutionInstallContext ctx, String type) {
        EntityId known = ctx.getId(ctx.getTenantId(), EntityType.DEVICE_PROFILE, type);
        if (known != null) {
            return new DeviceProfileId(known.getId());
        }
        boolean existed = deviceProfileService.findDeviceProfileByName(ctx.getTenantId(), type) != null;
        DeviceProfile profile = deviceProfileService.findOrCreateDeviceProfile(ctx.getTenantId(), type);
        registerProfile(ctx, EntityType.DEVICE_PROFILE, null, type, profile.getId(), !existed);
        return profile.getId();
    }

    private AssetProfileId ensureAssetProfile(SolutionInstallContext ctx, String type) {
        EntityId known = ctx.getId(ctx.getTenantId(), EntityType.ASSET_PROFILE, type);
        if (known != null) {
            return new AssetProfileId(known.getId());
        }
        boolean existed = assetProfileService.findAssetProfileByName(ctx.getTenantId(), type) != null;
        AssetProfile profile = assetProfileService.findOrCreateAssetProfile(ctx.getTenantId(), type);
        registerProfile(ctx, EntityType.ASSET_PROFILE, null, type, profile.getId(), !existed);
        return profile.getId();
    }

    private void registerProfile(SolutionInstallContext ctx, EntityType entityType, String jsonId, String name, EntityId id, boolean created) {
        if (created) {
            ctx.registerCreated(id);
        }
        ctx.registerJsonId(jsonId, id);
        ctx.putId(ctx.getTenantId(), entityType, name, id);
    }

    /**
     * Binds template JSON leniently: the shipped files were authored against PE and carry fields
     * this build's domain classes don't declare, which the strict default mapper rejects.
     */
    private <T> T bind(JsonNode node, Class<T> type) {
        try {
            return node != null ? JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER.treeToValue(node, type) : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to read solution " + type.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to read solution file: " + e.getMessage(), e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to read solution file: " + e.getMessage(), e);
        }
    }

    /**
     * Removes and returns the template's placeholder id. It has to come off the node before the
     * node is bound, because the domain classes reject properties they don't declare.
     */
    private String takeJsonId(JsonNode node) {
        JsonNode jsonId = node instanceof ObjectNode object ? object.remove("jsonId") : node.get("jsonId");
        return jsonId != null && jsonId.isTextual() ? jsonId.asText() : null;
    }

    private void provisionCustomers(SolutionInstallContext ctx) throws Exception {
        for (CustomerDefinition def : loadList(ctx, "customers.json", new TypeReference<List<CustomerDefinition>>() {})) {
            Customer customer = new Customer();
            customer.setTenantId(ctx.getTenantId());
            customer.setTitle(def.getName());
            customer.setEmail(def.getEmail());
            customer.setCountry(def.getCountry());
            customer.setCity(def.getCity());
            customer.setState(def.getState());
            customer.setZip(def.getZip());
            customer.setAddress(def.getAddress());
            Customer saved = customerService.saveCustomer(customer);
            ctx.registerCreated(saved.getId());
            ctx.registerJsonId(def.getJsonId(), saved.getId());
            ctx.putId(ctx.getTenantId(), EntityType.CUSTOMER, saved.getTitle(), saved.getId());
            saveAttributes(ctx, saved.getId(), def.getAttributes(), def.getSharedAttributes());
            addToGroupIfNamed(ctx, ctx.getTenantId(), EntityType.CUSTOMER, def.getGroup(), saved.getId());
            // Groups the template expects to exist under this customer, even while still empty —
            // devices/assets are assigned into them by name in the steps that follow.
            def.getDeviceGroups().forEach(name -> findOrCreateGroup(ctx, saved.getId(), EntityType.DEVICE, name));
            def.getAssetGroups().forEach(name -> findOrCreateGroup(ctx, saved.getId(), EntityType.ASSET, name));
        }
    }

    /**
     * Users, their groups and the role grants those groups carry. Runs after dashboards because a
     * group role typically grants read access over a dashboard group created in that step.
     */
    private void provisionUsers(SolutionInstallContext ctx) throws Exception {
        TenantDefinition tenantDef = loadOne(ctx, "tenant.json", TenantDefinition.class);
        if (tenantDef != null) {
            provisionUserGroups(ctx, ctx.getTenantId(), tenantDef.getUserGroups());
        }
        for (CustomerDefinition def : loadList(ctx, "customers.json", new TypeReference<List<CustomerDefinition>>() {})) {
            EntityId customerId = ctx.getId(ctx.getTenantId(), EntityType.CUSTOMER, def.getName());
            if (customerId == null) {
                continue;
            }
            provisionUserGroups(ctx, customerId, def.getUserGroups());
            for (UserDefinition userDef : def.getUsers()) {
                provisionUser(ctx, new CustomerId(customerId.getId()), userDef);
            }
        }
    }

    private void provisionUserGroups(SolutionInstallContext ctx, EntityId ownerId, List<UserGroupDefinition> groupDefs) {
        for (UserGroupDefinition groupDef : groupDefs) {
            EntityGroupId userGroupId = findOrCreateGroup(ctx, ownerId, EntityType.USER, groupDef.getName());
            for (String roleName : groupDef.getGenericRoles()) {
                saveGroupPermission(ctx, userGroupId, roleName, null, null);
            }
            for (GroupRoleDefinition roleDef : groupDef.getGroupRoles()) {
                saveGroupPermission(ctx, userGroupId, roleDef.getRoleName(), roleDef.getGroupType(), roleDef.getGroupName());
            }
        }
    }

    /**
     * A generic grant (no entity group) applies the role across the owner's whole scope; a group
     * grant narrows it to one entity group, which is how templates share dashboards with customers.
     */
    private void saveGroupPermission(SolutionInstallContext ctx, EntityGroupId userGroupId, String roleName,
                                     EntityType targetGroupType, String targetGroupName) {
        EntityId roleId = ctx.getId(ctx.getTenantId(), EntityType.ROLE, roleName);
        if (roleId == null) {
            log.warn("[{}] Solution role not found: '{}'", ctx.getSolutionId(), roleName);
            return;
        }
        GroupPermission permission = new GroupPermission();
        permission.setTenantId(ctx.getTenantId());
        permission.setUserGroupId(userGroupId);
        permission.setRoleId(new RoleId(roleId.getId()));
        if (targetGroupName != null) {
            EntityGroupId targetGroupId = ctx.resolveGroupId(targetGroupType, targetGroupName);
            if (targetGroupId == null) {
                log.warn("[{}] Solution entity group not found: {} '{}'", ctx.getSolutionId(), targetGroupType, targetGroupName);
                return;
            }
            permission.setEntityGroupId(targetGroupId);
            permission.setEntityGroupType(targetGroupType);
        }
        ctx.registerCreated(groupPermissionService.saveGroupPermission(ctx.getTenantId(), permission).getId());
    }

    private void provisionUser(SolutionInstallContext ctx, CustomerId customerId, UserDefinition def) throws Exception {
        User newUser = new User();
        newUser.setTenantId(ctx.getTenantId());
        newUser.setCustomerId(customerId);
        newUser.setAuthority(Authority.CUSTOMER_USER);
        newUser.setEmail(def.getName());
        newUser.setFirstName(def.getFirstname());
        newUser.setLastName(def.getLastname());
        User saved = userService.saveUser(ctx.getTenantId(), newUser);
        ctx.registerCreated(saved.getId());
        ctx.registerJsonId(def.getJsonId(), saved.getId());
        ctx.putId(customerId, EntityType.USER, saved.getEmail(), saved.getId());
        // Also tenant-wide: emails are globally unique, and relations reference users by email
        // from a tenant-scoped lookup.
        ctx.putId(ctx.getTenantId(), EntityType.USER, saved.getEmail(), saved.getId());
        if (StringUtils.isNotBlank(def.getPassword())) {
            // Activate right away: solution users are demo accounts meant to be logged into.
            UserCredentials credentials = userService.findUserCredentialsByUserId(ctx.getTenantId(), saved.getId());
            credentials.setEnabled(true);
            credentials.setActivateToken(null);
            credentials.setPassword(passwordEncoder.encode(def.getPassword()));
            userService.replaceUserCredentials(ctx.getTenantId(), credentials);
        }
        saveAttributes(ctx, saved.getId(), def.getAttributes(), def.getSharedAttributes());
        addToGroupIfNamed(ctx, customerId, EntityType.USER, def.getGroup(), saved.getId());
    }

    private void provisionAssets(SolutionInstallContext ctx) throws Exception {
        for (AssetDefinition def : loadList(ctx, "assets.json", new TypeReference<List<AssetDefinition>>() {})) {
            CustomerId customerId = resolveCustomer(ctx, def.getCustomer());
            Asset asset = new Asset();
            asset.setTenantId(ctx.getTenantId());
            asset.setName(def.getName());
            asset.setType(def.getType());
            asset.setLabel(def.getLabel());
            asset.setAssetProfileId(ensureAssetProfile(ctx, def.getType()));
            if (customerId != null) {
                asset.setCustomerId(customerId);
            }
            Asset saved = assetService.saveAsset(asset);
            ctx.registerCreated(saved.getId());
            ctx.registerJsonId(def.getJsonId(), saved.getId());
            EntityId ownerId = customerId != null ? customerId : ctx.getTenantId();
            ctx.putId(ownerId, EntityType.ASSET, saved.getName(), saved.getId());
            ctx.putId(ctx.getTenantId(), EntityType.ASSET, saved.getName(), saved.getId());
            saveAttributes(ctx, saved.getId(), def.getAttributes(), def.getSharedAttributes());
            addToGroupIfNamed(ctx, ownerId, EntityType.ASSET, def.getGroup(), saved.getId());
            ctx.addPendingRelations(saved.getId(), def.getRelations());
            ctx.addEmulatorTarget(new EmulatorTarget(saved.getId(), saved.getName(), def.getEmulator(), true, null));
        }
    }

    private void provisionDevices(SolutionInstallContext ctx) throws Exception {
        for (DeviceDefinition def : loadList(ctx, "devices.json", new TypeReference<List<DeviceDefinition>>() {})) {
            CustomerId customerId = resolveCustomer(ctx, def.getCustomer());
            Device device = new Device();
            device.setTenantId(ctx.getTenantId());
            device.setName(def.getName());
            device.setType(def.getType());
            device.setLabel(def.getLabel());
            device.setAdditionalInfo(def.getAdditionalInfo());
            device.setDeviceProfileId(ensureDeviceProfile(ctx, def.getType()));
            if (customerId != null) {
                device.setCustomerId(customerId);
            }
            Device saved = deviceService.saveDevice(device);
            ctx.registerCreated(saved.getId());
            ctx.registerJsonId(def.getJsonId(), saved.getId());
            EntityId ownerId = customerId != null ? customerId : ctx.getTenantId();
            ctx.putId(ownerId, EntityType.DEVICE, saved.getName(), saved.getId());
            ctx.putId(ctx.getTenantId(), EntityType.DEVICE, saved.getName(), saved.getId());
            saveAttributes(ctx, saved.getId(), def.getAttributes(), def.getSharedAttributes());
            addToGroupIfNamed(ctx, ownerId, EntityType.DEVICE, def.getGroup(), saved.getId());
            ctx.addPendingRelations(saved.getId(), def.getRelations());
            ctx.addEmulatorTarget(new EmulatorTarget(saved.getId(), saved.getName(), def.getEmulator(), false, saved));
        }
    }

    /**
     * Some solution dashboards hardcode links like "/api/images/system/&lt;key&gt;" or
     * "/api/resource/js_module/system/&lt;key&gt;" directly in widget settings (PE ships them the same
     * way), so the referenced images/JS modules have to exist under the system tenant - not the
     * installing tenant - before {@link #provisionDashboards} saves those widgets. Idempotent via
     * createOrUpdateSystem*: shared across every tenant that installs this solution, so only the
     * first install actually creates anything.
     */
    private void provisionResources(SolutionInstallContext ctx) {
        for (Path file : listSolutionFiles(ctx.getSolutionId(), "resources", "images")) {
            imageService.createOrUpdateSystemImage(file.getFileName().toString(), readBytes(file));
        }
        for (Path file : listSolutionFiles(ctx.getSolutionId(), "resources", "js_modules")) {
            resourceService.createOrUpdateSystemResource(ResourceType.JS_MODULE, ResourceSubType.EXTENSION,
                    file.getFileName().toString(), readBytes(file));
        }
    }

    private List<Path> listSolutionFiles(String solutionId, String... subdirs) {
        Path dir = resolve(solutionId, subdirs);
        if (!Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        try (var files = Files.list(dir)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            log.warn("Failed to list solution directory {}", dir, e);
            return Collections.emptyList();
        }
    }

    private byte[] readBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read solution resource file: " + file, e);
        }
    }

    private void provisionDashboards(SolutionInstallContext ctx) throws Exception {
        for (DashboardDefinition def : loadList(ctx, "dashboards.json", new TypeReference<List<DashboardDefinition>>() {})) {
            CustomerId customerId = resolveCustomer(ctx, def.getCustomer());
            JsonNode json = replaceIds(ctx, JacksonUtil.toJsonNode(resolve(ctx.getSolutionId(), "dashboards", def.getFile()).toFile()));
            Dashboard template = bind(json, Dashboard.class);
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(ctx.getTenantId());
            dashboard.setTitle(def.getName());
            dashboard.setConfiguration(template.getConfiguration());
            dashboard.setImage(template.getImage());
            Dashboard saved = dashboardService.saveDashboard(dashboard);
            ctx.registerCreated(saved.getId());
            ctx.registerJsonId(def.getJsonId(), saved.getId());
            EntityId ownerId = customerId != null ? customerId : ctx.getTenantId();
            ctx.putId(ownerId, EntityType.DASHBOARD, saved.getTitle(), saved.getId());
            if (customerId != null) {
                dashboardService.assignDashboardToCustomer(ctx.getTenantId(), saved.getId(), customerId);
            }
            EntityGroupId groupId = addToGroupIfNamed(ctx, ownerId, EntityType.DASHBOARD, def.getGroup(), saved.getId());
            if (def.isMain()) {
                ctx.getSolutionInstructions().setDashboardId(saved.getId());
                ctx.getSolutionInstructions().setDashboardGroupId(groupId);
                ctx.getSolutionInstructions().setMainDashboardPublic(def.isMakePublic());
            }
        }
    }

    /** Runs last: both ends of every relation are named, so all entities must already exist. */
    private void provisionRelations(SolutionInstallContext ctx) {
        for (Map.Entry<EntityId, List<RelationDefinition>> entry : ctx.getPendingRelations().entrySet()) {
            EntityId from = entry.getKey();
            for (RelationDefinition def : entry.getValue()) {
                // A TENANT-typed reference always means this install's tenant, whatever it's named.
                EntityId other = def.getEntityType() == EntityType.TENANT
                        ? ctx.getTenantId()
                        : ctx.resolveId(ctx.getTenantId(), def.getEntityType(), def.getEntityName());
                if (other == null) {
                    log.warn("[{}] Solution relation target not found: {} '{}'", ctx.getSolutionId(), def.getEntityType(), def.getEntityName());
                    continue;
                }
                boolean fromOther = def.getDirection() == null || def.getDirection() == EntitySearchDirection.FROM;
                EntityRelation relation = new EntityRelation(
                        fromOther ? other : from, fromOther ? from : other,
                        def.getType() != null ? def.getType() : EntityRelation.CONTAINS_TYPE,
                        RelationTypeGroup.COMMON);
                relationService.saveRelation(ctx.getTenantId(), relation);
            }
        }
    }

    /**
     * Resolves each entity's named emulator and backfills its history, waiting for all of them so
     * the install only reports success once the dashboards actually have data behind them.
     */
    private void runEmulators(SolutionInstallContext ctx) {
        if (ctx.getEmulatorTargets().isEmpty()) {
            return;
        }
        Map<String, EmulatorDefinition> deviceEmulators = loadEmulators(ctx, "device_emulators.json");
        Map<String, EmulatorDefinition> assetEmulators = loadEmulators(ctx, "asset_emulators.json");
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (EmulatorTarget target : ctx.getEmulatorTargets()) {
            EmulatorDefinition definition = (target.asset() ? assetEmulators : deviceEmulators).get(target.emulatorName());
            if (definition == null) {
                log.warn("[{}] Solution emulator not found: '{}'", ctx.getSolutionId(), target.emulatorName());
                continue;
            }
            futures.add(emulatorLauncher.launch(ctx.getTenantId(), target.entityId(), target.entityName(),
                    definition, target.device()));
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(EMULATOR_TIMEOUT_MIN, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Data generation is best effort — the entities and dashboards are already in place,
            // and failing the install here would roll back a perfectly good solution.
            log.warn("[{}][{}] Telemetry emulation did not finish cleanly", ctx.getTenantId(), ctx.getSolutionId(), e);
        }
        log.info("[{}][{}] Emulated telemetry for {} entities", ctx.getTenantId(), ctx.getSolutionId(), futures.size());
    }

    /** Resolves the {@code extendz} chain so every definition is self-contained before use. */
    private Map<String, EmulatorDefinition> loadEmulators(SolutionInstallContext ctx, String fileName) {
        Map<String, EmulatorDefinition> byName = new LinkedHashMap<>();
        for (EmulatorDefinition definition : loadList(ctx, fileName, new TypeReference<List<EmulatorDefinition>>() {})) {
            byName.put(definition.getName(), definition);
        }
        byName.values().stream()
                .filter(definition -> StringUtils.isNotEmpty(definition.getExtendz()))
                .forEach(definition -> {
                    EmulatorDefinition parent = byName.get(definition.getExtendz());
                    if (parent != null) {
                        definition.enrich(parent);
                    } else {
                        log.warn("[{}] Solution emulator parent not found: '{}'", ctx.getSolutionId(), definition.getExtendz());
                    }
                });
        return byName;
    }

    private void provisionSchedulerEvents(SolutionInstallContext ctx) {
        for (SchedulerEventDefinition def : loadList(ctx, "scheduler_events.json", new TypeReference<List<SchedulerEventDefinition>>() {})) {
            SchedulerEvent event = new SchedulerEvent();
            event.setTenantId(ctx.getTenantId());
            event.setName(def.getName());
            event.setType(def.getType());
            event.setSchedule(def.getSchedule());
            event.setConfiguration(def.getConfiguration());
            // The originator is written with the template's placeholder id, not a real one.
            if (def.getOriginatorId() != null) {
                String realId = ctx.getRealIds().get(def.getOriginatorId().getId().toString());
                if (realId == null) {
                    log.warn("[{}] Scheduler event '{}' originator not found, skipping", ctx.getSolutionId(), def.getName());
                    continue;
                }
                event.setOriginatorId(EntityIdFactory.getByTypeAndId(def.getOriginatorId().getEntityType().name(), realId));
            }
            ctx.getCreatedSchedulerEvents().add(schedulerEventService.saveSchedulerEvent(event).getId());
        }
    }

    /**
     * Both {@code calculated_fields} and {@code alarm_rules} hold {@link CalculatedField} objects
     * — alarm rules are simply the ones with {@code type: ALARM}. They come last because each is
     * attached to a device or asset profile by id.
     */
    private void provisionCalculatedFields(SolutionInstallContext ctx) {
        List<JsonNode> nodes = new ArrayList<>(loadJsonFromDirectory(ctx.getSolutionId(), "calculated_fields"));
        nodes.addAll(loadJsonFromDirectory(ctx.getSolutionId(), "alarm_rules"));
        for (JsonNode node : nodes) {
            CalculatedField cf = bind(replaceIds(ctx, node), CalculatedField.class);
            if (cf == null) {
                continue;
            }
            cf.setId(null);
            cf.setTenantId(ctx.getTenantId());
            ctx.registerCreated(calculatedFieldService.save(cf).getId());
        }
    }

    // ---------------------------------------------------------------- helpers

    private CustomerId resolveCustomer(SolutionInstallContext ctx, String customerName) {
        if (StringUtils.isBlank(customerName)) {
            return null;
        }
        EntityId id = ctx.getId(ctx.getTenantId(), EntityType.CUSTOMER, customerName);
        return id != null ? new CustomerId(id.getId()) : null;
    }

    /** Finds or creates the named entity group under the owner and adds the entity to it. */
    private EntityGroupId addToGroupIfNamed(SolutionInstallContext ctx, EntityId ownerId, EntityType groupType, String groupName, EntityId entityId) {
        if (StringUtils.isBlank(groupName)) {
            return null;
        }
        EntityGroupId groupId = findOrCreateGroup(ctx, ownerId, groupType, groupName);
        entityGroupService.addEntityToEntityGroup(ctx.getTenantId(), groupId, entityId);
        return groupId;
    }

    /**
     * Groups are reused when a template names the same one twice under the same owner, and only
     * groups this install actually created are registered for rollback — never a pre-existing one.
     * The owner is part of the key: sibling customers routinely declare identically named groups.
     */
    private EntityGroupId findOrCreateGroup(SolutionInstallContext ctx, EntityId ownerId, EntityType groupType, String groupName) {
        EntityGroupId cached = ctx.getGroupId(ownerId, groupType, groupName);
        if (cached != null) {
            return cached;
        }
        CustomerId ownerCustomerId = ownerId instanceof CustomerId customerId && !customerId.isNullUid() ? customerId : null;
        EntityGroup group = entityGroupService.findEntityGroupByTypeAndName(ctx.getTenantId(), ownerCustomerId, groupType, groupName);
        if (group == null) {
            EntityGroup newGroup = new EntityGroup();
            newGroup.setOwnerId(ownerCustomerId != null ? ownerCustomerId : ctx.getTenantId());
            newGroup.setType(groupType);
            newGroup.setName(groupName);
            group = entityGroupService.saveEntityGroup(ctx.getTenantId(), newGroup);
            ctx.registerCreated(group.getId());
        }
        ctx.putGroupId(ownerId, groupType, groupName, group.getId());
        return group.getId();
    }

    private void saveAttributes(SolutionInstallContext ctx, EntityId entityId, JsonNode serverAttributes, JsonNode sharedAttributes) throws Exception {
        saveAttributes(ctx, entityId, serverAttributes, AttributeScope.SERVER_SCOPE);
        saveAttributes(ctx, entityId, sharedAttributes, AttributeScope.SHARED_SCOPE);
    }

    private void saveAttributes(SolutionInstallContext ctx, EntityId entityId, JsonNode attributes, AttributeScope scope) throws Exception {
        if (attributes == null || !attributes.isObject() || attributes.isEmpty()) {
            return;
        }
        long ts = System.currentTimeMillis();
        List<AttributeKvEntry> entries = new ArrayList<>();
        attributes.fields().forEachRemaining(field -> entries.add(new BaseAttributeKvEntry(toKvEntry(field.getKey(), field.getValue()), ts)));
        attributesService.save(ctx.getTenantId(), entityId, scope, entries).get();
    }

    private KvEntry toKvEntry(String key, JsonNode value) {
        if (value.isBoolean()) {
            return new BooleanDataEntry(key, value.asBoolean());
        }
        if (value.isIntegralNumber()) {
            return new LongDataEntry(key, value.asLong());
        }
        if (value.isNumber()) {
            return new DoubleDataEntry(key, value.asDouble());
        }
        if (value.isObject() || value.isArray()) {
            return new JsonDataEntry(key, value.toString());
        }
        return new StringDataEntry(key, value.asText());
    }

    /** Repoints a profile's default rule chain from the template placeholder to the created chain. */
    private void remapRuleChain(SolutionInstallContext ctx, RuleChainId templateRuleChainId, Consumer<UUID> setter) {
        if (templateRuleChainId == null) {
            return;
        }
        String realId = ctx.getRealIds().get(templateRuleChainId.getId().toString());
        setter.accept(realId != null ? UUID.fromString(realId) : null);
    }

    /**
     * Rewrites template-local placeholder ids to the real ids created so far. Done as plain text
     * substitution because the placeholders appear all over dashboard/rule-chain JSON — inside
     * entity aliases, widget configs and node relations — not in a fixed set of fields.
     */
    private JsonNode replaceIds(SolutionInstallContext ctx, JsonNode json) {
        if (json == null || ctx.getRealIds().isEmpty()) {
            return json;
        }
        String text = json.toString();
        for (Map.Entry<String, String> entry : ctx.getRealIds().entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return JacksonUtil.toJsonNode(text);
    }

    private <T> List<T> loadList(SolutionInstallContext ctx, String fileName, TypeReference<List<T>> type) {
        String json = readEntitiesFile(ctx, fileName);
        if (json == null) {
            return Collections.emptyList();
        }
        List<T> result = fromJson(json, type);
        return result != null ? result : Collections.emptyList();
    }

    private <T> T loadOne(SolutionInstallContext ctx, String fileName, Class<T> type) {
        String json = readEntitiesFile(ctx, fileName);
        return json != null ? fromJson(json, type) : null;
    }

    private String readEntitiesFile(SolutionInstallContext ctx, String fileName) {
        Path file = resolve(ctx.getSolutionId(), "entities", fileName);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return resolveTokens(ctx, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read solution file " + file, e);
        }
    }

    /**
     * Expands the placeholder tokens the templates use for values that cannot be baked into a
     * shipped file: per-install unique emails, the installing user's own identity, and timestamps.
     * Substitution is deliberately deterministic for a given install — several steps re-read the
     * same file and must see identical values.
     */
    private String resolveTokens(SolutionInstallContext ctx, String content) {
        if (!content.contains("$")) {
            return content;
        }
        User user = ctx.getUser();
        // Longest tokens first: "$randomEmail" also starts with "$random".
        content = replaceEach(content, "$randomEmail", i -> "user." + ctx.getInstallSuffix() + "." + i + "@thingsboard.org");
        content = replaceEach(content, "$randomFirstName", i -> FIRST_NAMES.get(i % FIRST_NAMES.size()));
        content = replaceEach(content, "$randomLastName", i -> LAST_NAMES.get(i % LAST_NAMES.size()));
        // Plus-addressed off the installing user so the demo logins still reach their inbox while
        // staying unique — emails are globally unique, and a template may use this token twice.
        content = replaceEach(content, "$customerEmail", i -> toPlusAddress(user.getEmail(), ctx.getInstallSuffix() + "." + i));
        content = content.replace("$customerFirstName", StringUtils.isNotBlank(user.getFirstName()) ? user.getFirstName() : "Customer");
        content = content.replace("$customerLastName", StringUtils.isNotBlank(user.getLastName()) ? user.getLastName() : "User");
        content = content.replace("$random", ctx.getInstallSuffix());
        long now = System.currentTimeMillis();
        content = content.replace("${currentTime+6m}", Long.toString(now + SIX_MONTHS_MS));
        content = content.replace("${currentTime}", Long.toString(now));
        return content;
    }

    private String toPlusAddress(String email, String tag) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) + "+" + tag + email.substring(at) : "user." + tag + "@thingsboard.org";
    }

    /** Replaces every occurrence of {@code token} with a value derived from its position. */
    private String replaceEach(String content, String token, java.util.function.IntFunction<String> valueByIndex) {
        StringBuilder result = new StringBuilder();
        int from = 0;
        for (int i = 0, at; (at = content.indexOf(token, from)) >= 0; i++) {
            result.append(content, from, at).append(valueByIndex.apply(i));
            from = at + token.length();
        }
        return from == 0 ? content : result.append(content.substring(from)).toString();
    }

    private List<JsonNode> loadJsonFromDirectory(String solutionId, String dirName) {
        Path dir = resolve(solutionId, dirName);
        if (!Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<>();
        try (var files = Files.list(dir)) {
            files.filter(p -> p.toString().endsWith(".json")).sorted().forEach(p -> {
                JsonNode node = JacksonUtil.toJsonNode(p.toFile());
                if (node != null) {
                    result.add(node);
                }
            });
        } catch (IOException e) {
            log.warn("Failed to list solution directory {}", dir, e);
        }
        return result;
    }

    private String toCreatedEntitiesKey(String solutionId) {
        return solutionId + "_entities";
    }

    private String toCreatedSchedulerEventsKey(String solutionId) {
        return solutionId + "_scheduler_events";
    }

    private boolean isInstalled(List<AttributeKvEntry> stateList, String solutionId) {
        String key = toStatusKey(solutionId);
        return stateList.stream()
                .filter(attr -> attr.getKey().equals(key))
                .findFirst()
                .flatMap(AttributeKvEntry::getBooleanValue)
                .orElse(Boolean.FALSE);
    }

    private Path resolve(String subdir, String... subdirs) {
        return getSolutionsDir().resolve(Paths.get(subdir, subdirs));
    }

    private Path getSolutionsDir() {
        return Paths.get(installScripts.getDataDir(), JSON_DIR, SOLUTIONS_DIR);
    }

    /** Markdown bodies are optional per template; a missing file yields an empty section. */
    private String readFile(Path file) {
        try {
            return Files.exists(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            log.warn("Failed to read solution file {}", file, e);
            return "";
        }
    }

    // Per-tenant install state lives in tenant server-scope attributes, matching PE's key naming.
    private String toStatusKey(String solutionId) {
        return solutionId + "_status";
    }

    private String toInstructionsKey(String solutionId) {
        return solutionId + "_instructions";
    }

}
