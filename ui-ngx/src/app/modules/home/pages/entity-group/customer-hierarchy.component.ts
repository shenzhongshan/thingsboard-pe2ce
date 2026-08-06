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

import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroup, EntityGroupId, isGroupAll } from '@shared/models/entity-group.models';
import { Customer } from '@shared/models/customer.model';
import { EntityGroupService } from '@core/http/entity-group.service';
import { CustomerService } from '@core/http/customer.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { GroupEntityTableHeaderComponent } from '@home/pages/entity-group/group-entity-table-header.component';
import { createGroupDetailsConfig } from '@home/pages/entity-group/entity-group-details-config.util';
import {
  AssetGroupEntitiesTableConfigResolver,
  CustomerGroupEntitiesTableConfigResolver,
  DashboardGroupEntitiesTableConfigResolver,
  DeviceGroupEntitiesTableConfigResolver,
  EntityViewGroupEntitiesTableConfigResolver,
  UserGroupEntitiesTableConfigResolver
} from '@home/pages/entity-group/group-entities-table-config.resolvers';

type NodeKind = 'customerGroup' | 'customer' | 'typeFolder' | 'entityGroup';

interface HierarchyNode {
  kind: NodeKind;
  name: string;
  icon: string;
  level: number;
  expandable: boolean;
  expanded: boolean;
  loading: boolean;
  children: HierarchyNode[];
  parent?: HierarchyNode;
  group?: EntityGroup;
  customer?: Customer;
  groupType?: EntityType;
  customerId?: string;
}

const groupTypeFolders: Array<{type: EntityType; nameKey: string; icon: string}> = [
  {type: EntityType.USER, nameKey: 'entity-group.group-type-user', icon: 'account_circle'},
  {type: EntityType.CUSTOMER, nameKey: 'entity-group.group-type-customer', icon: 'supervisor_account'},
  {type: EntityType.ASSET, nameKey: 'entity-group.group-type-asset', icon: 'domain'},
  {type: EntityType.DEVICE, nameKey: 'entity-group.group-type-device', icon: 'devices_other'},
  {type: EntityType.ENTITY_VIEW, nameKey: 'entity-group.group-type-entity-view', icon: 'view_quilt'},
  {type: EntityType.DASHBOARD, nameKey: 'entity-group.group-type-dashboard', icon: 'dashboard'}
];

const groupTypeIcons = new Map<EntityType, string>(groupTypeFolders.map(f => [f.type, f.icon]));

@Component({
  selector: 'tb-customer-hierarchy',
  templateUrl: './customer-hierarchy.component.html',
  styleUrls: ['./customer-hierarchy.component.scss'],
  standalone: false
})
export class CustomerHierarchyComponent extends PageComponent implements OnInit {

  rootNodes: HierarchyNode[] = [];
  // flat pre-order projection of the expanded tree — what the template renders
  visibleNodes: HierarchyNode[] = [];
  selectedNode: HierarchyNode = null;
  treeCollapsed = false;

  // right panel state
  tableConfig: EntityTableConfig<any> = null;
  folderNode: HierarchyNode = null;

  // entityGroup node's own details drawer (pencil next to "{name}: {type}" in the header,
  // matches PE's tree-node-selected view) — kept separate from tableConfig's own row-details
  // drawer, same "two independent co-existing drawers" pattern as the standalone group pages.
  groupDetailsConfig: EntityTableConfig<EntityGroup>;
  groupDetailsOpen = false;
  editingGroupId: EntityGroupId;
  private editingGroupNode: HierarchyNode;

  EntityType = EntityType;

  constructor(protected store: Store<AppState>,
              private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private customerGroupResolver: CustomerGroupEntitiesTableConfigResolver,
              private deviceGroupResolver: DeviceGroupEntitiesTableConfigResolver,
              private assetGroupResolver: AssetGroupEntitiesTableConfigResolver,
              private entityViewGroupResolver: EntityViewGroupEntitiesTableConfigResolver,
              private dashboardGroupResolver: DashboardGroupEntitiesTableConfigResolver,
              private userGroupResolver: UserGroupEntitiesTableConfigResolver) {
    super(store);
    this.groupDetailsConfig = createGroupDetailsConfig(this.entityGroupService, action => this.onGroupAction(action), false);
  }

  ngOnInit() {
    this.loadRoot();
  }

  private pageLink(): PageLink {
    return new PageLink(100, 0, null, { property: 'name', direction: Direction.ASC });
  }

  // Customer's JPA-mapped sort column is 'title', not 'name' (unlike entity groups).
  private customerPageLink(): PageLink {
    return new PageLink(100, 0, null, { property: 'title', direction: Direction.ASC });
  }

  private updateVisibleNodes() {
    const flat: HierarchyNode[] = [];
    const walk = (nodes: HierarchyNode[]) => {
      for (const node of nodes) {
        flat.push(node);
        if (node.expanded && node.children?.length) {
          walk(node.children);
        }
      }
    };
    walk(this.rootNodes);
    this.visibleNodes = flat;
  }

