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
package org.thingsboard.server.service.solutions.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.solutions.data.definition.EntitySearchKey;
import org.thingsboard.server.service.solutions.data.definition.RelationDefinition;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mutable state for one install run.
 * <p>
 * Three things are tracked as entities are created:
 * <ul>
 *   <li>{@code createdEntities} — every id, newest last, so a failure can be rolled back and a
 *       later uninstall can delete exactly what was created;</li>
 *   <li>{@code realIds} — template-local placeholder id to real UUID, used to rewrite the
 *       dashboard/rule-chain JSON that cross-references entities;</li>
 *   <li>{@code entityIds} — (owner, type, name) to real id, used to resolve name-based
 *       references such as relations, which can only be created once both ends exist.</li>
 * </ul>
 */
@RequiredArgsConstructor
@Getter
public class SolutionInstallContext {

    private final TenantId tenantId;
    private final String solutionId;
    private final User user;
    private final TenantSolutionTemplateInstructions solutionInstructions = new TenantSolutionTemplateInstructions();
    /**
     * Makes this install's generated emails unique. Fixed for the whole run so that steps which
     * re-read the same template file resolve the same values.
     */
    private final String installSuffix = UUID.randomUUID().toString().substring(0, 8);

    /** Creation order; rollback walks it in reverse. */
    private final List<EntityId> createdEntities = new ArrayList<>();
    /**
     * Tracked apart from {@link #createdEntities} because CE has no {@code SCHEDULER_EVENT} entity
     * type — {@code SchedulerEventId} is a bare {@code UUIDBased}, not an {@link EntityId}.
     */
    private final List<SchedulerEventId> createdSchedulerEvents = new ArrayList<>();
    private final Map<String, String> realIds = new HashMap<>();
    private final Map<EntitySearchKey, EntityId> entityIds = new HashMap<>();
    /**
     * Entity groups touched by this run, keyed by (type, name) with no owner: a template names a
     * group once and then refers to it from anywhere, including across owners when sharing.
     */
    private final Map<EntitySearchKey, EntityGroupId> groupIds = new HashMap<>();
    /** Relations are deferred until every entity exists, since they reference peers by name. */
    private final Map<EntityId, List<RelationDefinition>> pendingRelations = new LinkedHashMap<>();
    /** Entities that asked for generated history, resolved and run at the end of the install. */
    private final List<EmulatorTarget> emulatorTargets = new ArrayList<>();

    /**
     * An entity awaiting telemetry backfill. {@code device} is set only for devices — it carries
     * the fields needed to keep them reporting active once the backfill ends.
     */
    public record EmulatorTarget(EntityId entityId, String entityName, String emulatorName, boolean asset,
                                 org.thingsboard.server.common.data.Device device) {
    }

    public void addEmulatorTarget(EmulatorTarget target) {
        if (target.emulatorName() != null && !target.emulatorName().isBlank()) {
            emulatorTargets.add(target);
        }
    }

    public void registerCreated(EntityId entityId) {
        if (entityId != null) {
            createdEntities.add(entityId);
        }
    }

    /** Records the placeholder-to-real id mapping used when rewriting template JSON. */
    public void registerJsonId(String jsonId, EntityId entityId) {
        if (jsonId != null && entityId != null) {
            realIds.put(jsonId, entityId.getId().toString());
        }
    }

    public void putId(EntityId ownerId, EntityType entityType, String name, EntityId entityId) {
        entityIds.put(EntitySearchKey.of(ownerId, entityType, name), entityId);
    }

    public EntityId getId(EntityId ownerId, EntityType entityType, String name) {
        return entityIds.get(EntitySearchKey.of(ownerId, entityType, name));
    }

    /**
     * Looks a name up under the given owner and, failing that, under the tenant — templates
     * routinely reference tenant-level entities from customer-owned ones.
     */
    public EntityId resolveId(EntityId ownerId, EntityType entityType, String name) {
        EntityId id = getId(ownerId, entityType, name);
        return id != null ? id : getId(tenantId, entityType, name);
    }

    public void putGroupId(EntityId ownerId, EntityType groupType, String name, EntityGroupId groupId) {
        groupIds.put(EntitySearchKey.ofGroup(ownerId, groupType, name), groupId);
    }

    public EntityGroupId getGroupId(EntityId ownerId, EntityType groupType, String name) {
        return groupIds.get(EntitySearchKey.ofGroup(ownerId, groupType, name));
    }

    /**
     * Finds a group by name regardless of owner, preferring the tenant's. Group role grants are
     * written from a customer's user group but normally target a tenant-owned dashboard group.
     */
    public EntityGroupId resolveGroupId(EntityType groupType, String name) {
        EntityGroupId tenantOwned = getGroupId(tenantId, groupType, name);
        if (tenantOwned != null) {
            return tenantOwned;
        }
        return groupIds.entrySet().stream()
                .filter(e -> e.getKey().entityType() == groupType && e.getKey().entityName().equals(name))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    public void addPendingRelations(EntityId entityId, List<RelationDefinition> relations) {
        if (relations != null && !relations.isEmpty()) {
            pendingRelations.put(entityId, relations);
        }
    }

}
