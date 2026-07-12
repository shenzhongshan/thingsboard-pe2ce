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
import { EntityGroup } from '@shared/models/entity-group.models';
import { Customer } from '@shared/models/customer.model';
import { EntityGroupService } from '@core/http/entity-group.service';
import { CustomerService } from '@core/http/customer.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
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

  EntityType = EntityType;

  constructor(protected store: Store<AppState>,
              private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              private customerGroupResolver: CustomerGroupEntitiesTableConfigResolver,
              private deviceGroupResolver: DeviceGroupEntitiesTableConfigResolver,
              private assetGroupResolver: AssetGroupEntitiesTableConfigResolver,
              private entityViewGroupResolver: EntityViewGroupEntitiesTableConfigResolver,
              private dashboardGroupResolver: DashboardGroupEntitiesTableConfigResolver,
              private userGroupResolver: UserGroupEntitiesTableConfigResolver) {
    super(store);
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
      case 'typeFolder':
        this.entityGroupService.getEntityGroupsByType(node.groupType, this.pageLink(), node.customerId).subscribe((page) => {
          node.children = page.data.map(group => ({
            kind: 'entityGroup' as NodeKind,
            name: group.name,
            icon: groupTypeIcons.get(node.groupType),
            level: node.level + 1,
            expandable: false,
            expanded: false,
            loading: false,
            children: [],
            parent: node,
            group,
            groupType: node.groupType,
            customerId: node.customerId
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

  selectParent() {
    if (this.selectedNode?.parent) {
      this.select(this.selectedNode.parent);
    }
  }

  toggleTree() {
    this.treeCollapsed = !this.treeCollapsed;
  }

}
