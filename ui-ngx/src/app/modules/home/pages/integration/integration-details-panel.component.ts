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

import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { DebugEventType } from '@shared/models/event.models';
import { AuditLogMode } from '@shared/models/audit-log.models';
import { AttributeScope, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import {
  Integration,
  IntegrationType,
  integrationTypeInfoMap,
  MqttCredentialsType,
  MqttProtocolVersion,
  mqttProtocolVersionLabels,
  MQTT_FAMILY_INTEGRATION_TYPES
} from '@shared/models/integration.models';
import { Converter, ConverterScriptLanguage, ConverterType, converterTypeTranslations } from '@shared/models/converter.models';
import { IntegrationService } from '@core/http/integration.service';
import { ConverterService } from '@core/http/converter.service';
import { MatDialog } from '@angular/material/dialog';
import {
  ConverterDialogComponent,
  ConverterDialogData
} from '@home/pages/integration/converter/converter-dialog.component';
import {
  ConverterTestDialogComponent,
  ConverterTestDialogData
} from '@home/pages/integration/converter/converter-test-dialog.component';

export type IntegrationDetailsPanelKind = 'integration' | 'converter';

export interface IntegrationDetailsPanelTarget {
  kind: IntegrationDetailsPanelKind;
  entity: Integration | Converter;
}

interface ConfigRow {
  key: string;
  value: string;
}

/**
 * PE's `tb-integration-tabs`/`tb-converter-tabs`, adapted to how this CE port structures the
 * feature: a slide-in side panel (`mat-drawer`, hosted by the list pages) rather than PE's generic
 * `tb-entities-table` details panel, since Integration/Converter here use a purpose-built wizard
 * dialog for add/edit instead of that framework. 详情 (Overview) mirrors PE's own approach of reusing
 * the same field set as the add/edit form, bound to a `.disable()`d `FormGroup` instead of a
 * hand-rolled read-only template — the pencil/edit button opens the wizard for actual editing.
 *
 * MQTT-family types (the only ones this port can live-verify against both a running CE instance and
 * PE's own UI) get the full field-for-field layout copied from `integration-dialog.component.html`'s
 * connection step. The other ~25 integration types still fall back to a flattened key/value dump of
 * the raw `configuration` JSON — same information, without duplicating every type's bespoke template
 * a second time in a component that can't be exercised against a live instance of each protocol.
 *
 * Named `tb-integration-details-panel` (not the more obvious `tb-entity-details-panel`) because CE
 * already has a generic `EntityDetailsPanelComponent`/`tb-entity-details-panel` as part of the
 * `tb-entities-table` framework this component deliberately does not use — reusing that selector
 * caused a live NG0300 "multiple components match" crash the first time this shipped.
 */
@Component({
  selector: 'tb-integration-details-panel',
  templateUrl: './integration-details-panel.component.html',
  styleUrls: ['./integration-details-panel.component.scss'],
  standalone: false
})
export class IntegrationDetailsPanelComponent implements OnChanges {

  @Input() target: IntegrationDetailsPanelTarget;
  @Input() focusTab: 'overview' | 'events' = 'overview';

  @Output() closed = new EventEmitter<void>();
  @Output() editRequested = new EventEmitter<void>();
  @Output() deleteRequested = new EventEmitter<void>();
  /** Fired after a successful in-place save, so the list page can refresh its row without closing the panel. */
  @Output() saved = new EventEmitter<void>();

  EntityType = EntityType;
  AttributeScope = AttributeScope;
  LatestTelemetry = LatestTelemetry;
  AuditLogMode = AuditLogMode;
  ConverterType = ConverterType;
  ConverterScriptLanguage = ConverterScriptLanguage;
  MqttCredentialsType = MqttCredentialsType;
  mqttProtocolVersionLabels = mqttProtocolVersionLabels;
  /** Same option list the converter dialog offers, so the 详情 tab's picker matches it. */
  integrationTypes = Object.values(IntegrationType);

  selectedIndex = 0;
  configRows: ConfigRow[] = [];
  uplinkConverterName: string = null;
  downlinkConverterName: string = null;
  /** PE edits in place (the same disabled fields become editable) rather than opening a separate
   *  wizard dialog. Only possible where every field already has a real, bindable control: converters
   *  (fully form-backed) and MQTT-family integrations. The other ~25 integration types still fall
   *  back to the read-only configRows dump, so their edit button keeps opening the wizard dialog. */
  editMode = false;
  saving = false;
  checkInProgress = false;
  checkResult: {success: boolean; error?: string} = null;

  /** Universal fields for every integration; MQTT-family types get extra connection controls added
   *  in {@link buildIntegrationForm}. Always `.disable()`d immediately after being built. */
  integrationForm: UntypedFormGroup;
  /** Populated (and immediately `.disable()`d) for every converter. */
  converterForm: UntypedFormGroup;

  constructor(private store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private integrationService: IntegrationService,
              private converterService: ConverterService,
              private dialog: MatDialog) {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.target) {
      this.integrationForm = null;
      this.converterForm = null;
      this.configRows = [];
      this.uplinkConverterName = null;
      this.downlinkConverterName = null;
      this.editMode = false;
      this.saving = false;
      this.checkInProgress = false;
      this.checkResult = null;
      if (this.target?.kind === 'integration') {
        const integration = this.target.entity as Integration;
        this.buildIntegrationForm(integration);
        this.loadConverterNames(integration);
      } else if (this.target?.kind === 'converter') {
        this.buildConverterForm(this.target.entity as Converter);
      }
    }
    if (changes.focusTab || changes.target) {
      this.selectedIndex = this.focusTab === 'events' ? 1 : 0;
    }
  }

  // Universal fields (every integration type) always become real, disabled form controls, matching
  // PE's approach of reusing the same fields the add/edit wizard uses. MQTT-family types additionally
  // get their connection fields as real controls too, copied from integration-dialog.component.ts's
  // buildForm() — every other type falls back to a flattened dump of `configuration` (see class doc).
  private buildIntegrationForm(integration: Integration) {
    const cfg: any = integration.configuration || {};
    this.integrationForm = this.fb.group({
      name: [integration.name, [Validators.required]],
      enabled: [integration.enabled !== false],
      allowCreateDevicesOrAssets: [integration.allowCreateDevicesOrAssets !== false],
      debugSettings: [integration.debugSettings || { failuresEnabled: false, allEnabled: false, allEnabledUntil: 0 }],
      remote: [integration.remote === true],
      description: [integration.additionalInfo?.description || ''],
      metadata: [cfg.metadata || {}],
      // Editable in edit mode via the clear / edit / "create new" actions beside each field,
      // mirroring PE's tb-converter-autocomplete.
      defaultConverterId: [integration.defaultConverterId || null, [Validators.required]],
      downlinkConverterId: [integration.downlinkConverterId || null]
    });
    if (this.isMqttFamilyType) {
      const cc = cfg.clientConfiguration || {};
      this.integrationForm.addControl('host', this.fb.control(cc.host || '', [Validators.required]));
      this.integrationForm.addControl('port', this.fb.control(cc.port || '', [Validators.required]));
      this.integrationForm.addControl('clientId', this.fb.control(cc.clientId || ''));
      this.integrationForm.addControl('ssl', this.fb.control(cc.ssl === true));
      this.integrationForm.addControl('protocolVersion', this.fb.control(cc.protocolVersion || MqttProtocolVersion.MQTT_3_1_1));
      this.integrationForm.addControl('cleanSession', this.fb.control(cc.cleanSession !== false));
      this.integrationForm.addControl('retainedMessage', this.fb.control(cc.retainedMessage === true));
      this.integrationForm.addControl('maxBytesInMessage', this.fb.control(cc.maxBytesInMessage || 32368));
      this.integrationForm.addControl('connectTimeoutSec', this.fb.control(cc.connectTimeoutSec || 10));
      this.integrationForm.addControl('downlinkTopicPattern', this.fb.control(cfg.downlinkTopicPattern || '${topic}'));
      this.integrationForm.addControl('credentialsType', this.fb.control(cc.credentials?.type || MqttCredentialsType.ANONYMOUS));
      this.integrationForm.addControl('username', this.fb.control(cc.credentials?.username || ''));
      this.integrationForm.addControl('password', this.fb.control(cc.credentials?.password || ''));
      this.integrationForm.addControl('caCert', this.fb.control(cc.credentials?.caCert || ''));
      this.integrationForm.addControl('cert', this.fb.control(cc.credentials?.cert || ''));
      this.integrationForm.addControl('privateKey', this.fb.control(cc.credentials?.privateKey || ''));
      this.integrationForm.addControl('privateKeyPassword', this.fb.control(cc.credentials?.privateKeyPassword || ''));
      this.integrationForm.addControl('sasKey', this.fb.control(cc.credentials?.sasKey || ''));
      // PE always renders at least one topic filter row (its form seeds '#'/QoS 0), so the
      // 主题过滤器 section is never empty there. Mirror that instead of hiding the whole section.
      const topicFilters = (cfg.topicFilters?.length ? cfg.topicFilters : [{ filter: '#', qos: 0 }]);
      this.integrationForm.addControl('topicFilters', this.fb.array(topicFilters.map((tf: any) =>
        this.fb.group({ filter: [tf.filter], qos: [tf.qos ?? 0] }))));
    } else {
      // Metadata is always rendered as a real field above (tb-key-val-map), so exclude it here to
      // avoid showing it twice.
      this.configRows = this.flatten(cfg).filter((row) => !row.key.startsWith('metadata'));
    }
    this.integrationForm.disable({ emitEvent: false });
  }

  get topicFiltersArray(): UntypedFormArray {
    return this.integrationForm?.get('topicFilters') as UntypedFormArray;
  }

  addTopicFilter() {
    this.topicFiltersArray.push(this.fb.group({ filter: ['#'], qos: [0] }));
  }

  removeTopicFilter(i: number) {
    this.topicFiltersArray.removeAt(i);
  }

  /**
   * Same gating as the wizard: only for types PE marks `checkConnection`, and never for a remote
   * integration (the platform cannot dial the broker on the remote process's behalf).
   */
  get isCheckConnectionAvailable(): boolean {
    const info = this.integration ? integrationTypeInfoMap.get(this.integration.type) : null;
    return !!info?.checkConnection && !this.integrationForm?.get('remote')?.value;
  }

  checkConnection(): void {
    this.checkInProgress = true;
    this.checkResult = null;
    const probe = this.buildEditedIntegration();
    this.integrationService.checkIntegrationConnection(probe, { ignoreErrors: true }).subscribe({
      next: () => {
        this.checkInProgress = false;
        this.checkResult = { success: true };
      },
      error: (err) => {
        this.checkInProgress = false;
        this.checkResult = { success: false, error: err?.error?.message || err?.message || '' };
      }
    });
  }

  private buildConverterForm(converter: Converter) {
    const cfg: any = converter.configuration || {};
    const isUplink = converter.type === ConverterType.UPLINK;
    // Same control set as converter-dialog.component.ts's buildForm(), so the 详情 tab and the
    // create/edit dialog stay in sync field-for-field.
    this.converterForm = this.fb.group({
      name: [converter.name, [Validators.required]],
      type: [converter.type],
      integrationType: [converter.integrationType || ''],
      debugSettings: [converter.debugSettings || { failuresEnabled: false, allEnabled: false, allEnabledUntil: 0 }],
      scriptLang: [cfg.scriptLang || ConverterScriptLanguage.TBEL],
      script: [isUplink ? (cfg.decoder || '') : (cfg.encoder || '')],
      updateOnlyKeys: [cfg.updateOnlyKeys || []],
      description: [converter.additionalInfo?.description || '']
    });
    this.converterForm.disable({ emitEvent: false });
  }

  get converterScriptEditorLang(): ScriptLanguage {
    return this.converterForm?.get('scriptLang')?.value === ConverterScriptLanguage.JS ? ScriptLanguage.JS : ScriptLanguage.TBEL;
  }

  get converterIsUplink(): boolean {
    return this.converterForm?.get('type')?.getRawValue() === ConverterType.UPLINK;
  }

  /** PE swaps the help doc with the script language; see converter-dialog's identical getter. */
  get converterScriptHelpId(): string {
    const base = this.converterForm?.get('scriptLang')?.getRawValue() === ConverterScriptLanguage.JS
      ? 'converter/' : 'converter/tbel/';
    return base + (this.converterIsUplink ? 'decoder_fn' : 'encoder_fn');
  }

  testConverterFunction(): void {
    this.dialog.open<ConverterTestDialogComponent, ConverterTestDialogData, string>(
      ConverterTestDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          type: this.converterForm.get('type').getRawValue(),
          script: this.converterForm.get('script').getRawValue(),
          scriptLang: this.converterForm.get('scriptLang').getRawValue()
        }
      }).afterClosed().subscribe((savedScript) => {
      // Only writable while editing; in read mode the control is disabled and setValue is a no-op
      // that would silently diverge from the persisted entity.
      if (savedScript !== undefined && this.editMode) {
        this.converterForm.get('script').setValue(savedScript);
      }
    });
  }

  // --- Converter slot actions (edit mode only), mirroring PE's tb-converter-autocomplete ---

  /** Clears the slot. Only offered for downlink; uplink is required, same as PE. */
  clearConverter(kind: 'uplink' | 'downlink') {
    const ctrl = this.integrationForm.get(kind === 'uplink' ? 'defaultConverterId' : 'downlinkConverterId');
    ctrl.setValue(null);
    if (kind === 'uplink') {
      this.uplinkConverterName = null;
    } else {
      this.downlinkConverterName = null;
    }
  }

  editConverter(kind: 'uplink' | 'downlink') {
    const id = this.integrationForm.get(kind === 'uplink' ? 'defaultConverterId' : 'downlinkConverterId')?.value?.id;
    if (!id) {
      return;
    }
    this.openConverterDialog({ converterId: id }, kind);
  }

  createConverter(kind: 'uplink' | 'downlink') {
    this.openConverterDialog({
      presetType: kind === 'uplink' ? ConverterType.UPLINK : ConverterType.DOWNLINK,
      presetIntegrationType: this.integration?.type
    }, kind);
  }

  private openConverterDialog(data: ConverterDialogData, kind: 'uplink' | 'downlink') {
    this.dialog.open<ConverterDialogComponent, ConverterDialogData, Converter | boolean>(
      ConverterDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data
      }).afterClosed().subscribe((saved) => {
      if (!saved) {
        return;
      }
      const converter = saved as Converter;
      // Newly created converters get bound into the slot; edits just refresh the displayed name.
      if (converter.id) {
        this.integrationForm.get(kind === 'uplink' ? 'defaultConverterId' : 'downlinkConverterId')
          .setValue(converter.id);
        if (kind === 'uplink') {
          this.uplinkConverterName = converter.name;
        } else {
          this.downlinkConverterName = converter.name;
        }
      }
    });
  }

  private loadConverterNames(integration: Integration) {
    forkJoin([
      integration.defaultConverterId
        ? this.converterService.getConverter(integration.defaultConverterId.id).pipe(catchError(() => of(null)))
        : of(null),
      integration.downlinkConverterId
        ? this.converterService.getConverter(integration.downlinkConverterId.id).pipe(catchError(() => of(null)))
        : of(null)
    ]).subscribe(([uplink, downlink]) => {
      this.uplinkConverterName = uplink?.name || null;
      this.downlinkConverterName = downlink?.name || null;
    });
  }

  // Recursively flattens the connection configuration into dot-path rows — the fallback for every
  // integration type other than MQTT-family (see class doc).
  private flatten(obj: any, prefix = ''): ConfigRow[] {
    const rows: ConfigRow[] = [];
    if (!obj || typeof obj !== 'object') {
      return rows;
    }
    for (const key of Object.keys(obj)) {
      const value = obj[key];
      const path = prefix ? `${prefix}.${key}` : key;
      if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
        rows.push(...this.flatten(value, path));
      } else if (value !== null && value !== undefined && value !== '') {
        rows.push({ key: path, value: Array.isArray(value) ? JSON.stringify(value) : String(value) });
      }
    }
    return rows;
  }

  get integration(): Integration {
    return this.target?.kind === 'integration' ? this.target.entity as Integration : null;
  }

  get converter(): Converter {
    return this.target?.kind === 'converter' ? this.target.entity as Converter : null;
  }

  get isMqttFamilyType(): boolean {
    return this.integration ? MQTT_FAMILY_INTEGRATION_TYPES.includes(this.integration.type) : false;
  }

  get entityId(): EntityId {
    return this.target?.entity?.id as unknown as EntityId;
  }

  get entityName(): string {
    return this.target?.entity ? (this.target.entity as any).name : '';
  }

  get subtitle(): string {
    return this.target?.kind === 'integration' ? 'integration.integration-details' : 'converter.converter-details';
  }

  get pageRoute(): string[] {
    const id = this.target?.entity?.id?.id;
    if (!id) {
      return [];
    }
    return this.target.kind === 'integration'
      ? ['/integrations-center/integrations', id]
      : ['/integrations-center/converters', id];
  }

  get typeName(): string {
    if (this.target?.kind === 'integration') {
      const info = integrationTypeInfoMap.get(this.integration.type);
      return info ? this.translate.instant(info.name) : this.integration.type;
    }
    if (this.target?.kind === 'converter') {
      return this.translate.instant(converterTypeTranslations.get(this.converter.type));
    }
    return '';
  }

  get eventsLabel(): string {
    return this.target?.kind === 'integration' ? 'integration.events' : 'converter.events';
  }

  get defaultEventType(): DebugEventType {
    return this.target?.kind === 'integration' ? DebugEventType.DEBUG_INTEGRATION : DebugEventType.DEBUG_CONVERTER;
  }

  get debugEventTypes(): DebugEventType[] {
    return [this.defaultEventType];
  }

  get deleteLabel(): string {
    return this.target?.kind === 'integration' ? 'integration.delete' : 'converter.delete';
  }

  get copyIdLabel(): string {
    return this.target?.kind === 'integration' ? 'integration.copy-integration-id' : 'converter.copy-converter-id';
  }

  get copyIdMessage(): string {
    return this.target?.kind === 'integration' ? 'integration.integration-id-copied-message' : 'converter.converter-id-copied-message';
  }

  copyToClipboard(value: string, messageKey: string) {
    if (!value) {
      return;
    }
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

  /** Converters are always fully form-backed; integrations only when MQTT-family (see class doc). */
  get canEditInline(): boolean {
    return this.target?.kind === 'converter' || (this.target?.kind === 'integration' && this.isMqttFamilyType);
  }

  edit(): void {
    if (!this.canEditInline) {
      this.editRequested.emit();
      return;
    }
    this.editMode = true;
    const form = this.target.kind === 'integration' ? this.integrationForm : this.converterForm;
    form.enable({ emitEvent: false });
  }

  cancelEdit(): void {
    this.editMode = false;
    if (this.target?.kind === 'integration') {
      this.buildIntegrationForm(this.target.entity as Integration);
    } else if (this.target?.kind === 'converter') {
      this.buildConverterForm(this.target.entity as Converter);
    }
  }

  saveEdit(): void {
    const form = this.target.kind === 'integration' ? this.integrationForm : this.converterForm;
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    this.saving = true;
    if (this.target.kind === 'integration') {
      this.integrationService.saveIntegration(this.buildEditedIntegration()).subscribe({
        next: (saved) => this.onEditSaved({ kind: 'integration', entity: saved }),
        error: () => { this.saving = false; }
      });
    } else {
      this.converterService.saveConverter(this.buildEditedConverter()).subscribe({
        next: (saved) => this.onEditSaved({ kind: 'converter', entity: saved }),
        error: () => { this.saving = false; }
      });
    }
  }

  private onEditSaved(target: IntegrationDetailsPanelTarget) {
    this.saving = false;
    this.editMode = false;
    this.target = target;
    if (target.kind === 'integration') {
      this.buildIntegrationForm(target.entity as Integration);
      this.loadConverterNames(target.entity as Integration);
    } else {
      this.buildConverterForm(target.entity as Converter);
    }
    this.saved.emit();
  }

  private buildEditedIntegration(): Integration {
    const v = this.integrationForm.value;
    const original = this.target.entity as Integration;
    const cfg: any = { ...(original.configuration || {}) };
    if (this.isMqttFamilyType) {
      cfg.clientConfiguration = {
        ...(cfg.clientConfiguration || {}),
        host: v.host,
        port: v.port,
        clientId: v.clientId || undefined,
        ssl: v.ssl,
        protocolVersion: v.protocolVersion,
        cleanSession: v.cleanSession,
        retainedMessage: v.retainedMessage,
        maxBytesInMessage: v.maxBytesInMessage,
        connectTimeoutSec: v.connectTimeoutSec,
        credentials: this.buildMqttCredentials(v)
      };
      cfg.topicFilters = v.topicFilters;
      cfg.downlinkTopicPattern = v.downlinkTopicPattern;
    }
    cfg.metadata = v.metadata;
    return {
      ...original,
      name: v.name,
      enabled: v.enabled,
      allowCreateDevicesOrAssets: v.allowCreateDevicesOrAssets,
      debugSettings: v.debugSettings,
      remote: v.remote,
      defaultConverterId: v.defaultConverterId,
      downlinkConverterId: v.downlinkConverterId,
      configuration: cfg,
      additionalInfo: { ...(original.additionalInfo || {}), description: v.description }
    };
  }

  private buildMqttCredentials(v: any): any {
    switch (v.credentialsType) {
      case MqttCredentialsType.BASIC:
        return { type: MqttCredentialsType.BASIC, username: v.username, password: v.password };
      case MqttCredentialsType.CERT_PEM:
        return { type: MqttCredentialsType.CERT_PEM, caCert: v.caCert, cert: v.cert, privateKey: v.privateKey, privateKeyPassword: v.privateKeyPassword };
      case MqttCredentialsType.SAS:
        return { type: MqttCredentialsType.SAS, sasKey: v.sasKey, caCert: v.caCert };
      default:
        return { type: MqttCredentialsType.ANONYMOUS };
    }
  }

  private buildEditedConverter(): Converter {
    // getRawValue(): `type` is rendered read-only for an existing converter (same as the dialog,
    // which disables the toggle when editing), and .value drops disabled controls.
    const v = this.converterForm.getRawValue();
    const original = this.target.entity as Converter;
    const isUplink = v.type === ConverterType.UPLINK;
    return {
      ...original,
      name: v.name,
      type: v.type,
      integrationType: v.integrationType || null,
      debugSettings: v.debugSettings,
      configuration: {
        ...(original.configuration || {}),
        scriptLang: v.scriptLang,
        ...(isUplink ? { decoder: v.script } : { encoder: v.script }),
        updateOnlyKeys: v.updateOnlyKeys || []
      },
      additionalInfo: { ...(original.additionalInfo || {}), description: v.description }
    };
  }

  delete(): void {
    this.deleteRequested.emit();
  }

  close(): void {
    this.closed.emit();
  }

}
