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
  KinesisInitialPosition,
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
      connection: this.fb.group({
        // Applies to every type: `remote` moves execution to a separate process that connects back
        // over gRPC with this integration's routing key and secret.
        remote: [integration.remote === true],
        // PE's genericAdditionalInfoTemplate — shown inside every type's advanced-settings panel.
        description: [integration.additionalInfo?.description || ''],
        metadata: [cfg.metadata || {}],
        // MQTT family (MQTT, AWS_IOT, AZURE_IOT_HUB, TTN, TTI) — nested under clientConfiguration,
        // matching PE's wire shape (MqttClientConfiguration.java) exactly.
        host: [cc.host || '', MQTT_FAMILY_INTEGRATION_TYPES.includes(integration.type) ? [Validators.required] : []],
        port: [cc.port || 1883],
        customHost: [cc.customHost === true],
        clientId: [cc.clientId || ''],
        ssl: [cc.ssl === true],
        protocolVersion: [cc.protocolVersion || MqttProtocolVersion.MQTT_3_1_1],
        cleanSession: [cc.cleanSession !== false],
        retainedMessage: [cc.retainedMessage === true],
        maxBytesInMessage: [cc.maxBytesInMessage || 32368, [Validators.min(1), Validators.max(256000000)]],
        connectTimeoutSec: [cc.connectTimeoutSec || 10, [Validators.required, Validators.min(1), Validators.max(200)]],
        credentialsType: [cc.credentials?.type || MqttCredentialsType.ANONYMOUS],
        username: [cc.credentials?.username || ''],
        password: [cc.credentials?.password || ''],
        caCert: [cc.credentials?.caCert || ''],
        cert: [cc.credentials?.cert || ''],
        privateKey: [cc.credentials?.privateKey || ''],
        privateKeyPassword: [cc.credentials?.privateKeyPassword || ''],
        sasKey: [cc.credentials?.sasKey || ''],
        // PE seeds one '#'/QoS 0 row so the 主题过滤器 section is never empty; match that.
        topicFilters: this.fb.array((cfg.topicFilters?.length ? cfg.topicFilters : [{ filter: '#', qos: 0 }])
          .map((tf: any) => this.buildTopicFilterRow(tf.filter, tf.qos))),
        // Legacy CE field: no longer edited (PE has no such field), but preserved on save so
        // integrations already configured with a fixed downlink topic keep working.
        downlinkTopic: [cfg.downlinkTopic || ''],
        downlinkTopicPattern: [cfg.downlinkTopicPattern || '${topic}'],
        // TCP / UDP
        tcpPort: [cfg.port || 10560, (integration.type === IntegrationType.TCP || integration.type === IntegrationType.UDP) ? [Validators.required] : []],
        handlerType: [cfg.handlerType || TcpHandlerType.TEXT],
        maxFrameLength: [cfg.maxFrameLength || 65535],
        charsetName: [cfg.charsetName || 'UTF-8'],
        delimiter: [cfg.delimiter || '\\n'],
        // TCP only — real Netty socket/framing tuning, matching PE's tb-tcp-integration-form field
        // names exactly. UDP does not use any of these (UdpIntegrationServer ignores them).
        soBacklogOption: [cfg.soBacklogOption ?? 128],
        soRcvBuf: [cfg.soRcvBuf ?? 64],
        soSndBuf: [cfg.soSndBuf ?? 64],
        soKeepaliveOption: [cfg.soKeepaliveOption !== false],
        tcpNoDelay: [cfg.tcpNoDelay === true],
        stripDelimiter: [cfg.stripDelimiter !== false],
        messageSeparator: [cfg.messageSeparator || TcpMessageSeparator.SYSTEM_LINE_SEPARATOR],
        customSeparatorRawValue: [cfg.customSeparatorRawValue || ''],
        lengthFieldOffset: [cfg.lengthFieldOffset ?? 0],
        lengthFieldLength: [cfg.lengthFieldLength ?? 2],
        lengthAdjustment: [cfg.lengthAdjustment ?? 0],
        initialBytesToStrip: [cfg.initialBytesToStrip ?? 0],
        byteOrder: [cfg.byteOrder || TcpByteOrder.BIG_ENDIAN],
        failFast: [cfg.failFast !== false],
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

  copyToClipboard(value: string, what: 'key' | 'secret') {
    if (!value) {
      return;
    }
    navigator.clipboard.writeText(value).then(() => {
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant(what === 'key'
          ? 'integration.integration-key-copied-message'
          : 'integration.integration-secret-copied-message'),
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
      configuration: this.buildConfiguration(basic.type, this.connectionGroup.value)
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

  private buildConfiguration(type: IntegrationType, c: any): any {
    const configuration: any = {};
    if (MQTT_FAMILY_INTEGRATION_TYPES.includes(type)) {
      // Nested under clientConfiguration to match PE's wire shape (MqttClientConfiguration.java);
      // topicFilters/downlinkTopicPattern stay top-level, same as PE.
      const clientConfiguration: any = {
        host: c.host,
        port: c.port,
        customHost: c.customHost,
        ssl: c.ssl,
        protocolVersion: c.protocolVersion,
        cleanSession: c.cleanSession,
        retainedMessage: c.retainedMessage,
        maxBytesInMessage: c.maxBytesInMessage,
        connectTimeoutSec: c.connectTimeoutSec,
        credentials: this.buildMqttCredentials(c)
      };
      if (c.clientId) {
        clientConfiguration.clientId = c.clientId;
      }
      configuration.clientConfiguration = clientConfiguration;
      configuration.topicFilters = c.topicFilters;
      if (c.downlinkTopic) {
        configuration.downlinkTopic = c.downlinkTopic;
      }
      if (c.downlinkTopicPattern) {
        configuration.downlinkTopicPattern = c.downlinkTopicPattern;
      }
    } else if (type === IntegrationType.TCP) {
      configuration.port = c.tcpPort;
      configuration.soBacklogOption = c.soBacklogOption;
      configuration.soRcvBuf = c.soRcvBuf;
      configuration.soSndBuf = c.soSndBuf;
      configuration.soKeepaliveOption = c.soKeepaliveOption;
      configuration.tcpNoDelay = c.tcpNoDelay;
      configuration.handlerType = c.handlerType;
      configuration.maxFrameLength = c.maxFrameLength;
      configuration.charsetName = c.charsetName;
      if (c.handlerType === TcpHandlerType.BINARY) {
        configuration.lengthFieldOffset = c.lengthFieldOffset;
        configuration.lengthFieldLength = c.lengthFieldLength;
        configuration.lengthAdjustment = c.lengthAdjustment;
        configuration.initialBytesToStrip = c.initialBytesToStrip;
        configuration.byteOrder = c.byteOrder;
        configuration.failFast = c.failFast;
      } else if (c.handlerType === TcpHandlerType.TEXT) {
        configuration.stripDelimiter = c.stripDelimiter;
        configuration.messageSeparator = c.messageSeparator;
        if (c.messageSeparator === TcpMessageSeparator.CUSTOM) {
          configuration.customSeparatorRawValue = c.customSeparatorRawValue;
        }
      }
    } else if (type === IntegrationType.UDP) {
      configuration.port = c.tcpPort;
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
    // Free-form metadata lives alongside the type-specific config, same as PE.
    if (c.metadata && Object.keys(c.metadata).length) {
      configuration.metadata = c.metadata;
    }
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
