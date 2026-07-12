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
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { EditorOptions } from 'tinymce';
import { Authority } from '@shared/models/authority.enum';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

export const mailTemplateKeys = [
  'test', 'activation', 'accountActivated', 'resetPassword', 'passwordWasReset', 'accountLockout',
  'twoFaVerification'
];

@Component({
  selector: 'tb-mail-templates',
  templateUrl: './mail-templates.component.html',
  styleUrls: ['./white-labeling.component.scss', './settings-card.scss'],
  standalone: false
})
export class MailTemplatesComponent extends PageComponent implements OnInit, HasConfirmForm {

  mailTemplatesForm: UntypedFormGroup;

  templateKeys = mailTemplateKeys;
  selectedTemplate = 'test';
  isTenantAdmin = getCurrentAuthState(this.store).authUser.authority === Authority.TENANT_ADMIN;

  tinyMceOptions: Partial<EditorOptions> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['link', 'table', 'image', 'lists', 'code', 'fullscreen'],
    menubar: 'edit insert tools view format table',
    toolbar: 'undo redo | fontfamily fontsize blocks | bold italic strikethrough | forecolor backcolor ' +
      '| link table image | alignleft aligncenter alignright alignjustify ' +
      '| numlist bullist | outdent indent | removeformat | code | fullscreen',
    toolbar_mode: 'sliding',
    height: 400,
    autofocus: false,
    branding: false,
    promotion: false,
    relative_urls: false,
    urlconverter_callback: (url) => url
  };

  private mailTemplates: any = {};

  constructor(protected store: Store<AppState>,
              private whiteLabelingService: WhiteLabelingService,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.mailTemplatesForm = this.fb.group({
      useSystemMailSettings: [false],
      subject: [''],
      body: ['']
    });
    this.whiteLabelingService.getMailTemplates(true).subscribe(
      (templates) => {
        this.mailTemplates = templates || {};
        this.mailTemplatesForm.get('useSystemMailSettings').setValue(
          this.mailTemplates.useSystemMailSettings === true, { emitEvent: false });
        this.loadTemplate(this.selectedTemplate);
        this.mailTemplatesForm.markAsPristine();
      }
    );
    this.mailTemplatesForm.get('useSystemMailSettings').valueChanges.subscribe(
      () => this.updateDisabledState()
    );
  }

  get useSystemMailSettings(): boolean {
    return this.isTenantAdmin && this.mailTemplatesForm.get('useSystemMailSettings').value === true;
  }

  private updateDisabledState() {
    if (this.useSystemMailSettings) {
      this.mailTemplatesForm.get('subject').disable({ emitEvent: false });
      this.mailTemplatesForm.get('body').disable({ emitEvent: false });
    } else {
      this.mailTemplatesForm.get('subject').enable({ emitEvent: false });
      this.mailTemplatesForm.get('body').enable({ emitEvent: false });
    }
  }

  private loadTemplate(key: string) {
    const template = this.mailTemplates[key] || {};
    this.mailTemplatesForm.get('subject').setValue(template.subject || '', { emitEvent: false });
    this.mailTemplatesForm.get('body').setValue(template.body || '', { emitEvent: false });
    this.updateDisabledState();
  }

  private stashTemplate(key: string) {
    if (!this.mailTemplates[key]) {
      this.mailTemplates[key] = {};
    }
    this.mailTemplates[key].subject = this.mailTemplatesForm.get('subject').value;
    this.mailTemplates[key].body = this.mailTemplatesForm.get('body').value;
  }

  templateChanged(key: string) {
    this.stashTemplate(this.selectedTemplate);
    this.selectedTemplate = key;
    this.loadTemplate(key);
  }

  save(): void {
    this.stashTemplate(this.selectedTemplate);
    if (this.isTenantAdmin) {
      this.mailTemplates.useSystemMailSettings = this.mailTemplatesForm.get('useSystemMailSettings').value === true;
    }
    this.whiteLabelingService.saveMailTemplates(this.mailTemplates).subscribe(
      (templates) => {
        this.mailTemplates = templates || this.mailTemplates;
        this.loadTemplate(this.selectedTemplate);
        this.mailTemplatesForm.markAsPristine();
      }
    );
  }

  confirmForm(): UntypedFormGroup {
    return this.mailTemplatesForm;
  }

}
