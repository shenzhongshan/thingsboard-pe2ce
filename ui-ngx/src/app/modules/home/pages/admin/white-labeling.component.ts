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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { MatDialog } from '@angular/material/dialog';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { WhiteLabelingApplyService } from '@core/services/white-labeling-apply.service';
import { WhiteLabelingParams } from '@shared/models/white-labeling.models';
import {
  CustomCssDialogComponent,
  CustomCssDialogData
} from '@home/pages/admin/custom-css-dialog.component';

@Component({
  selector: 'tb-white-labeling',
  templateUrl: './white-labeling.component.html',
  styleUrls: ['./white-labeling.component.scss', './settings-card.scss'],
  standalone: false
})
export class WhiteLabelingComponent extends PageComponent implements OnInit, HasConfirmForm {

  whiteLabelingForm: UntypedFormGroup;

  private customCss: string = null;

  constructor(protected store: Store<AppState>,
              private whiteLabelingService: WhiteLabelingService,
              private whiteLabelingApplyService: WhiteLabelingApplyService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.whiteLabelingForm = this.fb.group({
      appTitle: [null],
      favicon: [null],
      logo: [null],
      logoImageHeight: [null],
      enableHelpLinks: [true],
      helpLinkBaseUrl: [null],
      uiHelpBaseUrl: [null],
      primaryPalette: [null],
      accentPalette: [null],
      overrideTrendzName: [false],
      showNameVersion: [false],
      platformName: [null],
      platformVersion: [null]
    });
    this.load();
  }

  private load() {
    this.whiteLabelingService.getCurrentWhiteLabelParams().subscribe(
      (params) => this.setWhiteLabelingParams(params)
    );
  }

  private setWhiteLabelingParams(params: WhiteLabelingParams) {
    params = params || {};
    this.customCss = params.customCss || null;
    this.whiteLabelingForm.reset({
      appTitle: params.appTitle,
      favicon: params.favicon?.url || null,
      logo: params.logoImageUrl || null,
      logoImageHeight: params.logoImageHeight,
      enableHelpLinks: params.enableHelpLinks !== false,
      helpLinkBaseUrl: params.helpLinkBaseUrl,
      uiHelpBaseUrl: params.uiHelpBaseUrl,
      primaryPalette: params.paletteSettings?.primaryPalette || null,
      accentPalette: params.paletteSettings?.accentPalette || null,
      overrideTrendzName: params.overrideTrendzName || false,
      showNameVersion: params.showNameVersion || false,
      platformName: params.platformName,
      platformVersion: params.platformVersion
    });
  }

  private buildParams(): WhiteLabelingParams {
    const v = this.whiteLabelingForm.value;
    return {
      appTitle: v.appTitle,
      favicon: v.favicon ? { url: v.favicon } : null,
      logoImageUrl: v.logo,
      logoImageHeight: v.logoImageHeight,
      enableHelpLinks: v.enableHelpLinks,
      helpLinkBaseUrl: v.helpLinkBaseUrl,
      uiHelpBaseUrl: v.uiHelpBaseUrl,
      paletteSettings: {
        primaryPalette: v.primaryPalette,
        accentPalette: v.accentPalette
      },
      overrideTrendzName: v.overrideTrendzName,
      showNameVersion: v.showNameVersion,
      platformName: v.platformName,
      platformVersion: v.platformVersion,
      customCss: this.customCss
    };
  }

  openCustomCss() {
    this.dialog.open<CustomCssDialogComponent, CustomCssDialogData, string>(CustomCssDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: { customCss: this.customCss, readonly: false }
    }).afterClosed().subscribe((result) => {
      if (result !== null && result !== undefined) {
        this.customCss = result;
        this.whiteLabelingForm.markAsDirty();
      }
    });
  }

  save(): void {
    this.whiteLabelingService.saveWhiteLabelParams(this.buildParams()).subscribe(
      (params) => {
        this.setWhiteLabelingParams(params);
        this.whiteLabelingForm.markAsPristine();
        this.whiteLabelingApplyService.loadAndApply().subscribe();
      }
    );
  }

  preview(): void {
    this.whiteLabelingService.previewWhiteLabelParams(this.buildParams()).subscribe(
      (params) => this.whiteLabelingApplyService.applyWhiteLabeling(params)
    );
  }

  reset(): void {
    this.whiteLabelingService.deleteWhiteLabelParams().subscribe(
      () => {
        this.load();
        this.whiteLabelingApplyService.loadAndApply().subscribe();
      }
    );
  }

  confirmForm(): UntypedFormGroup {
    return this.whiteLabelingForm;
  }

}
