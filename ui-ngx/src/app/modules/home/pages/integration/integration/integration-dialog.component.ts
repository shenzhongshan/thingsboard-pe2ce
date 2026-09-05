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
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationService } from '@core/http/integration.service';
import { ConverterService } from '@core/http/converter.service';
import { PageLink } from '@shared/models/page/page-link';
import {
  Converter,
  ConverterId,
  ConverterScriptLanguage,
  ConverterType,
  DEFAULT_JS_DECODER,
  DEFAULT_JS_ENCODER,
  DEFAULT_TBEL_DECODER,
  DEFAULT_TBEL_ENCODER
} from '@shared/models/converter.models';
import {
  ConverterTestDialogComponent,
  ConverterTestDialogData
} from '@home/pages/integration/converter/converter-test-dialog.component';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import {
  HTTP_WEBHOOK_INTEGRATION_TYPES,
  Integration,
  integrationHttpEndpointUrl,
  IntegrationType,
  IntegrationTypeInfo,
  integrationTypeInfoMap,
  HTTP_HEADER_SECURITY_TYPES,
  HTTP_REPLACE_NO_CONTENT_TYPES,
  KinesisInitialPosition,
  LoriotCredentialsType,
  loriotCredentialsTypeLabels,
  MqttCredentialsType,
  MqttProtocolVersion,
  mqttProtocolVersionLabels,
  MQTT_FAMILY_INTEGRATION_TYPES,
  newIntegration,
  OpcUaIdentityType,
  OpcUaSecurityPolicy,
  PulsarCredentialsType,
  TcpByteOrder,
  TcpHandlerType,
  TcpMessageSeparator,
  THINGPARK_INTEGRATION_TYPES,
  TuyaEnv,
  TuyaRegion
} from '@shared/models/integration.models';
import { EntityType } from '@shared/models/entity-type.models';
import {
  IntegrationConnectionFormComponent
} from '@home/pages/integration/integration/integration-connection-form.component';

export interface IntegrationDialogData {
  integrationId?: string;
}

@Component({
  selector: 'tb-integration-dialog',
  templateUrl: './integration-dialog.component.html',
  styleUrls: ['./integration-dialog.component.scss'],
  standalone: false
})
export class IntegrationDialogComponent implements OnInit {

  form: UntypedFormGroup;
  isEdit = false;
  loaded = false;

  EntityType = EntityType;
  IntegrationType = IntegrationType;
  ConverterScriptLanguage = ConverterScriptLanguage;
  TcpHandlerType = TcpHandlerType;
  TcpMessageSeparator = TcpMessageSeparator;
  TcpByteOrder = TcpByteOrder;
  MqttCredentialsType = MqttCredentialsType;
  MqttProtocolVersion = MqttProtocolVersion;
  mqttProtocolVersionLabels = mqttProtocolVersionLabels;
  PulsarCredentialsType = PulsarCredentialsType;
  TuyaRegion = TuyaRegion;
  TuyaEnv = TuyaEnv;
  OpcUaSecurityPolicy = OpcUaSecurityPolicy;
  OpcUaIdentityType = OpcUaIdentityType;
  KinesisInitialPosition = KinesisInitialPosition;
  MQTT_FAMILY_INTEGRATION_TYPES = MQTT_FAMILY_INTEGRATION_TYPES;
  HTTP_WEBHOOK_INTEGRATION_TYPES = HTTP_WEBHOOK_INTEGRATION_TYPES;
  HTTP_HEADER_SECURITY_TYPES = HTTP_HEADER_SECURITY_TYPES;
  HTTP_REPLACE_NO_CONTENT_TYPES = HTTP_REPLACE_NO_CONTENT_TYPES;
  THINGPARK_INTEGRATION_TYPES = THINGPARK_INTEGRATION_TYPES;
  LoriotCredentialsType = LoriotCredentialsType;
  loriotCredentialsTypeLabels = loriotCredentialsTypeLabels;
  integrationTypeInfoMap = integrationTypeInfoMap;
  integrationTypes = Object.values(IntegrationType);
  tcpHandlerTypes = Object.values(TcpHandlerType);

  uplinkConverters: Converter[] = [];
  downlinkConverters: Converter[] = [];

  checkInProgress = false;
  checkResult: {success: boolean; error?: string} = null;

  private integration: Integration;

