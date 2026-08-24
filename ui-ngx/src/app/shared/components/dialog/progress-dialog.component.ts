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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable, Subscription } from 'rxjs';

export interface ProgressDialogData {
  message: string;
  obs: Observable<any>;
}

@Component({
    selector: 'tb-progress-dialog',
    templateUrl: './progress-dialog.component.html',
    styleUrls: ['./progress-dialog.component.scss'],
    standalone: false
})
export class ProgressDialogComponent extends DialogComponent<ProgressDialogComponent, void> implements OnInit, OnDestroy {

  private obsSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<ProgressDialogComponent, void>,
              @Inject(MAT_DIALOG_DATA) public data: ProgressDialogData) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.obsSubscription = this.data.obs.subscribe({
      complete: () => this.dialogRef.close(),
      error: () => this.dialogRef.close()
    });
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.obsSubscription) {
      this.obsSubscription.unsubscribe();
    }
  }

}
