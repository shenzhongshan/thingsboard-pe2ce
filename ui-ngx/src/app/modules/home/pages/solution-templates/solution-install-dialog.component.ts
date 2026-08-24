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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { SolutionInstallResponse, TenantSolutionTemplateInstructions } from '@shared/models/solution-template.models';

export interface SolutionInstallDialogData {
  solutionInstallResponse: TenantSolutionTemplateInstructions | SolutionInstallResponse;
  instructions: boolean;
  showMainDashboardButton: boolean;
}

@Component({
    selector: 'tb-solution-install-dialog',
    templateUrl: './solution-install-dialog.component.html',
    styleUrls: ['./solution-install-dialog.component.scss'],
    standalone: false
})
export class SolutionInstallDialogComponent extends DialogComponent<SolutionInstallDialogComponent, void> {

  solutionInstallResponse: TenantSolutionTemplateInstructions | SolutionInstallResponse;
  instructions: boolean;
  showMainDashboardButton: boolean;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<SolutionInstallDialogComponent, void>,
              @Inject(MAT_DIALOG_DATA) public data: SolutionInstallDialogData) {
    super(store, router, dialogRef);
    this.solutionInstallResponse = data.solutionInstallResponse;
    this.instructions = data.instructions;
    this.showMainDashboardButton = data.showMainDashboardButton;
  }

  get titleKey(): string {
    if (this.instructions) {
      return 'solution-template.instructions';
    }
    return (this.solutionInstallResponse as SolutionInstallResponse).success ?
      'solution-template.install-title' : 'solution-template.install-failed-title';
  }

  get showGotoDashboardButton(): boolean {
    return (this.instructions || (this.solutionInstallResponse as SolutionInstallResponse).success) && this.showMainDashboardButton;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  gotoMainDashboard(): void {
    const dashboardId = this.solutionInstallResponse.dashboardId;
    if (this.solutionInstallResponse.dashboardGroupId && dashboardId) {
      this.dialogRef.close();
      this.router.navigateByUrl(`dashboards/${dashboardId.id}`);
    }
  }

}
