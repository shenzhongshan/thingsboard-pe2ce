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
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationService } from '@core/http/integration.service';
import { ConverterService } from '@core/http/converter.service';
import { PageLink } from '@shared/models/page/page-link';
import {
  Converter,
  ConverterScriptLanguage,
  ConverterType,
  DEFAULT_JS_DECODER,
  DEFAULT_JS_ENCODER,
  DEFAULT_TBEL_DECODER,
  DEFAULT_TBEL_ENCODER
} from '@shared/models/converter.models';
import { ConverterTestDialogComponent } from '@home/pages/integration/converter/converter-test-dialog.component';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import {
  HTTP_WEBHOOK_INTEGRATION_TYPES,
  Integration,
  integrationHttpEndpointUrl,
  IntegrationType,
  integrationTypeInfoMap,
  KinesisInitialPosition,
  MqttCredentialsType,
  MQTT_FAMILY_INTEGRATION_TYPES,
  newIntegration,
  OpcUaIdentityType,
  OpcUaSecurityPolicy,
  PulsarCredentialsType,
  TcpHandlerType,
  TuyaEnv,
  TuyaRegion
} from '@shared/models/integration.models';
import { EntityType } from '@shared/models/entity-type.models';

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
  MqttCredentialsType = MqttCredentialsType;
  PulsarCredentialsType = PulsarCredentialsType;
  TuyaRegion = TuyaRegion;
  TuyaEnv = TuyaEnv;
  OpcUaSecurityPolicy = OpcUaSecurityPolicy;
  OpcUaIdentityType = OpcUaIdentityType;
  KinesisInitialPosition = KinesisInitialPosition;
  MQTT_FAMILY_INTEGRATION_TYPES = MQTT_FAMILY_INTEGRATION_TYPES;
  HTTP_WEBHOOK_INTEGRATION_TYPES = HTTP_WEBHOOK_INTEGRATION_TYPES;
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
          name: [existingUplink?.name || this.defaultConverterName(integration.type, true), [Validators.required]],
          debugSettings: [existingUplink?.debugSettings || { failuresEnabled: true, allEnabled: false, allEnabledUntil: 0 }],
          scriptLang: [existingUplink?.configuration?.scriptLang || ConverterScriptLanguage.TBEL],
          script: [existingUplink?.configuration?.decoder || DEFAULT_TBEL_DECODER, [Validators.required]],
          updateOnlyKeys: [existingUplink?.configuration?.updateOnlyKeys || []]
        }),
        downlinkConverter: this.fb.group({
          create: [!!existingDownlink],
          name: [existingDownlink?.name || this.defaultConverterName(integration.type, false), [Validators.required]],
          debugSettings: [existingDownlink?.debugSettings || { failuresEnabled: true, allEnabled: false, allEnabledUntil: 0 }],
          scriptLang: [existingDownlink?.configuration?.scriptLang || ConverterScriptLanguage.TBEL],
          script: [existingDownlink?.configuration?.encoder || DEFAULT_TBEL_ENCODER, [Validators.required]],
          updateOnlyKeys: [existingDownlink?.configuration?.updateOnlyKeys || []]
        })
      }),
      connection: this.fb.group({
        // MQTT family (MQTT, AWS_IOT, AZURE_IOT_HUB, TTN, TTI)
        host: [cfg.host || '', MQTT_FAMILY_INTEGRATION_TYPES.includes(integration.type) ? [Validators.required] : []],
        port: [cfg.port || 1883],
        customHost: [cfg.customHost === true],
        clientId: [cfg.clientId || ''],
        credentialsType: [cfg.credentials?.type || MqttCredentialsType.ANONYMOUS],
        username: [cfg.credentials?.username || ''],
        password: [cfg.credentials?.password || ''],
        caCert: [cfg.credentials?.caCert || ''],
        cert: [cfg.credentials?.cert || ''],
        privateKey: [cfg.credentials?.privateKey || ''],
        privateKeyPassword: [cfg.credentials?.privateKeyPassword || ''],
        sasKey: [cfg.credentials?.sasKey || ''],
        topicFilters: this.fb.array((cfg.topicFilters || []).map((tf: any) => this.buildTopicFilterRow(tf.filter, tf.qos))),
        downlinkTopic: [cfg.downlinkTopic || ''],
        downlinkTopicPattern: [cfg.downlinkTopicPattern || ''],
        // TCP / UDP
        tcpPort: [cfg.port || 10560, (integration.type === IntegrationType.TCP || integration.type === IntegrationType.UDP) ? [Validators.required] : []],
        handlerType: [cfg.handlerType || TcpHandlerType.TEXT],
        maxFrameLength: [cfg.maxFrameLength || 65535],
        charsetName: [cfg.charsetName || 'UTF-8'],
        delimiter: [cfg.delimiter || '\\n'],
        // CoAP
        coapPort: [cfg.port || 5683],
        resourceName: [cfg.resourceName || 'telemetry'],
        // OPC-UA
        opcHost: [cfg.host || '', integration.type === IntegrationType.OPC_UA ? [Validators.required] : []],
        opcPort: [cfg.port || 49320],
        endpoint: [cfg.endpoint || ''],
        applicationName: [cfg.applicationName || 'ThingsBoard OPC-UA Integration'],
        applicationUri: [cfg.applicationUri || 'urn:thingsboard:opcua:client'],
        scanPeriodInSeconds: [cfg.scanPeriodInSeconds || 1],
        security: [cfg.security || OpcUaSecurityPolicy.None],
        identityType: [cfg.identity?.type || OpcUaIdentityType.ANONYMOUS],
        identityUsername: [cfg.identity?.username || ''],
        identityPassword: [cfg.identity?.password || ''],
        mapping: this.fb.array((cfg.mapping || []).map((m: any) => this.buildOpcUaMappingRow(m))),
        // CUSTOM
        clazz: [cfg.clazz || '', integration.type === IntegrationType.CUSTOM ? [Validators.required] : []],
        // Kafka
        kafkaClientId: [cc.clientId || ''],
        kafkaGroupId: [cc.groupId || ''],
        kafkaTopics: [cc.topics || '', integration.type === IntegrationType.KAFKA ? [Validators.required] : []],
        kafkaBootstrapServers: [cc.bootstrapServers || 'YOUR_KAFKA_DOMAIN:9092'],
        kafkaAutoCreateTopics: [cc.autoCreateTopics === 'true'],
        kafkaPollInterval: [cc.pollInterval || 5000],
        kafkaOtherProperties: this.fb.array(Object.entries(cc.otherProperties || {}).map(([k, v]) => this.buildKvRow(k, v as string))),
        // RabbitMQ
        rmqExchangeName: [cc.exchangeName || ''],
        rmqHost: [cc.host || '', integration.type === IntegrationType.RABBITMQ ? [Validators.required] : []],
        rmqPort: [cc.port || 5672],
        rmqVirtualHost: [cc.virtualHost || '/'],
        rmqUsername: [cc.username || 'guest'],
        rmqPassword: [cc.password || 'guest'],
        rmqDownlinkTopic: [cc.downlinkTopic || '#'],
        rmqQueues: [cc.queues || ''],
        rmqConnectionTimeout: [cc.connectionTimeout || 30000],
        rmqHandshakeTimeout: [cc.handshakeTimeout || 10000],
        rmqDurable: [cc.durable !== false],
        rmqExclusive: [cc.exclusive === true],
        rmqAutoDelete: [cc.autoDelete === true],
        // Apache Pulsar / Tuya share the Pulsar client
        pulsarServiceUrl: [cc.serviceUrl || 'pulsar://localhost:6650'],
        pulsarTopics: [cc.topics || ''],
        pulsarSubscriptionName: [cc.subscriptionName || ''],
        pulsarMaxNumMessages: [cc.maxNumMessages || 100],
        pulsarMaxNumBytes: [cc.maxNumBytes || 10485760],
        pulsarTimeoutInMs: [cc.timeoutInMs || 5000],
        pulsarCredentialsType: [cc.credentials?.type || PulsarCredentialsType.ANONYMOUS],
        pulsarToken: [cc.credentials?.token || ''],
        // Tuya
        tuyaRegion: [cc.region || TuyaRegion.CN],
        tuyaAccessId: [cc.accessId || ''],
        tuyaAccessKey: [cc.accessKey || ''],
        tuyaEnv: [cc.env || TuyaEnv.PROD],
        // AWS SQS
        sqsQueueUrl: [sqs.queueUrl || ''],
        sqsRegion: [sqs.region || 'us-east-1'],
        sqsAccessKeyId: [sqs.accessKeyId || ''],
        sqsSecretAccessKey: [sqs.secretAccessKey || ''],
        sqsPollingPeriodSeconds: [sqs.pollingPeriodSeconds || 5],
        // AWS Kinesis
        kinesisStreamName: [cc.streamName || ''],
        kinesisRegion: [cc.region || 'us-east-1'],
        kinesisAccessKeyId: [cc.accessKeyId || ''],
        kinesisSecretAccessKey: [cc.secretAccessKey || ''],
        kinesisInitialPosition: [cc.initialPositionInStream || KinesisInitialPosition.LATEST],
        kinesisMaxRecords: [cc.maxRecords || 100],
        // Azure Event Hub
        aehConnectionString: [cc.connectionString || ''],
        aehConsumerGroup: [cc.consumerGroup || '$Default'],
        // Azure Service Bus
        asbConnectionString: [cc.connectionString || ''],
        asbTopicName: [cc.topicName || ''],
        asbSubName: [cc.subName || ''],
        // GCP Pub/Sub
        pubsubProjectId: [cc.projectId || ''],
        pubsubSubscriptionId: [cc.subscriptionId || ''],
        pubsubServiceAccountKey: [cc.serviceAccountKey || '']
      })
    });
    this.basicGroup.get('type').valueChanges.subscribe((type) => {
      const hostCtrl = this.connectionGroup.get('host');
      hostCtrl.setValidators(MQTT_FAMILY_INTEGRATION_TYPES.includes(type) ? [Validators.required] : []);
      hostCtrl.updateValueAndValidity();
      const tcpPortCtrl = this.connectionGroup.get('tcpPort');
      tcpPortCtrl.setValidators(type === IntegrationType.TCP || type === IntegrationType.UDP ? [Validators.required] : []);
      tcpPortCtrl.updateValueAndValidity();
      const opcHostCtrl = this.connectionGroup.get('opcHost');
      opcHostCtrl.setValidators(type === IntegrationType.OPC_UA ? [Validators.required] : []);
      opcHostCtrl.updateValueAndValidity();
      const clazzCtrl = this.connectionGroup.get('clazz');
      clazzCtrl.setValidators(type === IntegrationType.CUSTOM ? [Validators.required] : []);
      clazzCtrl.updateValueAndValidity();
      const kafkaTopicsCtrl = this.connectionGroup.get('kafkaTopics');
      kafkaTopicsCtrl.setValidators(type === IntegrationType.KAFKA ? [Validators.required] : []);
      kafkaTopicsCtrl.updateValueAndValidity();
      const rmqHostCtrl = this.connectionGroup.get('rmqHost');
      rmqHostCtrl.setValidators(type === IntegrationType.RABBITMQ ? [Validators.required] : []);
      rmqHostCtrl.updateValueAndValidity();
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
    const downlinkCreateCtrl = this.downlinkConverterGroup.get('create');
    const toggleDownlinkValidators = (create: boolean) => {
      const nameCtrl = this.downlinkConverterGroup.get('name');
      const scriptCtrl = this.downlinkConverterGroup.get('script');
      nameCtrl.setValidators(create ? [Validators.required] : []);
      scriptCtrl.setValidators(create ? [Validators.required] : []);
      nameCtrl.updateValueAndValidity();
      scriptCtrl.updateValueAndValidity();
    };
    toggleDownlinkValidators(downlinkCreateCtrl.value);
    downlinkCreateCtrl.valueChanges.subscribe((create) => toggleDownlinkValidators(create));
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

  testUplinkConverter(): void {
    this.dialog.open(ConverterTestDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog'],
      data: {
        type: ConverterType.UPLINK,
        script: this.uplinkConverterGroup.get('script').value,
        scriptLang: this.uplinkConverterGroup.get('scriptLang').value
      }
    });
  }

  testDownlinkConverter(): void {
    this.dialog.open(ConverterTestDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog'],
      data: {
        type: ConverterType.DOWNLINK,
        script: this.downlinkConverterGroup.get('script').value,
        scriptLang: this.downlinkConverterGroup.get('scriptLang').value
      }
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

  // Generic key/value row, reused for Kafka "other properties".
  private buildKvRow(key: string, value: string): UntypedFormGroup {
    return this.fb.group({ key: [key, [Validators.required]], value: [value] });
  }

  get kafkaOtherPropertiesArray(): UntypedFormArray {
    return this.connectionGroup.get('kafkaOtherProperties') as UntypedFormArray;
  }

  addKafkaOtherProperty() {
    this.kafkaOtherPropertiesArray.push(this.buildKvRow('', ''));
  }

  removeKafkaOtherProperty(i: number) {
    this.kafkaOtherPropertiesArray.removeAt(i);
  }

  private buildSubscriptionTagRow(key: string, nodeId: string, required: boolean): UntypedFormGroup {
    return this.fb.group({ key: [key, [Validators.required]], nodeId: [nodeId, [Validators.required]], required: [required === true] });
  }

  private buildOpcUaMappingRow(m: any): UntypedFormGroup {
    return this.fb.group({
      deviceName: [m?.deviceName || '', [Validators.required]],
      subscriptionTags: this.fb.array((m?.subscriptionTags || []).map((t: any) => this.buildSubscriptionTagRow(t.key, t.nodeId, t.required)))
    });
  }

  get opcUaMappingArray(): UntypedFormArray {
    return this.connectionGroup.get('mapping') as UntypedFormArray;
  }

  addOpcUaMapping() {
    this.opcUaMappingArray.push(this.buildOpcUaMappingRow(null));
  }

  removeOpcUaMapping(i: number) {
    this.opcUaMappingArray.removeAt(i);
  }

  opcUaTagsArray(mappingIndex: number): UntypedFormArray {
    return this.opcUaMappingArray.at(mappingIndex).get('subscriptionTags') as UntypedFormArray;
  }

  addOpcUaTag(mappingIndex: number) {
    this.opcUaTagsArray(mappingIndex).push(this.buildSubscriptionTagRow('', '', false));
  }

  removeOpcUaTag(mappingIndex: number, tagIndex: number) {
    this.opcUaTagsArray(mappingIndex).removeAt(tagIndex);
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

  get isHttpWebhookType(): boolean {
    return HTTP_WEBHOOK_INTEGRATION_TYPES.includes(this.selectedType);
  }

  get isMqttFamilyType(): boolean {
    return MQTT_FAMILY_INTEGRATION_TYPES.includes(this.selectedType);
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
    const uplink = this.uplinkConverterGroup.value;
    const downlink = this.downlinkConverterGroup.value;
    const c = this.connectionGroup.value;
    const configuration: any = this.buildConfiguration(basic.type, c);

    const uplinkConverter: Converter = {
      ...(this.integration?.defaultConverterId ? { id: this.integration.defaultConverterId } : {}),
      name: uplink.name,
      type: ConverterType.UPLINK,
      integrationType: basic.type,
      debugSettings: uplink.debugSettings,
      configuration: { scriptLang: uplink.scriptLang, decoder: uplink.script, updateOnlyKeys: uplink.updateOnlyKeys || [] }
    } as Converter;

    this.converterService.saveConverter(uplinkConverter).pipe(
      switchMap((savedUplink) => {
        if (downlink.create) {
          const downlinkConverter: Converter = {
            ...(this.integration?.downlinkConverterId ? { id: this.integration.downlinkConverterId } : {}),
            name: downlink.name,
            type: ConverterType.DOWNLINK,
            integrationType: basic.type,
            debugSettings: downlink.debugSettings,
            configuration: { scriptLang: downlink.scriptLang, encoder: downlink.script, updateOnlyKeys: downlink.updateOnlyKeys || [] }
          } as Converter;
          return this.converterService.saveConverter(downlinkConverter).pipe(
            map((savedDownlink) => ({ savedUplink, savedDownlink }))
          );
        }
        return of({ savedUplink, savedDownlink: null });
      }),
      switchMap(({ savedUplink, savedDownlink }) => {
        const integration: Integration = {
          ...(this.integration || {} as Integration),
          name: basic.name,
          type: basic.type,
          enabled: basic.enabled,
          allowCreateDevicesOrAssets: basic.allowCreateDevicesOrAssets,
          debugSettings: basic.debugSettings,
          defaultConverterId: savedUplink.id,
          downlinkConverterId: savedDownlink ? savedDownlink.id : null,
          configuration
        };
        return this.integrationService.saveIntegration(integration);
      })
    ).subscribe(() => this.dialogRef.close(true));
  }

  private buildConfiguration(type: IntegrationType, c: any): any {
    const configuration: any = {};
    if (MQTT_FAMILY_INTEGRATION_TYPES.includes(type)) {
      configuration.host = c.host;
      configuration.port = c.port;
      configuration.customHost = c.customHost;
      if (c.clientId) {
        configuration.clientId = c.clientId;
      }
      configuration.credentials = this.buildMqttCredentials(c);
      configuration.topicFilters = c.topicFilters;
      if (c.downlinkTopic) {
        configuration.downlinkTopic = c.downlinkTopic;
      }
      if ((type === IntegrationType.TTN || type === IntegrationType.TTI) && c.downlinkTopicPattern) {
        configuration.downlinkTopicPattern = c.downlinkTopicPattern;
      }
    } else if (type === IntegrationType.TCP || type === IntegrationType.UDP) {
      configuration.port = c.tcpPort;
      configuration.handlerType = c.handlerType;
      configuration.maxFrameLength = c.maxFrameLength;
      if (c.handlerType !== TcpHandlerType.BINARY) {
        configuration.charsetName = c.charsetName;
        configuration.delimiter = c.delimiter;
      }
    } else if (type === IntegrationType.COAP) {
      configuration.port = c.coapPort;
      configuration.resourceName = c.resourceName;
    } else if (type === IntegrationType.OPC_UA) {
      configuration.host = c.opcHost;
      configuration.port = c.opcPort;
      configuration.endpoint = c.endpoint;
      configuration.applicationName = c.applicationName;
      configuration.applicationUri = c.applicationUri;
      configuration.scanPeriodInSeconds = c.scanPeriodInSeconds;
      configuration.security = c.security;
      configuration.identity = c.identityType === OpcUaIdentityType.USERNAME
        ? { type: OpcUaIdentityType.USERNAME, username: c.identityUsername, password: c.identityPassword }
        : { type: OpcUaIdentityType.ANONYMOUS };
      configuration.mapping = c.mapping;
    } else if (type === IntegrationType.CUSTOM) {
      configuration.clazz = c.clazz;
    } else if (type === IntegrationType.KAFKA) {
      configuration.clientConfiguration = {
        clientId: c.kafkaClientId, groupId: c.kafkaGroupId, topics: c.kafkaTopics,
        bootstrapServers: c.kafkaBootstrapServers, autoCreateTopics: String(c.kafkaAutoCreateTopics),
        pollInterval: c.kafkaPollInterval,
        otherProperties: Object.fromEntries((c.kafkaOtherProperties || []).map((kv: any) => [kv.key, kv.value]))
      };
    } else if (type === IntegrationType.RABBITMQ) {
      configuration.clientConfiguration = {
        exchangeName: c.rmqExchangeName, host: c.rmqHost, port: c.rmqPort, virtualHost: c.rmqVirtualHost,
        username: c.rmqUsername, password: c.rmqPassword, downlinkTopic: c.rmqDownlinkTopic, queues: c.rmqQueues,
        connectionTimeout: c.rmqConnectionTimeout, handshakeTimeout: c.rmqHandshakeTimeout,
        durable: c.rmqDurable, exclusive: c.rmqExclusive, autoDelete: c.rmqAutoDelete
      };
    } else if (type === IntegrationType.APACHE_PULSAR) {
      configuration.clientConfiguration = {
        serviceUrl: c.pulsarServiceUrl, topics: c.pulsarTopics, subscriptionName: c.pulsarSubscriptionName,
        maxNumMessages: c.pulsarMaxNumMessages, maxNumBytes: c.pulsarMaxNumBytes, timeoutInMs: c.pulsarTimeoutInMs,
        credentials: c.pulsarCredentialsType === PulsarCredentialsType.TOKEN
          ? { type: PulsarCredentialsType.TOKEN, token: c.pulsarToken } : { type: PulsarCredentialsType.ANONYMOUS }
      };
    } else if (type === IntegrationType.TUYA) {
      configuration.clientConfiguration = {
        region: c.tuyaRegion, accessId: c.tuyaAccessId, accessKey: c.tuyaAccessKey, env: c.tuyaEnv
      };
    } else if (type === IntegrationType.AWS_SQS) {
      configuration.sqsConfiguration = {
        queueUrl: c.sqsQueueUrl, region: c.sqsRegion, accessKeyId: c.sqsAccessKeyId,
        secretAccessKey: c.sqsSecretAccessKey, pollingPeriodSeconds: c.sqsPollingPeriodSeconds
      };
    } else if (type === IntegrationType.AWS_KINESIS) {
      configuration.clientConfiguration = {
        streamName: c.kinesisStreamName, region: c.kinesisRegion, accessKeyId: c.kinesisAccessKeyId,
        secretAccessKey: c.kinesisSecretAccessKey, initialPositionInStream: c.kinesisInitialPosition,
        maxRecords: c.kinesisMaxRecords
      };
    } else if (type === IntegrationType.AZURE_EVENT_HUB) {
      configuration.clientConfiguration = { connectionString: c.aehConnectionString, consumerGroup: c.aehConsumerGroup };
    } else if (type === IntegrationType.AZURE_SERVICE_BUS) {
      configuration.clientConfiguration = { connectionString: c.asbConnectionString, topicName: c.asbTopicName, subName: c.asbSubName };
    } else if (type === IntegrationType.PUB_SUB) {
      configuration.clientConfiguration = {
        projectId: c.pubsubProjectId, subscriptionId: c.pubsubSubscriptionId, serviceAccountKey: c.pubsubServiceAccountKey
      };
    }
    // HTTP + LPWAN webhook types (SIGFOX/OCEANCONNECT/CHIRPSTACK/PARTICLE/KPN/LORIOT/THINGPARK/TPE/TMOBILE_IOT_CDP):
    // no extra config in this port — the routing key is the only thing that identifies them.
    return configuration;
  }

  private buildMqttCredentials(c: any): any {
    switch (c.credentialsType) {
      case MqttCredentialsType.BASIC:
        return { type: MqttCredentialsType.BASIC, username: c.username, password: c.password };
      case MqttCredentialsType.CERT_PEM:
        return { type: MqttCredentialsType.CERT_PEM, caCert: c.caCert, cert: c.cert, privateKey: c.privateKey, privateKeyPassword: c.privateKeyPassword };
      case MqttCredentialsType.SAS:
        return { type: MqttCredentialsType.SAS, sasKey: c.sasKey, caCert: c.caCert };
      default:
        return { type: MqttCredentialsType.ANONYMOUS };
    }
  }

}
