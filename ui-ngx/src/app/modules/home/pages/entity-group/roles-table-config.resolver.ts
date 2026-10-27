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

import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { DateEntityTableColumn, EntityTableColumn, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RoleService } from '@core/http/role.service';
import { PermissionService } from '@core/http/permission.service';
import { Role, roleTypeTranslations } from '@shared/models/role.models';
import { RoleComponent } from '@home/pages/entity-group/role.component';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';

@Injectable()
export class RolesTableConfigResolver {

  private readonly config: EntityTableConfig<Role> = new EntityTableConfig<Role>();

  constructor(private roleService: RoleService,
              private permissionService: PermissionService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private customTranslate: CustomTranslatePipe) {

    this.config.entityType = EntityType.ROLE;
    this.config.entityComponent = RoleComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ROLE);
    this.config.entityResources = entityTypeResources.get(EntityType.ROLE);

    this.config.columns.push(
      new DateEntityTableColumn<Role>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Role>('name', 'role.name', '40%'),
      new EntityTableColumn<Role>('type', 'role.role-type', '15%',
        entity => this.translate.instant(roleTypeTranslations.get(entity.type))),
      new EntityTableColumn<Role>('description', 'role.description', '45%',
        entity => this.customTranslate.transform(entity.additionalInfo?.description || ''))
    );

    this.config.deleteEntityTitle = role => this.translate.instant('role.delete-role-title', {roleName: role.name});
    this.config.deleteEntityContent = () => this.translate.instant('role.delete-role-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('role.delete-roles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('role.delete-roles-text');

    this.config.entitiesFetchFunction = pageLink => this.roleService.getRoles(pageLink);
    this.config.loadEntity = id => this.roleService.getRole(id.id);
    this.config.saveEntity = role => this.roleService.saveRole(role);
    this.config.deleteEntity = id => this.roleService.deleteRole(id.id);
    this.config.onEntityAction = action => this.onRoleAction(action);
  }

  resolve(): Observable<EntityTableConfig<Role>> {
    this.config.tableTitle = this.translate.instant('role.roles');
    return this.permissionService.getAllowedPermissions().pipe(
      map((info) => {
        this.config.componentsData = {
          allowedResources: (info.allowedResources || []).slice().sort(),
          groupRoleOperations: (info.allowedForGroupRoleOperations || []).slice().sort(),
          operationsByResource: info.operationsByResource || {}
        };
        return this.config;
      })
    );
  }

  private openRole($event: Event, role: Role) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([role.id.id], {relativeTo: this.config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  onRoleAction(action: EntityAction<Role>): boolean {
    switch (action.action) {
      case 'open':
        this.openRole(action.event, action.entity);
        return true;
    }
    return false;
  }

}
