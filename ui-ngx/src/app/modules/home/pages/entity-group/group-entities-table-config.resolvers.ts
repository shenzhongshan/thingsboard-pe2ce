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

import { Injectable, Injector } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DialogService } from '@core/services/dialog.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, isGroupAll } from '@shared/models/entity-group.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { DeviceInfo, Device } from '@shared/models/device.models';
import { AssetInfo } from '@shared/models/asset.models';
import { Customer } from '@shared/models/customer.model';
import { User } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { UserService } from '@core/http/user.service';
import { DevicesTableConfigResolver } from '@home/pages/device/devices-table-config.resolver';
import { AssetsTableConfigResolver } from '@home/pages/asset/assets-table-config.resolver';
import { CustomersTableConfigResolver } from '@home/pages/customer/customers-table-config.resolver';
import { UsersTableConfigResolver } from '@home/pages/user/users-table-config.resolver';
import { DeviceWizardDialogComponent } from '@home/components/wizard/device-wizard-dialog.component';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { AddUserDialogComponent, AddUserDialogData } from '@modules/home/pages/user/add-user-dialog.component';
import {
  AddGroupEntitiesDialogComponent,
  AddGroupEntitiesDialogData
} from '@home/pages/entity-group/add-group-entities-dialog.component';

/**
 * Table configs for entities inside an entity group. Each resolver instantiates a FRESH copy of
 * the corresponding standard table config resolver (they hold a singleton config that the plain
 * pages share — patching that in place would leak group behavior into the main tables) and then
 * repoints fetching at the group membership API and augments add/remove actions.
 */
abstract class GroupEntitiesTableConfigResolver<T extends BaseData<HasId>> {

  protected constructor(protected injector: Injector,
                        protected entityGroupService: EntityGroupService,
                        protected translate: TranslateService,
                        protected dialogService: DialogService,
                        protected dialog: MatDialog) {
  }

  protected abstract resolveBase(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<T>>;

  protected abstract entityTypeTitle(): string;

  /** Configure how a brand-new entity is created from inside a non-All group. */
  protected abstract configureGroupAdd(config: EntityTableConfig<T>, group: EntityGroup): void;

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<T>> {
    const entityGroupId: string = route.params.entityGroupId;
    return forkJoin({
      config: this.resolveBase(route),
      group: this.entityGroupService.getEntityGroup(entityGroupId)
    }).pipe(map(({config, group}) => this.patch(config, group)));
  }

  private patch(config: EntityTableConfig<T>, group: EntityGroup): EntityTableConfig<T> {
    config.tableTitle = group.name + ': ' + this.translate.instant(this.entityTypeTitle());
    config.entitiesFetchFunction = pageLink =>
      this.entityGroupService.getEntityGroupEntities<T>(group.id.id, pageLink);
    if (!isGroupAll(group)) {
      this.configureGroupAdd(config, group);
      config.addActionDescriptors = [
        {
          name: this.translate.instant('entity-group.create-new-entity'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: () => config.getTable().addEntity(null)
        },
        {
          name: this.translate.instant('entity-group.add-existing-entities'),
          icon: 'playlist_add',
          isEnabled: () => true,
          onAction: () => this.addExisting(config, group)
        }
      ];
      config.cellActionDescriptors = config.cellActionDescriptors.concat({
        name: this.translate.instant('entity-group.remove-from-group'),
        icon: 'remove_circle_outline',
        isEnabled: () => true,
        onAction: ($event, entity) => this.removeFromGroup($event, config, group, [entity])
      });
      config.groupActionDescriptors = config.groupActionDescriptors.concat({
        name: this.translate.instant('entity-group.remove-from-group'),
        icon: 'remove_circle_outline',
        isEnabled: true,
        onAction: ($event, entities) => this.removeFromGroup($event, config, group, entities)
      });
    }
    return config;
  }

  protected addToGroup<E extends BaseData<HasId>>(group: EntityGroup, entity: E): Observable<E> {
    if (!entity) {
      return of(entity);
    }
    return this.entityGroupService.addEntitiesToEntityGroup(group.id.id, [entity.id.id as string])
      .pipe(map(() => entity));
  }

  private addExisting(config: EntityTableConfig<T>, group: EntityGroup) {
    this.dialog.open<AddGroupEntitiesDialogComponent, AddGroupEntitiesDialogData, boolean>(
      AddGroupEntitiesDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { entityGroupId: group.id.id, groupType: group.type }
      }).afterClosed().subscribe((added) => {
      if (added) {
        config.updateData();
      }
    });
  }

  private removeFromGroup($event: Event, config: EntityTableConfig<T>, group: EntityGroup, entities: T[]) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entity-group.remove-from-group-title', {count: entities.length}),
      this.translate.instant('entity-group.remove-from-group-text', {entityGroupName: group.name}),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.entityGroupService.removeEntitiesFromEntityGroup(group.id.id, entities.map(e => e.id.id as string))
          .subscribe(() => config.updateData());
      }
    });
  }

  /** Default create-new flow: the generic add-entity dialog, then membership. */
  protected genericDialogAdd(config: EntityTableConfig<T>, group: EntityGroup): void {
    config.addEntity = () => this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<T>, T>(
      AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { entitiesTableConfig: config }
      }).afterClosed().pipe(mergeMap(entity => this.addToGroup(group, entity)));
  }
}

@Injectable()
export class DeviceGroupEntitiesTableConfigResolver extends GroupEntitiesTableConfigResolver<DeviceInfo> {