  private loadRoot() {
    this.entityGroupService.getEntityGroupsByType(EntityType.CUSTOMER, this.pageLink()).subscribe((page) => {
      this.rootNodes = page.data.map(group => ({
        kind: 'customerGroup' as NodeKind,
        name: group.name,
        icon: 'supervisor_account',
        level: 0,
        expandable: true,
        expanded: false,
        loading: false,
        children: null,
        group
      }));
      this.updateVisibleNodes();
      // PE preselects the customers "All" group
      const all = this.rootNodes.find(n => n.group.name === 'All') || this.rootNodes[0];
      if (all) {
        this.select(all);
      }
    });
  }

  toggle(node: HierarchyNode) {
    if (!node.expandable) {
      return;
    }
    if (node.expanded) {
      node.expanded = false;
      this.updateVisibleNodes();
      return;
    }
    node.expanded = true;
    if (node.children === null && !node.loading) {
      this.loadChildren(node);
    }
    this.updateVisibleNodes();
  }

  private loadChildren(node: HierarchyNode, onLoaded?: () => void) {
    node.loading = true;
    const done = () => {
      node.loading = false;
      this.updateVisibleNodes();
      onLoaded?.();
    };
    switch (node.kind) {
      case 'customerGroup':
        this.entityGroupService.getEntityGroupEntities<Customer>(node.group.id.id, this.pageLink()).subscribe((page) => {
          node.children = page.data.map(customer => ({
            kind: 'customer' as NodeKind,
            name: customer.title,
            icon: 'supervisor_account',
            level: node.level + 1,
            expandable: true,
            expanded: false,
            loading: false,
            children: null,
            parent: node,
            customer
          }));
          done();
        });
        break;
      case 'customer':
        this.customerService.getCustomerChildren(node.customer.id.id, this.customerPageLink()).subscribe((page) => {
          const subCustomerNodes: HierarchyNode[] = page.data.map(customer => ({
            kind: 'customer' as NodeKind,
            name: customer.title,
            icon: 'supervisor_account',
            level: node.level + 1,
            expandable: true,
            expanded: false,
            loading: false,
            children: null,
            parent: node,
            customer
          }));
          const typeFolderNodes: HierarchyNode[] = groupTypeFolders.map(folder => ({
            kind: 'typeFolder' as NodeKind,
            name: folder.nameKey,
            icon: folder.icon,
            level: node.level + 1,
            expandable: true,
            expanded: false,
            loading: false,
            children: null,
            parent: node,
            groupType: folder.type,
            customerId: node.customer.id.id
          }));
          node.children = [...subCustomerNodes, ...typeFolderNodes];
          done();
        });
        break;
      case 'typeFolder': {
        // A CUSTOMER-type group's members are themselves customers, so those leaves recurse
        // (a group's own customer members can have further sub-customers/type-folders under
        // them, matching PE's arbitrarily-deep hierarchy) — every other group type is a leaf.
        const isCustomerGroup = node.groupType === EntityType.CUSTOMER;
        this.entityGroupService.getEntityGroupsByType(node.groupType, this.pageLink(), node.customerId).subscribe((page) => {
          node.children = page.data.map(group => ({
            kind: 'entityGroup' as NodeKind,
            name: group.name,
            icon: groupTypeIcons.get(node.groupType),
            level: node.level + 1,
            expandable: isCustomerGroup,
            expanded: false,
            loading: false,
            children: isCustomerGroup ? null : [],
            parent: node,
            group,
            groupType: node.groupType,
            customerId: node.customerId
          }));
          done();
        });
        break;
      }
      case 'entityGroup':
        // Only reached for CUSTOMER-type groups (see above) — same member shape as customerGroup.
        this.entityGroupService.getEntityGroupEntities<Customer>(node.group.id.id, this.pageLink()).subscribe((page) => {
          node.children = page.data.map(customer => ({
            kind: 'customer' as NodeKind,
            name: customer.title,
            icon: 'supervisor_account',
            level: node.level + 1,
            expandable: true,
            expanded: false,
            loading: false,
            children: null,
            parent: node,
            customer
          }));
          done();
        });
        break;
      default:
        done();
    }
  }

  select(node: HierarchyNode) {
    this.selectedNode = node;
    this.tableConfig = null;
    this.folderNode = null;
    switch (node.kind) {
      case 'customerGroup':
        this.customerGroupResolver.resolveByGroup(node.group).subscribe((config) => {
          config.pageMode = false;
          config.onManageRelatedGroup = (groupType, customerId) => this.navigateToGroupFolder(node, customerId, groupType);
          this.tableConfig = config;
        });
        break;
      case 'customer':
        // selecting a customer just reveals its group folders
        if (!node.expanded) {
          this.toggle(node);
        }
        break;
      case 'typeFolder':
        this.folderNode = node;
        break;
      case 'entityGroup':
        this.resolverFor(node.groupType).resolveByGroup(node.group).subscribe((config) => {
          config.pageMode = false;
          if (node.groupType === EntityType.CUSTOMER) {
            config.onManageRelatedGroup = (groupType, customerId) => this.navigateToGroupFolder(node, customerId, groupType);
          }
          // "{name}: {type}" + edit pencil opening the group's own details drawer, matching PE.
          config.headerComponent = GroupEntityTableHeaderComponent;
          config.componentsData = config.componentsData || {};
          config.componentsData.groupTableTitle = config.tableTitle;
          config.componentsData.openGroupDetails = () => {
            this.editingGroupNode = node;
            this.editingGroupId = node.group.id;
            this.groupDetailsOpen = true;
          };
          config.tableTitle = '';
          this.tableConfig = config;
        });
        break;
    }
  }

