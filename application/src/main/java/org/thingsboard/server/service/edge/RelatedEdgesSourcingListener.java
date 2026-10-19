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
package org.thingsboard.server.service.edge;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.edge.RelatedEdgesService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RelatedEdgesSourcingListener {

    private final RelatedEdgesService relatedEdgesService;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        log.debug("RelatedEdgesSourcingListener initiated");
        executorService = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("related-edges-listener"));
    }

    @PreDestroy
    public void destroy() {
        log.debug("RelatedEdgesSourcingListener destroy");
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent<?> event) {
        switch (event.getActionType()) {
            case ASSIGNED_TO_EDGE, UNASSIGNED_FROM_EDGE -> {
                executorService.submit(() -> {
                    log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
                    try {
                        // 分组被分配/取消分配给 edge 时，组内**全部成员**的 related-edges
                        // 结果都变了（查询是"直连 ∪ 分组两跳"）。事件的 entityId 是分组 id，
                        // 只清它等于没清成员的。成员可能很多，所以按租户前缀整体清空 ——
                        // 分组分配是低频的管理操作，这个代价可以接受。
                        if (EntityType.ENTITY_GROUP.equals(event.getEntityId().getEntityType())) {
                            relatedEdgesService.publishEdgeIdsEvictEventByTenantId(event.getTenantId());
                        }
                        relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
                    } catch (Exception e) {
                        log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event, e);
                    }
                });
            }
            // 成员增删只影响这一个实体，精确清它即可。这两个 action 来自
            // BaseEntityGroupService 的 addRelation/removeRelation。
            case ADDED_TO_ENTITY_GROUP, REMOVED_FROM_ENTITY_GROUP -> {
                executorService.submit(() -> {
                    log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
                    try {
                        relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
                    } catch (Exception e) {
                        log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event, e);
                    }
                });
            }
        }
    }

    @TransactionalEventListener(
            fallbackExecution = true,
            condition = "#event.entityId.getEntityType() != T(org.thingsboard.server.common.data.EntityType).AI_MODEL"
    )
    public void handleEvent(DeleteEntityEvent<?> event) {
        executorService.submit(() -> {
            log.trace("[{}] DeleteEntityEvent called: {}", event.getTenantId(), event);
            try {
                // 删除分组同样会改变其成员的 related-edges 结果，而成员关系此刻已随分组一起
                // 消失、无从枚举 —— 只能整租户清空。与 PE 的同一处理一致。
                if (EntityType.ENTITY_GROUP.equals(event.getEntityId().getEntityType())) {
                    relatedEdgesService.publishEdgeIdsEvictEventByTenantId(event.getTenantId());
                }
                relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
            } catch (Exception e) {
                log.error("[{}] failed to process DeleteEntityEvent: {}", event.getTenantId(), event, e);
            }
        });
    }

}