  constructor(@Inject(MAT_DIALOG_DATA) public data: IntegrationDialogData,
              private dialogRef: MatDialogRef<IntegrationDialogComponent, boolean>,
              protected store: Store<AppState>,
              private integrationService: IntegrationService,
              private converterService: ConverterService,
              private translate: TranslateService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.integrationId;
  }

  ngOnInit() {
    const converters$ = this.converterService.getConverters(new PageLink(1024, 0));
    if (this.isEdit) {
      forkJoin([converters$, this.integrationService.getIntegration(this.data.integrationId)]).subscribe(
        ([convPage, integration]) => {
          this.splitConverters(convPage.data);
          this.integration = integration;
          this.buildForm(integration);
          this.loaded = true;
        });
    } else {
      converters$.subscribe((convPage) => {
        this.splitConverters(convPage.data);
        this.integration = newIntegration();
        this.buildForm(this.integration);
        this.loaded = true;
      });
    }
  }

  private splitConverters(converters: Converter[]) {
    this.uplinkConverters = converters.filter((c) => c.type === ConverterType.UPLINK);
    this.downlinkConverters = converters.filter((c) => c.type === ConverterType.DOWNLINK);
  }

  // Per-step groups so mat-stepper's linear validation only gates on the current step's fields.
  get basicGroup(): UntypedFormGroup {
    return this.form.get('basic') as UntypedFormGroup;
  }

  get convertersGroup(): UntypedFormGroup {
    return this.form.get('converters') as UntypedFormGroup;
  }

  get connectionGroup(): UntypedFormGroup {
    return this.form.get('connection') as UntypedFormGroup;
  }

