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

import { Component, OnInit, Renderer2, ViewChild, ViewContainerRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';
import { of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { MatDrawer } from '@angular/material/sidenav';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { IntegrationService } from '@core/http/integration.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { Sort } from '@angular/material/sort';
import {
  Integration,
  IntegrationInfo,
  integrationState,
  IntegrationState,
  integrationStateStyle,
  integrationTypeInfoMap
} from '@shared/models/integration.models';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import {
  IntegrationDialogComponent,
  IntegrationDialogData
} from '@home/pages/integration/integration/integration-dialog.component';
import { IntegrationDetailsPanelTarget } from '@home/pages/integration/integration-details-panel.component';

@Component({
  selector: 'tb-integrations',
  templateUrl: './integrations.component.html',
  styleUrls: ['./integrations.component.scss'],
  standalone: false
})
export class IntegrationsComponent extends PageComponent implements OnInit {

  integrations: IntegrationInfo[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  readonly displayedColumns = ['createdTime', 'name', 'type', 'dailyActivity', 'status', 'remote', 'actions'];
  readonly IntegrationState = IntegrationState;
  readonly EntityType = EntityType;

  /** 由路由 data.isEdgeTemplate 决定这个页面看的是 edge 模板还是普通实体 —— 同一套组件
   *  服务 /integrations-center 和 /edgeManagement 两条路由，与 PE 一致。 */
  isEdgeTemplate = false;

  /**
   * Edge 模式：从 edge 详情页进来（instances/:edgeId/integrations）时，只列出**分配给
   * 这台 edge** 的集成，动作换成分配/取消分配。与分组页同一套思路。
   */
  edgeId: string;

  sortProperty = 'createdTime';
  sortDirection: 'asc' | 'desc' = 'desc';

  @ViewChild('detailsDrawer') detailsDrawer: MatDrawer;
  detailsTarget: IntegrationDetailsPanelTarget = null;
  detailsFocusTab: 'overview' | 'events' = 'overview';

  constructor(protected store: Store<AppState>,
              private integrationService: IntegrationService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private viewContainerRef: ViewContainerRef,
              private renderer: Renderer2,
              private dialog: MatDialog,
              private route: ActivatedRoute) {
    super(store);
  }

  ngOnInit() {
    this.isEdgeTemplate = !!this.route.snapshot.data.isEdgeTemplate;
    // edgeId 在祖先路由段 ':edgeId/integrations' 上；Angular 默认不向子路由继承 params。
    this.edgeId = this.findRouteParam('edgeId');
    this.reload();
  }

  private findRouteParam(name: string): string {
    let route = this.route.snapshot;
    while (route) {
      if (route.params?.[name]) {
        return route.params[name];
      }
      route = route.parent;
    }
    return null;
  }

  get edgeMode(): boolean {
    return !!this.edgeId;
  }

  reload() {
    const pageLink = new PageLink(this.pageSize, this.page, this.textSearch,
      { property: this.sortProperty, direction: this.sortDirection === 'asc' ? Direction.ASC : Direction.DESC });
    // edge 模式下用的是 Integration（非 IntegrationInfo），字段是后者的超集里缺 dailyActivity
    // 等统计项，表格对缺失值显示空即可 —— 对齐 PE 的 /edge/{id}/integrations 端点形状。
    const source$ = this.edgeMode
      ? this.integrationService.getEdgeIntegrations(this.edgeId, pageLink)
      : this.integrationService.getIntegrationInfos(pageLink, this.isEdgeTemplate);
    source$.subscribe((pageData) => {
      this.integrations = pageData.data as any[];
      this.totalElements = pageData.totalElements;
    });
  }

  /** 把一个 edge 模板集成分配给这台 edge。 */
  assignIntegrationToEdge($event: Event) {
    $event?.stopPropagation();
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData, boolean>(
      AddEntitiesToEdgeDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { edgeId: this.edgeId, entityType: EntityType.INTEGRATION }
      }).afterClosed().subscribe((assigned) => {
      if (assigned) {
        this.reload();
      }
    });
  }

  unassignIntegrationFromEdge($event: Event, integration: {id: {id: string}; name: string}) {
    $event?.stopPropagation();
    // PE 的集成取消分配只有复数标题（ICU plural），单行就传 count:1 复用它；
    // 文案在 integration.* 命名空间下，不在 edge.* ——照抄 PE，不另造键。
    this.dialogService.confirm(
      this.translate.instant('integration.unassign-integrations-from-edge-title', {count: 1}),
      this.translate.instant('integration.unassign-integration-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.integrationService.unassignIntegrationFromEdge(this.edgeId, integration.id.id)
          .subscribe(() => this.reload());
      }
    });
  }

  onSortChange(sort: Sort) {
    this.sortProperty = sort.active;
    this.sortDirection = (sort.direction || 'desc') as 'asc' | 'desc';
    this.page = 0;
    this.reload();
  }

  typeName(integration: IntegrationInfo): string {
    const info = integrationTypeInfoMap.get(integration.type);
    return info ? this.translate.instant(info.name) : integration.type;
  }

  stateOf(integration: IntegrationInfo): IntegrationState {
    return integrationState(integration);
  }

  stateStyle(integration: IntegrationInfo): {background: string; color: string} {
    return integrationStateStyle.get(this.stateOf(integration));
  }

  stateTooltip(integration: IntegrationInfo): string {
    return integration.status?.error || '';
  }

  isDebugActive(integration: IntegrationInfo): boolean {
    const settings = integration.debugSettings;
    return this.entityDebugSettingsService.isDebugActive(settings?.allEnabledUntil) || !!settings?.failuresEnabled;
  }

  debugTooltip(integration: IntegrationInfo): string {
    return this.entityDebugSettingsService.getDebugConfigLabel(integration.debugSettings);
  }

  /**
   * Opens the same debug strategy panel the entity detail pages use, anchored on the clicked icon,
   * and persists whatever it returns straight onto the integration.
   */
  openDebugConfig($event: MouseEvent, integration: IntegrationInfo) {
    $event?.stopPropagation();
    this.entityDebugSettingsService.viewContainerRef = this.viewContainerRef;
    this.entityDebugSettingsService.renderer = this.renderer;
    this.entityDebugSettingsService.openDebugStrategyPanel({
      debugSettings: integration.debugSettings || {},
      debugConfig: {
        entityType: EntityType.INTEGRATION,
        additionalActionConfig: {
          title: this.translate.instant('action.see-debug-events'),
          action: () => this.openDebugEvents(integration)
        }
      },
      onSettingsAppliedFn: (settings: EntityDebugSettings) => this.saveDebugSettings(integration, settings)
    }, $event.target as Element);
  }

  openDebugEvents(integration: IntegrationInfo) {
    this.openDetails(integration.id.id, 'events');
  }

  private saveDebugSettings(integration: IntegrationInfo, debugSettings: EntityDebugSettings) {
    // Re-read before saving: the row was loaded from /integrationInfos, which does not carry the
    // configuration, so posting the row itself would blank it out.
    this.integrationService.getIntegration(integration.id.id).pipe(
      switchMap((full: Integration) => this.integrationService.saveIntegration({...full, debugSettings})),
      catchError(() => of(null))
    ).subscribe(() => this.reload());
  }

  pageChanged(event: {pageIndex: number; pageSize: number}) {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload();
  }

  enterSearchMode() { this.searchMode = true; }

  exitSearchMode() {
    this.searchMode = false;
    if (this.textSearch) {
      this.textSearch = null;
      this.reload();
    }
  }

  searchChanged() {
    this.page = 0;
    this.reload();
  }

  addIntegration() {
    this.openDialog(null);
  }

  /**
   * Row click opens the read-oriented side panel (PE's own layout: the list stays visible, the
   * panel slides in from the right) rather than the edit wizard directly - this build's wizard is
   * a purpose-built stepper that doesn't fit inline alongside the panel's tabs, so editing is a
   * separate step reached via the panel's edit FAB or the row's own pencil icon.
   */
  openIntegrationDetails($event: Event, integration: IntegrationInfo) {
    $event?.stopPropagation();
    this.openDetails(integration.id.id, 'overview');
  }

  /** Matches PE: clicking the status cell specifically opens the panel straight to the Events tab. */
  openIntegrationStatusDetails($event: Event, integration: IntegrationInfo) {
    $event?.stopPropagation();
    this.openDetails(integration.id.id, 'events');
  }

  private openDetails(integrationId: string, focusTab: 'overview' | 'events') {
    this.integrationService.getIntegration(integrationId).subscribe((full) => {
      this.detailsTarget = { kind: 'integration', entity: full };
      this.detailsFocusTab = focusTab;
      this.detailsDrawer.open();
    });
  }

  closeDetails() {
    this.detailsDrawer.close();
  }

  onDetailsEdit() {
    const id = this.detailsTarget?.entity?.id?.id;
    this.detailsDrawer.close();
    if (id) {
      this.openDialog(id);
    }
  }

  onDetailsDelete() {
    const integration = this.detailsTarget?.entity as IntegrationInfo;
    this.detailsDrawer.close();
    if (integration) {
      this.deleteIntegration(null, integration);
    }
  }

  private openDialog(integrationId: string) {
    this.dialog.open<IntegrationDialogComponent, IntegrationDialogData, boolean>(
      IntegrationDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { integrationId, isEdgeTemplate: this.isEdgeTemplate }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  deleteIntegration($event: Event, integration: IntegrationInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('integration.delete-integration-title', {integrationName: integration.name}),
      this.translate.instant('integration.delete-integration-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.integrationService.deleteIntegration(integration.id.id).subscribe(() => this.reload());
      }
    });
  }

}
