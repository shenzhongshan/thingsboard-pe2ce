///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityGroup } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { EntityGroupComponent } from '@home/pages/entity-group/entity-group.component';
import { EntityGroupTabsComponent } from '@home/pages/entity-group/entity-group-tabs.component';
import { forkJoin, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { EntityId } from '@shared/models/id/entity-id';

/**
 * Builds the standalone EntityTableConfig<EntityGroup> that drives the entity-group details drawer
 * (tb-entity-details-panel), shared by the group member view's pencil button and the group list
 * page's row edit pencil so both open the identical PE-parity drawer.
 */
export function createGroupDetailsConfig(entityGroupService: EntityGroupService,
                                         onEntityAction: (action: EntityAction<EntityGroup>) => boolean,
                                         showOpenButton = true): EntityTableConfig<EntityGroup> {
  const config = new EntityTableConfig<EntityGroup>();
  config.entityComponent = EntityGroupComponent;
  config.entityTabsComponent = EntityGroupTabsComponent;
  config.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_GROUP);
  config.entityResources = {helpLinkId: 'entityGroups'};
  config.loadEntity = id => entityGroupService.getEntityGroup(id.id);
  config.saveEntity = group => {
    const pending = group as EntityGroup & {pendingFirmwareId?: EntityId; pendingSoftwareId?: EntityId};
    const firmwareId = pending.pendingFirmwareId;
    const softwareId = pending.pendingSoftwareId;
    delete pending.pendingFirmwareId;
    delete pending.pendingSoftwareId;
    return entityGroupService.saveEntityGroup(group as EntityGroup).pipe(
      mergeMap(saved => {
        const tasks: Observable<any>[] = [];
        if (firmwareId !== undefined) {
          tasks.push(saveOrClearOtaPackage(entityGroupService, saved.id.id, OtaUpdateType.FIRMWARE, firmwareId));
        }
        if (softwareId !== undefined) {
          tasks.push(saveOrClearOtaPackage(entityGroupService, saved.id.id, OtaUpdateType.SOFTWARE, softwareId));
        }
        return tasks.length ? forkJoin(tasks).pipe(mergeMap(() => of(saved))) : of(saved);
      })
    );
  };
  config.detailsReadonly = () => false;
  // PE keeps 详情/列/设置/操作 visible (and editable together) while editing, and only hides the
  // remaining view-only tabs (权限/属性/最新遥测/审计日志/版本控制) — handled inside
  // EntityGroupTabsComponent's own @if(!isEdit) blocks, so the framework must not collapse the
  // whole tab strip to headless-single-tab mode the way it does for other entity types.
  config.hideDetailsTabsOnEdit = false;
  config.onEntityAction = onEntityAction;
  // 打开实体组 navigates INTO the group's member/device list. PE only shows it when the drawer was
  // reached from the top-level groups list (EntityGroupsComponent) — there you haven't entered the
  // group's contents yet, so "open" is meaningful. When reached from the group's own member/device
  // list (GroupMembersPageComponent), you're already viewing its contents, so PE omits the button.
  config.componentsData = {showOpenButton};
  return config;
}

function saveOrClearOtaPackage(entityGroupService: EntityGroupService, groupId: string,
                               type: OtaUpdateType, otaPackageId: EntityId | null): Observable<any> {
  if (otaPackageId) {
    return entityGroupService.saveDeviceGroupOtaPackage({
      groupId: {entityType: EntityType.ENTITY_GROUP, id: groupId},
      otaPackageType: type,
      otaPackageId: {entityType: EntityType.OTA_PACKAGE, id: otaPackageId.id}
    });
  }
  return entityGroupService.deleteDeviceGroupOtaPackage(groupId, type, {ignoreErrors: true});
}