  private buildForm(integration: Integration) {
    const cfg = integration.configuration || {};
    const cc = cfg.clientConfiguration || {};
    const sqs = cfg.sqsConfiguration || {};
    const existingUplink = integration.defaultConverterId
      ? this.uplinkConverters.find((c) => c.id?.id === integration.defaultConverterId.id) : null;
    const existingDownlink = integration.downlinkConverterId
      ? this.downlinkConverters.find((c) => c.id?.id === integration.downlinkConverterId.id) : null;
    this.form = this.fb.group({
      basic: this.fb.group({
        type: [integration.type, [Validators.required]],
        name: [integration.name, [Validators.required]],
        enabled: [integration.enabled !== false],
        allowCreateDevicesOrAssets: [integration.allowCreateDevicesOrAssets !== false],
        debugSettings: [integration.debugSettings || { failuresEnabled: false, allEnabled: false, allEnabledUntil: 0 }]
      }),
      converters: this.fb.group({
        uplinkConverter: this.fb.group({
          // PE's uplink step: NEW (inline-edited, owned by this integration) / EXISTING (points at
          // any already-created uplink converter). No SKIP — uplink is mandatory.
          sourceType: ['new'],
          existingConverterId: [null],
          name: [existingUplink?.name || this.defaultConverterName(integration.type, true), [Validators.required]],
          debugSettings: [existingUplink?.debugSettings || { failuresEnabled: true, allEnabled: false, allEnabledUntil: 0 }],
          scriptLang: [existingUplink?.configuration?.scriptLang || ConverterScriptLanguage.TBEL],
          script: [existingUplink?.configuration?.decoder || DEFAULT_TBEL_DECODER, [Validators.required]],
          updateOnlyKeys: [existingUplink?.configuration?.updateOnlyKeys || []]
        }),
        downlinkConverter: this.fb.group({
          // PE's ConverterSourceType: NEW (inline-edited, owned by this integration) / EXISTING
          // (points at any already-created downlink converter, possibly shared) / SKIP. LIBRARY
          // (browsing the git-based vendor template repo) is not built in this port — see
          // ConverterLibraryController on the backend, which already has no frontend caller either.
          sourceType: [existingDownlink ? 'new' : 'skip'],
          existingConverterId: [existingDownlink ? null : null],
          name: [existingDownlink?.name || this.defaultConverterName(integration.type, false), [Validators.required]],
          debugSettings: [existingDownlink?.debugSettings || { failuresEnabled: true, allEnabled: false, allEnabledUntil: 0 }],
          scriptLang: [existingDownlink?.configuration?.scriptLang || ConverterScriptLanguage.TBEL],
          script: [existingDownlink?.configuration?.encoder || DEFAULT_TBEL_ENCODER, [Validators.required]],
          updateOnlyKeys: [existingDownlink?.configuration?.updateOnlyKeys || []]
        })
      }),
      connection: IntegrationConnectionFormComponent.buildConnectionGroup(this.fb, integration)
    });
    this.basicGroup.get('type').valueChanges.subscribe((type) => {
      IntegrationConnectionFormComponent.retargetValidators(this.connectionGroup, type);
      // The downlink step is hidden for types with no downlink direction; leaving sourceType on
      // 'new'/'existing' would keep its (now unreachable) required validators and silently block Save.
      if (integrationTypeInfoMap.get(type)?.hideDownlink) {
        this.downlinkConverterGroup.get('sourceType').setValue('skip');
      }
      if (!this.isEdit) {
        const uplinkNameCtrl = this.uplinkConverterGroup.get('name');
        if (!uplinkNameCtrl.dirty) {
          uplinkNameCtrl.setValue(this.defaultConverterName(type, true));
        }
        const downlinkNameCtrl = this.downlinkConverterGroup.get('name');
        if (!downlinkNameCtrl.dirty) {
          downlinkNameCtrl.setValue(this.defaultConverterName(type, false));
        }
      }
    });
    if (!this.isEdit) {
      this.uplinkConverterGroup.get('scriptLang').valueChanges.subscribe(() => this.resetUplinkScriptTemplate());
      this.downlinkConverterGroup.get('scriptLang').valueChanges.subscribe(() => this.resetDownlinkScriptTemplate());
    }
    const downlinkSourceTypeCtrl = this.downlinkConverterGroup.get('sourceType');
    const toggleDownlinkValidators = (sourceType: string) => {
      const nameCtrl = this.downlinkConverterGroup.get('name');
      const scriptCtrl = this.downlinkConverterGroup.get('script');
      const existingCtrl = this.downlinkConverterGroup.get('existingConverterId');
      const isNew = sourceType === 'new';
      nameCtrl.setValidators(isNew ? [Validators.required] : []);
      scriptCtrl.setValidators(isNew ? [Validators.required] : []);
      existingCtrl.setValidators(sourceType === 'existing' ? [Validators.required] : []);
      nameCtrl.updateValueAndValidity();
      scriptCtrl.updateValueAndValidity();
      existingCtrl.updateValueAndValidity();
    };
    toggleDownlinkValidators(downlinkSourceTypeCtrl.value);
    downlinkSourceTypeCtrl.valueChanges.subscribe((sourceType) => toggleDownlinkValidators(sourceType));

    const uplinkSourceTypeCtrl = this.uplinkConverterGroup.get('sourceType');
    const toggleUplinkValidators = (sourceType: string) => {
      const nameCtrl = this.uplinkConverterGroup.get('name');
      const scriptCtrl = this.uplinkConverterGroup.get('script');
      const existingCtrl = this.uplinkConverterGroup.get('existingConverterId');
      const isNew = sourceType === 'new';
      nameCtrl.setValidators(isNew ? [Validators.required] : []);
      scriptCtrl.setValidators(isNew ? [Validators.required] : []);
      existingCtrl.setValidators(sourceType === 'existing' ? [Validators.required] : []);
      nameCtrl.updateValueAndValidity();
      scriptCtrl.updateValueAndValidity();
      existingCtrl.updateValueAndValidity();
    };
    toggleUplinkValidators(uplinkSourceTypeCtrl.value);
    uplinkSourceTypeCtrl.valueChanges.subscribe((sourceType) => toggleUplinkValidators(sourceType));
  }

  private defaultConverterName(type: IntegrationType, isUplink: boolean): string {
    const typeName = type && integrationTypeInfoMap.has(type) ? this.translate.instant(integrationTypeInfoMap.get(type).name) : '';
    const key = isUplink ? 'integration.default-uplink-converter-name' : 'integration.default-downlink-converter-name';
    return this.translate.instant(key, { type: typeName });
  }

  private resetUplinkScriptTemplate() {
    const isTbel = this.uplinkConverterGroup.get('scriptLang').value === ConverterScriptLanguage.TBEL;
    this.uplinkConverterGroup.get('script').setValue(isTbel ? DEFAULT_TBEL_DECODER : DEFAULT_JS_DECODER);
  }

  private resetDownlinkScriptTemplate() {
    const isTbel = this.downlinkConverterGroup.get('scriptLang').value === ConverterScriptLanguage.TBEL;
    this.downlinkConverterGroup.get('script').setValue(isTbel ? DEFAULT_TBEL_ENCODER : DEFAULT_JS_ENCODER);
  }

