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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupService } from '@core/http/entity-group.service';

export interface AssignEntityGroupToEdgeDialogData {
  edgeId: string;
  groupType: EntityType;
  /** 已经分配给这台 edge 的分组 id，从候选里排除掉，避免重复分配。 */
  excludeGroupIds: string[];
}

/**
 * 把一个已有实体分组分配给某台 edge。
 *
 * <p>不复用 AddEntitiesToEdgeDialogComponent：那个对话框用 tb-entity-list 按 entityType 取候选，
 * 对 ENTITY_GROUP 会列出全部类型的分组（设备组里混进资产组），没法按 groupType 过滤。
 * 这里用 tb-entity-group-autocomplete，它本身就带 groupType / excludeGroupIds。
 */
@Component({
  selector: 'tb-assign-entity-group-to-edge-dialog',
  templateUrl: './assign-entity-group-to-edge-dialog.component.html',
  standalone: false
})
export class AssignEntityGroupToEdgeDialogComponent
  extends DialogComponent<AssignEntityGroupToEdgeDialogComponent, boolean> implements OnInit {

  assignFormGroup: UntypedFormGroup;
  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AssignEntityGroupToEdgeDialogData,
              private entityGroupService: EntityGroupService,
              public dialogRef: MatDialogRef<AssignEntityGroupToEdgeDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.assignFormGroup = this.fb.group({
      entityGroupId: [null, [Validators.required]]
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  assign(): void {
    this.submitted = true;
    const entityGroupId: string = this.assignFormGroup.get('entityGroupId').value;
    this.entityGroupService.assignEntityGroupToEdge(this.data.edgeId, entityGroupId, this.data.groupType)
      .subscribe(() => this.dialogRef.close(true));
  }
}
