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
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DomainService } from '@core/http/domain.service';
import { Domain } from '@shared/models/oauth2.models';

@Component({
  selector: 'tb-create-domain-dialog',
  templateUrl: './create-domain-dialog.component.html',
  standalone: false
})
export class CreateDomainDialogComponent extends DialogComponent<CreateDomainDialogComponent, Domain> implements OnInit {

  domainFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private domainService: DomainService,
              public dialogRef: MatDialogRef<CreateDomainDialogComponent, Domain>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.domainFormGroup = this.fb.group({
      name: [null, [Validators.required, Validators.maxLength(255),
        Validators.pattern('^(?:\\w+(?::\\w+)?@)?[^\\s/]+(?::\\d+)?$')]]
    });
  }

  close(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.domainFormGroup.markAsPristine();
    const domain: Domain = {
      name: this.domainFormGroup.get('name').value,
      oauth2Enabled: false,
      propagateToEdge: false
    } as Domain;
    this.domainService.saveDomain(domain).subscribe((saved) => {
      this.dialogRef.close(saved);
    });
  }

}