  private resolverFor(type: EntityType): {resolveByGroup: (group: EntityGroup) => Observable<EntityTableConfig<any>>} {
    switch (type) {
      case EntityType.CUSTOMER: return this.customerGroupResolver;
      case EntityType.DEVICE: return this.deviceGroupResolver;
      case EntityType.ASSET: return this.assetGroupResolver;
      case EntityType.ENTITY_VIEW: return this.entityViewGroupResolver;
      case EntityType.DASHBOARD: return this.dashboardGroupResolver;
      case EntityType.USER: return this.userGroupResolver;
    }
  }

  /** "Open" from an embedded groups table — select the corresponding tree leaf. */
  openGroupFromFolder(folder: HierarchyNode, group: EntityGroup) {
    folder.expanded = true;
    // always refresh so newly created groups appear
    this.loadChildren(folder, () => {
      const leaf = folder.children?.find(child => child.group?.id?.id === group.id.id);
      if (leaf) {
        this.select(leaf);
      }
    });
  }

  private expandNode(node: HierarchyNode, onExpanded: () => void) {
    node.expanded = true;
    if (node.children === null) {
      this.loadChildren(node, onExpanded);
    } else {
      this.updateVisibleNodes();
      onExpanded();
    }
  }

  /** A customer table's "manage devices/assets/..." button, when embedded in this hierarchy
   * view, lands here instead of navigating away — expands parent -> customer -> type folder
   * -> "All" group in place and selects it, so the tree stays in sync with what's on screen. */
  private navigateToGroupFolder(parent: HierarchyNode, customerId: string, groupType: EntityType) {
    this.expandNode(parent, () => {
      const customerNode = parent.children?.find(c => c.kind === 'customer' && c.customer.id.id === customerId);
      if (!customerNode) {
        return;
      }
      this.expandNode(customerNode, () => {
        const folder = customerNode.children?.find(c => c.kind === 'typeFolder' && c.groupType === groupType);
        if (!folder) {
          return;
        }
        this.expandNode(folder, () => {
          const allGroup = folder.children?.find(g => isGroupAll(g.group)) || folder.children?.[0];
          if (allGroup) {
            this.select(allGroup);
          }
        });
      });
    });
  }

  onGroupDetailsClosed() {
    this.groupDetailsOpen = false;
  }

  /** Reflect a rename from the 详情 tab back into the tree node label and table header. */
  onGroupDetailsUpdated(group: EntityGroup) {
    if (!this.editingGroupNode) {
      return;
    }
    this.editingGroupNode.name = group.name;
    this.editingGroupNode.group = group;
    if (this.tableConfig?.componentsData && this.editingGroupNode === this.selectedNode) {
      const typeLabel = (this.tableConfig.componentsData.groupTableTitle as string)?.split(': ').slice(1).join(': ');
      this.tableConfig.componentsData.groupTableTitle = typeLabel ? `${group.name}: ${typeLabel}` : group.name;
    }
  }

  private onGroupAction(action: EntityAction<EntityGroup>): boolean {
    switch (action.action) {
      case 'delete':
        this.deleteEditingGroup(action.entity);
        return true;
      case 'makePublic':
        this.entityGroupService.makeEntityGroupPublic(action.entity.id.id, true).subscribe(() => this.select(this.editingGroupNode));
        return true;
      case 'makePrivate':
        this.entityGroupService.makeEntityGroupPublic(action.entity.id.id, false).subscribe(() => this.select(this.editingGroupNode));
        return true;
      default:
        return false;
    }
  }

  private deleteEditingGroup(group: EntityGroup) {
    this.dialogService.confirm(
      this.translate.instant('entity-group.delete-group-title', {entityGroupName: group.name}),
      this.translate.instant('entity-group.delete-group-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.entityGroupService.deleteEntityGroup(group.id.id).subscribe(() => {
          this.groupDetailsOpen = false;
          const folder = this.editingGroupNode?.parent;
          if (folder) {
            folder.children = folder.children.filter(c => c.group?.id?.id !== group.id.id);
            this.updateVisibleNodes();
            this.select(folder);
          }
        });
      }
    });
  }

  selectParent() {
    if (this.selectedNode?.parent) {
      this.select(this.selectedNode.parent);
    }
  }

  toggleTree() {
    this.treeCollapsed = !this.treeCollapsed;
  }

}