  get uplinkConverterGroup(): UntypedFormGroup {
    return this.convertersGroup.get('uplinkConverter') as UntypedFormGroup;
  }

  get downlinkConverterGroup(): UntypedFormGroup {
    return this.convertersGroup.get('downlinkConverter') as UntypedFormGroup;
  }

  get uplinkScriptEditorLang(): ScriptLanguage {
    return this.uplinkConverterGroup.get('scriptLang').value === ConverterScriptLanguage.JS ? ScriptLanguage.JS : ScriptLanguage.TBEL;
  }

  get downlinkScriptEditorLang(): ScriptLanguage {
    return this.downlinkConverterGroup.get('scriptLang').value === ConverterScriptLanguage.JS ? ScriptLanguage.JS : ScriptLanguage.TBEL;
  }

  // PE ships separate TBEL / JS help docs and swaps them with the script language — the TBEL ones
  // link to the TBEL guide and their examples omit helpers TBEL provides natively.
  get uplinkScriptHelpId(): string {
    return (this.uplinkConverterGroup.get('scriptLang').value === ConverterScriptLanguage.JS
      ? 'converter/' : 'converter/tbel/') + 'decoder_fn';
  }

  get downlinkScriptHelpId(): string {
    return (this.downlinkConverterGroup.get('scriptLang').value === ConverterScriptLanguage.JS
      ? 'converter/' : 'converter/tbel/') + 'encoder_fn';
  }

