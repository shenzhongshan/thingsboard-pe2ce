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
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { IntegrationService } from '@core/http/integration.service';
import { ConverterService } from '@core/http/converter.service';
import { BroadcastService } from '@core/services/broadcast.service';
import { IntegrationDetailsPanelKind, IntegrationDetailsPanelTarget } from '@home/pages/integration/integration-details-panel.component';
import { IntegrationDialogComponent, IntegrationDialogData } from '@home/pages/integration/integration/integration-dialog.component';
import { ConverterDialogComponent, ConverterDialogData } from '@home/pages/integration/converter/converter-dialog.component';

/**
 * PE's "打开详情页" button opens the same details panel as a full page (breadcrumb + list still
 * reachable via back button) rather than the side drawer. Reuses the drawer's own
 * tb-integration-details-panel component unmodified — only the host chrome differs.
 */
@Component({
  selector: 'tb-integration-entity-details-page',
  template: `
    <tb-integration-details-panel
      class="tb-integration-entity-details-page"
      [target]="target"
      focusTab="overview"
      (closed)="close()"
      (editRequested)="onEdit()"
      (deleteRequested)="onDelete()"
      (saved)="load()">
    </tb-integration-details-panel>
  `,
  styles: [`
    :host { display: block; height: 100%; }
    /* PE's own full-page details view keeps the same ~800px form width as the drawer instead of
       stretching fields edge-to-edge across a wide viewport - matched here, confirmed against PE's
       live full-page route (measured form width ~706-773px at a 1440px window). !important because
       without it this rule loses the cascade to something else in the page and the panel silently
       stays full-width (confirmed empirically, not worth chasing the exact competing rule). */
    .tb-integration-entity-details-page { display: block; height: 100%; max-width: 800px !important; }
  `],
  standalone: false
})
export class IntegrationEntityDetailsPageComponent implements OnInit {

  target: IntegrationDetailsPanelTarget = null;
  entityName = '';

  constructor(private route: ActivatedRoute,
              private router: Router,
              private integrationService: IntegrationService,
              private converterService: ConverterService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private broadcast: BroadcastService,
              private dialog: MatDialog) {
  }

  get kind(): IntegrationDetailsPanelKind {
    return this.route.snapshot.data.kind;
  }

  ngOnInit() {
    this.load();
  }

  load() {
    if (this.kind === 'integration') {
      const id = this.route.snapshot.paramMap.get('integrationId');
      this.integrationService.getIntegration(id).subscribe((full) => {
        this.target = { kind: 'integration', entity: full };
        this.entityName = full.name;
        this.broadcast.broadcast('updateBreadcrumb');
      });
    } else {
      const id = this.route.snapshot.paramMap.get('converterId');
      this.converterService.getConverter(id).subscribe((full) => {
        this.target = { kind: 'converter', entity: full };
        this.entityName = full.name;
        this.broadcast.broadcast('updateBreadcrumb');
      });
    }
  }

  close() {
    this.router.navigate([this.kind === 'integration' ? '/integrations-center/integrations' : '/integrations-center/converters']);
  }

  onEdit() {
    const id = this.target?.entity?.id?.id;
    if (!id) {
      return;
    }
    if (this.kind === 'integration') {
      this.dialog.open<IntegrationDialogComponent, IntegrationDialogData, boolean>(
        IntegrationDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: { integrationId: id }
        }).afterClosed().subscribe((changed) => { if (changed) { this.load(); } });
    } else {
      this.dialog.open<ConverterDialogComponent, ConverterDialogData, boolean>(
        ConverterDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: { converterId: id }
        }).afterClosed().subscribe((changed) => { if (changed) { this.load(); } });
    }
  }

  onDelete() {
    const entity = this.target?.entity;
    if (!entity) {
      return;
    }
    const titleKey = this.kind === 'integration' ? 'integration.delete-integration-title' : 'converter.delete-converter-title';
    const textKey = this.kind === 'integration' ? 'integration.delete-integration-text' : 'converter.delete-converter-text';
    const nameParam = this.kind === 'integration' ? { integrationName: entity.name } : { converterName: entity.name };
    this.dialogService.confirm(
      this.translate.instant(titleKey, nameParam),
      this.translate.instant(textKey),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        const obs = this.kind === 'integration'
          ? this.integrationService.deleteIntegration(entity.id.id)
          : this.converterService.deleteConverter(entity.id.id);
        obs.subscribe(() => this.close());
      }
    });
  }

}
