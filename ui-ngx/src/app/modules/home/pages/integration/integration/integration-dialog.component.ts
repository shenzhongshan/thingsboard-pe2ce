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
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { forkJoin } from 'rxjs';
import { IntegrationService } from '@core/http/integration.service';
import { ConverterService } from '@core/http/converter.service';
import { PageLink } from '@shared/models/page/page-link';
import { Converter, ConverterType } from '@shared/models/converter.models';
import {
  Integration,
  integrationHttpEndpointUrl,
  IntegrationType,
  integrationTypeInfoMap,
  newIntegration,
  TcpHandlerType
} from '@shared/models/integration.models';

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

  IntegrationType = IntegrationType;
  TcpHandlerType = TcpHandlerType;
  integrationTypeInfoMap = integrationTypeInfoMap;
  integrationTypes = Object.values(IntegrationType);
  tcpHandlerTypes = Object.values(TcpHandlerType);

  uplinkConverters: Converter[] = [];
  downlinkConverters: Converter[] = [];

  private integration: Integration;

  constructor(@Inject(MAT_DIALOG_DATA) public data: IntegrationDialogData,
              private dialogRef: MatDialogRef<IntegrationDialogComponent, boolean>,
              protected store: Store<AppState>,
              private integrationService: IntegrationService,
              private converterService: ConverterService,
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
    this.form = this.fb.group({
      basic: this.fb.group({
        type: [integration.type, [Validators.required]],
        name: [integration.name, [Validators.required]],
        enabled: [integration.enabled !== false],
        allowCreateDevicesOrAssets: [integration.allowCreateDevicesOrAssets !== false],
        debugMode: [integration.debugMode === true]
      }),
      converters: this.fb.group({
        defaultConverterId: [integration.defaultConverterId?.id || null, [Validators.required]],
        downlinkConverterId: [integration.downlinkConverterId?.id || null]
      }),
      connection: this.fb.group({
        // MQTT connection
        host: [cfg.host || '', integration.type === IntegrationType.MQTT ? [Validators.required] : []],
        port: [cfg.port || 1883],
        clientId: [cfg.clientId || ''],
        credentialsType: [cfg.credentials?.type || 'anonymous'],
        username: [cfg.credentials?.username || ''],
        password: [cfg.credentials?.password || ''],
        topicFilters: this.fb.array((cfg.topicFilters || []).map((tf: any) => this.buildTopicFilterRow(tf.filter, tf.qos))),
        // TCP
        tcpPort: [cfg.port || 10560, integration.type === IntegrationType.TCP ? [Validators.required] : []],
        handlerType: [cfg.handlerType || TcpHandlerType.TEXT],
        maxFrameLength: [cfg.maxFrameLength || 65535],
        charsetName: [cfg.charsetName || 'UTF-8'],
        delimiter: [cfg.delimiter || '\\n']
      })
    });
    this.basicGroup.get('type').valueChanges.subscribe((type) => {
      const hostCtrl = this.connectionGroup.get('host');
      hostCtrl.setValidators(type === IntegrationType.MQTT ? [Validators.required] : []);
      hostCtrl.updateValueAndValidity();
      const tcpPortCtrl = this.connectionGroup.get('tcpPort');
      tcpPortCtrl.setValidators(type === IntegrationType.TCP ? [Validators.required] : []);
      tcpPortCtrl.updateValueAndValidity();
    });
  }

  private buildTopicFilterRow(filter: string, qos: number): UntypedFormGroup {
    return this.fb.group({
      filter: [filter, [Validators.required]],
      qos: [qos ?? 0]
    });
  }

  get topicFiltersArray(): UntypedFormArray {
    return this.connectionGroup.get('topicFilters') as UntypedFormArray;
  }

  addTopicFilter() {
    this.topicFiltersArray.push(this.buildTopicFilterRow('#', 0));
  }

  removeTopicFilter(i: number) {
    this.topicFiltersArray.removeAt(i);
  }

  selectType(type: IntegrationType) {
    this.basicGroup.get('type').patchValue(type);
  }

  get selectedType(): IntegrationType {
    return this.basicGroup?.get('type')?.value;
  }

  get httpRoutingKey(): string {
    return this.integration?.routingKey || null;
  }

  get httpEndpointUrl(): string {
    return this.integration?.routingKey ? integrationHttpEndpointUrl(this.integration.routingKey) : null;
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
    const conv = this.convertersGroup.value;
    const c = this.connectionGroup.value;
    const configuration: any = {};
    if (basic.type === IntegrationType.MQTT) {
      configuration.host = c.host;
      configuration.port = c.port;
      if (c.clientId) {
        configuration.clientId = c.clientId;
      }
      configuration.credentials = c.credentialsType === 'basic'
        ? { type: 'basic', username: c.username, password: c.password }
        : { type: 'anonymous' };
      configuration.topicFilters = c.topicFilters;
    } else if (basic.type === IntegrationType.TCP) {
      configuration.port = c.tcpPort;
      configuration.handlerType = c.handlerType;
      configuration.maxFrameLength = c.maxFrameLength;
      if (c.handlerType !== TcpHandlerType.BINARY) {
        configuration.charsetName = c.charsetName;
        configuration.delimiter = c.delimiter;
      }
    }
    const integration: Integration = {
      ...(this.integration || {} as Integration),
      name: basic.name,
      type: basic.type,
      enabled: basic.enabled,
      allowCreateDevicesOrAssets: basic.allowCreateDevicesOrAssets,
      debugMode: basic.debugMode,
      defaultConverterId: { entityType: 'CONVERTER', id: conv.defaultConverterId } as any,
      downlinkConverterId: conv.downlinkConverterId ? { entityType: 'CONVERTER', id: conv.downlinkConverterId } as any : null,
      configuration
    };
    this.integrationService.saveIntegration(integration).subscribe(() => this.dialogRef.close(true));
  }

}