  testUplinkConverter(): void {
    this.dialog.open<ConverterTestDialogComponent, ConverterTestDialogData, string>(ConverterTestDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
      data: {
        type: ConverterType.UPLINK,
        script: this.uplinkConverterGroup.get('script').value,
        scriptLang: this.uplinkConverterGroup.get('scriptLang').value
      }
    }).afterClosed().subscribe((savedScript) => {
      if (savedScript !== undefined) {
        this.uplinkConverterGroup.get('script').setValue(savedScript);
      }
    });
  }

  testDownlinkConverter(): void {
    this.dialog.open<ConverterTestDialogComponent, ConverterTestDialogData, string>(ConverterTestDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
      data: {
        type: ConverterType.DOWNLINK,
        script: this.downlinkConverterGroup.get('script').value,
        scriptLang: this.downlinkConverterGroup.get('scriptLang').value
      }
    }).afterClosed().subscribe((savedScript) => {
      if (savedScript !== undefined) {
        this.downlinkConverterGroup.get('script').setValue(savedScript);
      }
    });
  }


  get selectedType(): IntegrationType {
    const value = this.basicGroup?.get('type')?.value;
    return this.integrationTypes.includes(value) ? value : null;
  }

  displayTypeFn = (type: IntegrationType): string => {
    if (!type) {
      return '';
    }
    const info = this.integrationTypeInfoMap.get(type);
    return info ? this.translate.instant(info.name) : '';
  };

  get filteredIntegrationTypes(): IntegrationType[] {
    const raw = this.basicGroup?.get('type')?.value;
    if (!raw || this.integrationTypes.includes(raw)) {
      return this.integrationTypes;
    }
    const query = String(raw).toLowerCase();
    return this.integrationTypes.filter((type) => {
      const info = this.integrationTypeInfoMap.get(type);
      return this.translate.instant(info.name).toLowerCase().includes(query);
    });
  }

  clearType(event: Event) {
    event.stopPropagation();
    this.basicGroup.get('type').setValue(null);
  }




  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const basic = this.basicGroup.value;
    const uplink = this.uplinkConverterGroup.value;
    const downlink = this.downlinkConverterGroup.value;
    const c = this.connectionGroup.value;
    const configuration: any = IntegrationConnectionFormComponent.buildConfiguration(
      basic.type, c, this.integration?.routingKey);

    // 'new' upserts this integration's own uplink converter (same id every save); 'existing' just
    // points defaultConverterId at a converter picked from the dropdown, with no write to it.
    const uplink$ = uplink.sourceType === 'existing'
      ? of({ id: new ConverterId(uplink.existingConverterId) } as Converter)
      : this.converterService.saveConverter({
          ...(this.integration?.defaultConverterId ? { id: this.integration.defaultConverterId } : {}),
          name: uplink.name,
          type: ConverterType.UPLINK,
          integrationType: basic.type,
          debugSettings: uplink.debugSettings,
          configuration: { scriptLang: uplink.scriptLang, decoder: uplink.script, updateOnlyKeys: uplink.updateOnlyKeys || [] }
        } as Converter);

    uplink$.pipe(
      switchMap((savedUplink) => {
        // 'new' upserts this integration's own converter (same id every save, like before this
        // 3-way picker existed); 'existing' just points at a converter picked from the dropdown,
        // with no write to it; 'skip' clears the link.
        if (downlink.sourceType === 'new') {
          const downlinkConverter: Converter = {
            ...(this.integration?.downlinkConverterId ? { id: this.integration.downlinkConverterId } : {}),
            name: downlink.name,
            type: ConverterType.DOWNLINK,
            integrationType: basic.type,
            debugSettings: downlink.debugSettings,
            configuration: { scriptLang: downlink.scriptLang, encoder: downlink.script, updateOnlyKeys: downlink.updateOnlyKeys || [] }
          } as Converter;
          return this.converterService.saveConverter(downlinkConverter).pipe(
            map((savedDownlink) => ({ savedUplink, downlinkConverterId: savedDownlink.id }))
          );
        }
        if (downlink.sourceType === 'existing') {
          return of({ savedUplink, downlinkConverterId: new ConverterId(downlink.existingConverterId) });
        }
        return of({ savedUplink, downlinkConverterId: null });
      }),
      switchMap(({ savedUplink, downlinkConverterId }) => {
        const integration: Integration = {
          ...(this.integration || {} as Integration),
          name: basic.name,
          type: basic.type,
          enabled: basic.enabled,
          allowCreateDevicesOrAssets: basic.allowCreateDevicesOrAssets,
          debugSettings: basic.debugSettings,
          remote: c.remote,
          // Explicit rather than relying on the spread: these are minted client-side at create
          // time (PE parity) and the backend only generates its own when they arrive empty.
          routingKey: this.integration?.routingKey,
          secret: this.integration?.secret,
          defaultConverterId: savedUplink.id,
          downlinkConverterId,
          configuration,
          additionalInfo: {...(this.integration?.additionalInfo || {}), description: c.description}
        };
        return this.integrationService.saveIntegration(integration);
      })
    ).subscribe(() => this.dialogRef.close(true));
  }

  get selectedTypeInfo(): IntegrationTypeInfo {
    return this.selectedType ? integrationTypeInfoMap.get(this.selectedType) : null;
  }

  /** Both are server-generated, so they only exist once the integration has been saved. */
  get integrationKey(): string {
    return this.integration?.routingKey || null;
  }

  get integrationSecret(): string {
    return this.integration?.secret || null;
  }

  copyToClipboard(value: string, what: 'key' | 'secret' | 'endpoint') {
    if (!value) {
      return;
    }
    const messageKey = {
      key: 'integration.integration-key-copied-message',
      secret: 'integration.integration-secret-copied-message',
      endpoint: 'integration.http-endpoint-url-copied-message'
    }[what];
    navigator.clipboard.writeText(value).then(() => {
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant(messageKey),
        type: 'success',
        duration: 1500,
        verticalPosition: 'top',
        horizontalPosition: 'left'
      }));
    });
  }

  /** PE hides the downlink converter step entirely for types that have no downlink direction. */
  get showDownlinkStep(): boolean {
    return !this.selectedTypeInfo?.hideDownlink;
  }

  /**
   * PE offers the probe only for types where it is meaningful, and never for a remote integration —
   * the platform cannot dial the broker on the remote process's behalf.
   */
  get isCheckConnectionAvailable(): boolean {
    return !!this.selectedTypeInfo?.checkConnection && !this.connectionGroup?.get('remote')?.value;
  }

  checkConnection(): void {
    if (this.connectionGroup.invalid) {
      this.connectionGroup.markAllAsTouched();
      return;
    }
    const basic = this.basicGroup.value;
    const probe: Integration = {
      ...(this.integration || {} as Integration),
      name: basic.name,
      type: basic.type,
      configuration: IntegrationConnectionFormComponent.buildConfiguration(
        basic.type, this.connectionGroup.value, this.integration?.routingKey)
    };
    this.checkInProgress = true;
    this.checkResult = null;
    this.integrationService.checkIntegrationConnection(probe, {ignoreErrors: true}).subscribe({
      next: () => {
        this.checkInProgress = false;
        this.checkResult = {success: true};
      },
      error: (err) => {
        this.checkInProgress = false;
        this.checkResult = {success: false, error: err?.error?.message || err?.message || ''};
      }
    });
  }

}
