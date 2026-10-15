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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { DialogService } from '@core/services/dialog.service';
import { SolutionService } from '@core/http/solution.service';
import {
  SolutionTemplateLevel,
  SolutionTemplateLevelTranslationMap,
  TenantSolutionTemplateInfo
} from '@shared/models/solution-template.models';
import {
  SolutionInstallDialogComponent,
  SolutionInstallDialogData
} from '@home/pages/solution-templates/solution-install-dialog.component';

@Component({
    selector: 'tb-solution-template-card',
    templateUrl: './solution-template-card.component.html',
    styleUrls: ['./solution-template-card.component.scss'],
    standalone: false
})
export class SolutionTemplateCardComponent extends PageComponent {

  @Input() solutionTemplate: TenantSolutionTemplateInfo;

  @Output() solutionTemplateChanged = new EventEmitter<void>();

  SolutionTemplateLevel = SolutionTemplateLevel;
  SolutionTemplateLevelTranslationMap = SolutionTemplateLevelTranslationMap;

  constructor(protected store: Store<AppState>,
              private solutionService: SolutionService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private translate: TranslateService,
              private router: Router) {
    super(store);
  }

  openSolutionTemplateDetails(): void {
    this.router.navigateByUrl(`solutionTemplates/${this.solutionTemplate.id}`);
  }

  installSolutionTemplate(): void {
    const waitSubject = new Subject<void>();
    this.dialogService.progress(waitSubject.asObservable(), this.translate.instant('solution-template.installing'));
    this.solutionService.installSolutionTemplate(this.solutionTemplate.id).subscribe(response => {
      if (response.success) {
        setTimeout(() => {
          waitSubject.next();
          waitSubject.complete();
          this.router.navigateByUrl(`dashboards/${response.dashboardId.id}`);
        }, this.solutionTemplate.installTimeoutMs);
      } else {
        waitSubject.next();
        waitSubject.complete();
        this.dialog.open<SolutionInstallDialogComponent, SolutionInstallDialogData>(SolutionInstallDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            solutionInstallResponse: response,
            instructions: false,
            showMainDashboardButton: false
          }
        }).afterClosed().subscribe(() => {
          this.solutionTemplateChanged.emit();
        });
      }
    }, () => {
      waitSubject.next();
      waitSubject.complete();
    });
  }

  openInstructions(): void {
    this.solutionService.getSolutionTemplateInstructions(this.solutionTemplate.id).subscribe(instructions => {
      this.dialog.open<SolutionInstallDialogComponent, SolutionInstallDialogData>(SolutionInstallDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          solutionInstallResponse: instructions,
          instructions: true,
          showMainDashboardButton: true
        }
      });
    });
  }

  deleteSolutionTemplate(): void {
    this.dialogService.confirm(
      this.translate.instant('solution-template.delete-solution-title', {solutionTitle: this.solutionTemplate.title}),
      this.translate.instant('solution-template.delete-solution-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(result => {
      if (result) {
        this.solutionService.uninstallSolutionTemplate(this.solutionTemplate.id).subscribe(() => {
          this.solutionTemplateChanged.emit();
        });
      }
    });
  }

}