  constructor(injector: Injector, entityGroupService: EntityGroupService, translate: TranslateService,
              dialogService: DialogService, dialog: MatDialog) {
    super(injector, entityGroupService, translate, dialogService, dialog);
  }

  protected resolveBase(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DeviceInfo>> {
    return Injector.create({providers: [DevicesTableConfigResolver], parent: this.injector})
      .get(DevicesTableConfigResolver).resolve(route);
  }

  protected entityTypeTitle(): string {
    return 'device.devices';
  }

  protected configureGroupAdd(config: EntityTableConfig<DeviceInfo>, group: EntityGroup): void {
    config.addEntity = () => {
      this.dialog.open<DeviceWizardDialogComponent, AddEntityDialogData<BaseData<HasId>>, Device>(
        DeviceWizardDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
        }).afterClosed().pipe(
        mergeMap(device => this.addToGroup(group, device))
      ).subscribe((device) => {
        if (device) {
          config.updateData();
        }
      });
      return of(null);
    };
  }
}

@Injectable()
export class AssetGroupEntitiesTableConfigResolver extends GroupEntitiesTableConfigResolver<AssetInfo> {

  constructor(injector: Injector, entityGroupService: EntityGroupService, translate: TranslateService,
              dialogService: DialogService, dialog: MatDialog) {
    super(injector, entityGroupService, translate, dialogService, dialog);
  }

  protected resolveBase(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AssetInfo>> {
    return Injector.create({providers: [AssetsTableConfigResolver], parent: this.injector})
      .get(AssetsTableConfigResolver).resolve(route);
  }

  protected entityTypeTitle(): string {
    return 'asset.assets';
  }

  protected configureGroupAdd(config: EntityTableConfig<AssetInfo>, group: EntityGroup): void {
    this.genericDialogAdd(config, group);
  }
}

@Injectable()
export class CustomerGroupEntitiesTableConfigResolver extends GroupEntitiesTableConfigResolver<Customer> {

  constructor(injector: Injector, entityGroupService: EntityGroupService, translate: TranslateService,
              dialogService: DialogService, dialog: MatDialog) {
    super(injector, entityGroupService, translate, dialogService, dialog);
  }

  protected resolveBase(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<Customer>> {
    return Injector.create({providers: [CustomersTableConfigResolver], parent: this.injector})
      .get(CustomersTableConfigResolver).resolve(route);
  }

  protected entityTypeTitle(): string {
    return 'customer.customers';
  }

  protected configureGroupAdd(config: EntityTableConfig<Customer>, group: EntityGroup): void {
    this.genericDialogAdd(config, group);
  }
}

/**
 * Shared base for the top-level user tables (the standard users config is tied to
 * tenant-admins-of-tenant / users-of-customer contexts, so both the "All" tab and group views
 * repoint fetching and creation at the whole-tenant users API).
 */
abstract class TenantUsersTableConfigResolver extends GroupEntitiesTableConfigResolver<User> {

  protected constructor(injector: Injector, entityGroupService: EntityGroupService, translate: TranslateService,
                        dialogService: DialogService, dialog: MatDialog,
                        protected userService: UserService, protected store: Store<AppState>) {
    super(injector, entityGroupService, translate, dialogService, dialog);
  }

  protected resolveBase(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<User>> {
    return Injector.create({providers: [UsersTableConfigResolver], parent: this.injector})
      .get(UsersTableConfigResolver).resolve(route).pipe(map(config => this.patchUsersConfig(config)));
  }

  protected entityTypeTitle(): string {
    return 'user.users';
  }

  protected patchUsersConfig(config: EntityTableConfig<User>): EntityTableConfig<User> {
    // The base resolver guesses a customer-users context here; repoint at whole-tenant users and
    // keep edits from overwriting authority/customer of existing users.
    config.tableTitle = this.translate.instant('user.users');
    config.entitiesFetchFunction = pageLink => this.userService.getUsers(pageLink);
    config.saveEntity = user => this.userService.saveUser(user);
    config.addEntity = () => this.addTenantUser();
    return config;
  }

  protected addTenantUser(): Observable<User> {
    const authUser = getCurrentAuthUser(this.store);
    return this.dialog.open<AddUserDialogComponent, AddUserDialogData, User>(
      AddUserDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          tenantId: authUser.tenantId,
          customerId: NULL_UUID,
          authority: Authority.TENANT_ADMIN
        }
      }).afterClosed();
  }

  protected configureGroupAdd(config: EntityTableConfig<User>, group: EntityGroup): void {
    config.addEntity = () => this.addTenantUser().pipe(mergeMap(user => this.addToGroup(group, user)));
  }
}

@Injectable()
export class AllUsersTableConfigResolver extends TenantUsersTableConfigResolver {

  constructor(injector: Injector, entityGroupService: EntityGroupService, translate: TranslateService,
              dialogService: DialogService, dialog: MatDialog, userService: UserService, store: Store<AppState>) {
    super(injector, entityGroupService, translate, dialogService, dialog, userService, store);
  }

  // Plain tenant users table — no group context at all.
  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<User>> {
    return this.resolveBase(route);
  }
}

@Injectable()
export class UserGroupEntitiesTableConfigResolver extends TenantUsersTableConfigResolver {

  constructor(injector: Injector, entityGroupService: EntityGroupService, translate: TranslateService,
              dialogService: DialogService, dialog: MatDialog, userService: UserService, store: Store<AppState>) {
    super(injector, entityGroupService, translate, dialogService, dialog, userService, store);
  }
}
