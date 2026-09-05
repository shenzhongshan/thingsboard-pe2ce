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

import { Component, Input } from '@angular/core';
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  HTTP_HEADER_SECURITY_TYPES,
  HTTP_REPLACE_NO_CONTENT_TYPES,
  HTTP_WEBHOOK_INTEGRATION_TYPES,
  Integration,
  coapBaseUrlOf,
  CoapSecurityMode,
  coapIntegrationEndpointUrl,
  defaultCoapBaseUrl,
  integrationHttpEndpointUrl,
  IntegrationType,
  KinesisInitialPosition,
  LoriotCredentialsType,
  loriotCredentialsTypeLabels,
  MQTT_FAMILY_INTEGRATION_TYPES,
  MqttCredentialsType,
  MqttProtocolVersion,
  mqttProtocolVersionLabels,
  OpcUaIdentityType,
  OpcUaSecurityPolicy,
  PulsarCredentialsType,
  TcpByteOrder,
  TcpHandlerType,
  TcpMessageSeparator,
  THINGPARK_INTEGRATION_TYPES,
  UdpHandlerType,
  TuyaEnv,
  TuyaRegion
} from '@shared/models/integration.models';

/**
 * The whole "connection" surface for every integration type, in one place.
 *
 * PE renders the same per-type form component (`tb-mqtt-integration-form`, `tb-tcp-integration-form`,
 * ...) in both the add-integration wizard and the details drawer. CE previously inlined the markup in
 * the wizard only, so the drawer could show — and edit — nothing but the MQTT family: the other 24
 * types had no connection fields at all once created. Extracting it here is what lets both hosts
 * render all 29.
 *
 * The component also owns form construction ({@link buildConnectionGroup}) and serialization
 * ({@link buildConfiguration}) as statics, so the two hosts cannot drift into disagreeing about the
 * wire shape — the failure mode that would otherwise silently drop fields on save from one host only.
 */
@Component({
  selector: 'tb-integration-connection-form',
  templateUrl: './integration-connection-form.component.html',
  styleUrls: ['./integration-connection-form.component.scss'],
  standalone: false
})
export class IntegrationConnectionFormComponent {

  @Input() connectionForm: UntypedFormGroup;
  @Input() type: IntegrationType;
  /** Both are minted client-side at create time (PE parity), so they are always available. */
  @Input() routingKey: string;
  @Input() secret: string;
  @Input() checkResult: {success: boolean; error?: string} = null;

  IntegrationType = IntegrationType;
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
  LoriotCredentialsType = LoriotCredentialsType;
  loriotCredentialsTypeLabels = loriotCredentialsTypeLabels;
  tcpHandlerTypes = Object.values(TcpHandlerType);
  /** PE's HandlerConfigurationType — a different enum from TCP's, and it includes HEX. */
  udpHandlerTypes = Object.values(UdpHandlerType);

  constructor(protected store: Store<AppState>,
              private translate: TranslateService) {
  }

  get isHttpWebhookType(): boolean {
    return HTTP_WEBHOOK_INTEGRATION_TYPES.includes(this.type);
  }

  get isMqttFamilyType(): boolean {
    return MQTT_FAMILY_INTEGRATION_TYPES.includes(this.type);
  }

  /** PE shows the header allow-list panel only for the types whose backend actually honours it. */
  get hasHeaderSecurityPanel(): boolean {
    return HTTP_HEADER_SECURITY_TYPES.includes(this.type);
  }

  get hasReplaceNoContentToggle(): boolean {
    return HTTP_REPLACE_NO_CONTENT_TYPES.includes(this.type);
  }

  get isThingParkType(): boolean {
    return THINGPARK_INTEGRATION_TYPES.includes(this.type);
  }

  coapSecurityModes = Object.values(CoapSecurityMode);

  /**
   * True for integrations persisted in CE's original {port, resourceName} shape. They keep the old
   * editor so an existing, working CoAP integration is never silently re-pointed at the shared
   * server (which would change the URL its devices post to).
   */
  get coapLegacyMode(): boolean {
    return this.connectionForm?.get('coapLegacy')?.value === true;
  }

  get coapShowNoSecEndpoint(): boolean {
    const mode = this.connectionForm?.get('coapSecurityMode')?.value;
    return mode === CoapSecurityMode.NO_SECURE || mode === CoapSecurityMode.MIXED;
  }

  get coapShowDtlsEndpoint(): boolean {
    const mode = this.connectionForm?.get('coapSecurityMode')?.value;
    return mode === CoapSecurityMode.DTLS || mode === CoapSecurityMode.MIXED;
  }

  get coapEndpointUrl(): string {
    return coapIntegrationEndpointUrl(this.routingKey, this.connectionForm?.get('coapBaseUrl')?.value);
  }

  get coapDtlsEndpointUrl(): string {
    return coapIntegrationEndpointUrl(this.routingKey, this.connectionForm?.get('coapDtlsBaseUrl')?.value);
  }

  /** PE groups these two behind one 允许下行 panel. */
  get hasAllowDownlinkPanel(): boolean {
    return this.type === IntegrationType.PARTICLE || this.type === IntegrationType.KPN;
  }

  /** Live-derived: PE recomputes it as the user edits baseUrl, and the key exists from create time. */
  get httpEndpointUrl(): string {
    return this.routingKey
      ? integrationHttpEndpointUrl(this.routingKey, this.type, this.connectionForm?.get('baseUrl')?.value)
      : null;
  }

  get topicFiltersArray(): UntypedFormArray {
    return this.connectionForm.get('topicFilters') as UntypedFormArray;
  }

  addTopicFilter() {
    this.topicFiltersArray.push(IntegrationConnectionFormComponent.topicFilterRow(this.fb(), '#', 0));
  }

  removeTopicFilter(i: number) {
    this.topicFiltersArray.removeAt(i);
  }

  get kafkaOtherPropertiesArray(): UntypedFormArray {
    return this.connectionForm.get('kafkaOtherProperties') as UntypedFormArray;
  }

  addKafkaOtherProperty() {
    this.kafkaOtherPropertiesArray.push(IntegrationConnectionFormComponent.kvRow(this.fb(), '', ''));
  }

  removeKafkaOtherProperty(i: number) {
    this.kafkaOtherPropertiesArray.removeAt(i);
  }

  get opcUaMappingArray(): UntypedFormArray {
    return this.connectionForm.get('mapping') as UntypedFormArray;
  }

  addOpcUaMapping() {
    this.opcUaMappingArray.push(IntegrationConnectionFormComponent.opcUaMappingRow(this.fb(), null));
  }

  removeOpcUaMapping(i: number) {
    this.opcUaMappingArray.removeAt(i);
  }

  opcUaTagsArray(mappingIndex: number): UntypedFormArray {
    return this.opcUaMappingArray.at(mappingIndex).get('subscriptionTags') as UntypedFormArray;
  }

  addOpcUaTag(mappingIndex: number) {
    this.opcUaTagsArray(mappingIndex).push(IntegrationConnectionFormComponent.subscriptionTagRow(this.fb(), '', '', false));
  }

  removeOpcUaTag(mappingIndex: number, tagIndex: number) {
    this.opcUaTagsArray(mappingIndex).removeAt(tagIndex);
  }

  /** The arrays are built by the static factory, so reuse whatever builder made this group. */
  private fb(): UntypedFormBuilder {
    return new UntypedFormBuilder();
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

  // ---------------------------------------------------------------------------------------------
  // Form construction + serialization. Static so the wizard and the details drawer share one
  // definition of the wire shape.
  // ---------------------------------------------------------------------------------------------

  static topicFilterRow(fb: UntypedFormBuilder, filter: string, qos: number): UntypedFormGroup {
    return fb.group({ filter: [filter, [Validators.required]], qos: [qos ?? 0] });
  }

  static kvRow(fb: UntypedFormBuilder, key: string, value: string): UntypedFormGroup {
    return fb.group({ key: [key, [Validators.required]], value: [value] });
  }

  static subscriptionTagRow(fb: UntypedFormBuilder, key: string, nodeId: string, required: boolean): UntypedFormGroup {
    return fb.group({
      key: [key, [Validators.required]],
      nodeId: [nodeId, [Validators.required]],
      required: [required === true]
    });
  }

  static opcUaMappingRow(fb: UntypedFormBuilder, m: any): UntypedFormGroup {
    return fb.group({
      deviceName: [m?.deviceName || '', [Validators.required]],
      subscriptionTags: fb.array((m?.subscriptionTags || [])
        .map((t: any) => IntegrationConnectionFormComponent.subscriptionTagRow(fb, t.key, t.nodeId ?? t.path, t.required)))
    });
  }

  static buildConnectionGroup(fb: UntypedFormBuilder, integration: Integration): UntypedFormGroup {
    const cfg = integration.configuration || {};
    const cc = cfg.clientConfiguration || {};
    const sqs = cfg.sqsConfiguration || {};
    const type = integration.type;
    return fb.group({
      // Applies to every type: `remote` moves execution to a separate process that connects back
      // over gRPC with this integration's routing key and secret.
      remote: [integration.remote === true],
      // PE's genericAdditionalInfoTemplate — shown inside every type's advanced-settings panel.
      description: [integration.additionalInfo?.description || ''],
      metadata: [cfg.metadata || {}],
      // MQTT family (MQTT, AWS_IOT, AZURE_IOT_HUB, TTN, TTI) — nested under clientConfiguration,
      // matching PE's wire shape (MqttClientConfiguration.java) exactly.
      host: [cc.host || '', MQTT_FAMILY_INTEGRATION_TYPES.includes(type) ? [Validators.required] : []],
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
      topicFilters: fb.array((cfg.topicFilters?.length ? cfg.topicFilters : [{ filter: '#', qos: 0 }])
        .map((tf: any) => IntegrationConnectionFormComponent.topicFilterRow(fb, tf.filter, tf.qos))),
      // Legacy CE field: no longer edited (PE has no such field), but preserved on save so
      // integrations already configured with a fixed downlink topic keep working.
      downlinkTopic: [cfg.downlinkTopic || ''],
      downlinkTopicPattern: [cfg.downlinkTopicPattern || '${topic}'],
      // TCP / UDP
      // PE uses distinct defaults per type: 10560 for TCP, 11560 for UDP.
      tcpPort: [cfg.port || (type === IntegrationType.UDP ? 11560 : 10560), (type === IntegrationType.TCP || type === IntegrationType.UDP) ? [Validators.required] : []],
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
      // UDP only (TCP has no broadcast concept); honoured by UdpIntegrationServer.
      // PE defaults broadcast ON for UDP (tb-udp-integration-form), unlike TCP which has no such option.
      soBroadcast: [cfg.soBroadcast !== false],
      udpHandlerType: [cfg.handlerConfiguration?.handlerType || UdpHandlerType.BINARY],
      udpCharsetName: [cfg.handlerConfiguration?.charsetName || 'UTF-8'],
      udpMaxFrameLength: [cfg.handlerConfiguration?.maxFrameLength ?? 128],
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
      // CoAP — shared-server (PE) fields plus the legacy standalone-port pair.
      coapLegacy: [type === IntegrationType.COAP && !cc.coapEndpoint && !cc.dtlsCoapEndpoint && cfg.port != null],
      coapSecurityMode: [cc.securityMode || CoapSecurityMode.NO_SECURE],
      coapBaseUrl: [coapBaseUrlOf(cc.coapEndpoint) || defaultCoapBaseUrl(false)],
      coapDtlsBaseUrl: [coapBaseUrlOf(cc.dtlsCoapEndpoint) || defaultCoapBaseUrl(true)],
      coapPort: [cfg.port || 5683],
      resourceName: [cfg.resourceName || 'telemetry'],
      // OPC-UA
      opcHost: [cfg.host || '', type === IntegrationType.OPC_UA ? [Validators.required] : []],
      opcPort: [cfg.port || 49320],
      endpoint: [cfg.endpoint || ''],
      applicationName: [cfg.applicationName || 'ThingsBoard OPC-UA Integration'],
      applicationUri: [cfg.applicationUri || 'urn:thingsboard:opcua:client'],
      scanPeriodInSeconds: [cfg.scanPeriodInSeconds || 1],
      security: [cfg.security || OpcUaSecurityPolicy.None],
      identityType: [cfg.identity?.type || OpcUaIdentityType.ANONYMOUS],
      identityUsername: [cfg.identity?.username || ''],
      identityPassword: [cfg.identity?.password || ''],
      mapping: fb.array((cfg.mapping || []).map((m: any) => IntegrationConnectionFormComponent.opcUaMappingRow(fb, m))),
      // CUSTOM
      clazz: [cfg.clazz || '', type === IntegrationType.CUSTOM ? [Validators.required] : []],
      // Kafka
      kafkaClientId: [cc.clientId || ''],
      kafkaGroupId: [cc.groupId || ''],
      kafkaTopics: [cc.topics || '', type === IntegrationType.KAFKA ? [Validators.required] : []],
      kafkaBootstrapServers: [cc.bootstrapServers || 'YOUR_KAFKA_DOMAIN:9092'],
      kafkaAutoCreateTopics: [cc.autoCreateTopics === 'true'],
      kafkaPollInterval: [cc.pollInterval || 5000],
      kafkaOtherProperties: fb.array(Object.entries(cc.otherProperties || {})
        .map(([k, v]) => IntegrationConnectionFormComponent.kvRow(fb, k, v as string))),
      // RabbitMQ
      rmqExchangeName: [cc.exchangeName || ''],
      rmqHost: [cc.host || '', type === IntegrationType.RABBITMQ ? [Validators.required] : []],
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
      kinesisUseInstanceMetadata: [cc.useCredentialsFromInstanceMetadata === true],
      kinesisRequestTimeout: [cc.requestTimeout ?? 0],
      // Azure Event Hub
      aehConnectionString: [cc.connectionString || ''],
      aehIotHubName: [cc.iotHubName || ''],
      aehConsumerGroup: [cc.consumerGroup || '$Default'],
      aehConnectTimeoutSec: [cc.connectTimeoutSec ?? 10],
      aehPersistentCheckpoints: [cc.enablePersistentCheckpoints === true],
      aehStorageConnectionString: [cc.storageConnectionString || ''],
      aehContainerName: [cc.containerName || ''],
      // Azure Service Bus
      asbConnectionString: [cc.connectionString || ''],
      asbTopicName: [cc.topicName || ''],
      asbSubName: [cc.subName || ''],
      asbDownlinkConnectionString: [cc.downlinkConnectionString || ''],
      asbDownlinkTopicName: [cc.downlinkTopicName || ''],
      // GCP Pub/Sub
      pubsubProjectId: [cc.projectId || ''],
      pubsubSubscriptionId: [cc.subscriptionId || ''],
      pubsubServiceAccountKey: [cc.serviceAccountKey || ''],
      // ---- HTTP webhook family. All keys are top-level in PE's configuration JSON except
      // ChirpStack's application-server trio, which PE nests under clientConfiguration.
      baseUrl: [cfg.baseUrl || window.location.origin,
        HTTP_WEBHOOK_INTEGRATION_TYPES.includes(type) ? [Validators.required] : []],
      enableSecurity: [cfg.enableSecurity === true],
      headersFilter: [cfg.headersFilter || {}],
      replaceNoContentToOk: [cfg.replaceNoContentToOk === true],
      // ChirpStack
      chirpApplicationServerUrl: [cc.applicationServerUrl || ''],
      chirpApplicationServerAPIToken: [cc.applicationServerAPIToken || ''],
      // PE defaults this ON — ChirpStack 4 is the current major version.
      chirpUseAPI4Plus: [cc.useAPI4Plus !== false],
      // Particle
      allowDownlink: [cfg.allowDownlink === true],
      particleToken: [cfg.credentials?.token || ''],
      // KPN Things
      kpnDestinationSharedSecret: [cfg.destinationSharedSecret || ''],
      kpnGripTenantId: [cfg.gripTenantId || ''],
      kpnApiId: [cfg.apiId || ''],
      kpnApiKey: [cfg.apiKey || ''],
      // Loriot
      loriotCreateOutput: [cfg.createLoriotOutput === true],
      loriotSendDownlink: [cfg.sendDownlink === true],
      loriotServer: [cfg.server || 'eu1'],
      loriotDomain: [cfg.domain || 'loriot.io'],
      loriotAppId: [cfg.appId || ''],
      loriotCredentialsType: [cfg.credentials?.type || LoriotCredentialsType.BASIC],
      loriotUsername: [cfg.credentials?.username || ''],
      loriotPassword: [cfg.credentials?.password || ''],
      loriotToken: [cfg.credentials?.token || ''],
      // ThingPark / ThingParkEnterprise
      tpEnableSecurityNew: [cfg.enableSecurityNew === true],
      tpAsId: [cfg.asId || ''],
      tpAsIdNew: [cfg.asIdNew || ''],
      tpAsKey: [cfg.asKey || ''],
      tpClientIdNew: [cfg.clientIdNew || ''],
      tpClientSecret: [cfg.clientSecret || ''],
      tpMaxTimeDiffInSeconds: [cfg.maxTimeDiffInSeconds ?? 60],
      tpDownlinkUrl: [cfg.downlinkUrl || 'https://api.thingpark.com/thingpark/lrc/rest/downlink']
    });
  }

  /**
   * Re-points the per-type required validators after the user changes the integration type. Only
   * the wizard needs this — the drawer edits an integration whose type is already fixed.
   */
  static retargetValidators(group: UntypedFormGroup, type: IntegrationType) {
    const set = (name: string, required: boolean) => {
      const ctrl = group.get(name);
      ctrl.setValidators(required ? [Validators.required] : []);
      ctrl.updateValueAndValidity();
    };
    set('host', MQTT_FAMILY_INTEGRATION_TYPES.includes(type));
    set('tcpPort', type === IntegrationType.TCP || type === IntegrationType.UDP);
    set('opcHost', type === IntegrationType.OPC_UA);
    set('clazz', type === IntegrationType.CUSTOM);
    set('kafkaTopics', type === IntegrationType.KAFKA);
    set('rmqHost', type === IntegrationType.RABBITMQ);
    set('baseUrl', HTTP_WEBHOOK_INTEGRATION_TYPES.includes(type));
  }

  static buildConfiguration(type: IntegrationType, c: any, routingKey: string): any {
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
        credentials: IntegrationConnectionFormComponent.buildMqttCredentials(c)
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
      configuration.soRcvBuf = c.soRcvBuf;
      configuration.soBroadcast = c.soBroadcast;
      // Nested exactly as PE stores it; only the sub-field the chosen type actually uses is written.
      const handlerConfiguration: any = { handlerType: c.udpHandlerType };
      if (c.udpHandlerType === UdpHandlerType.TEXT) {
        handlerConfiguration.charsetName = c.udpCharsetName;
      } else if (c.udpHandlerType === UdpHandlerType.HEX) {
        handlerConfiguration.maxFrameLength = c.udpMaxFrameLength;
      }
      configuration.handlerConfiguration = handlerConfiguration;
    } else if (type === IntegrationType.COAP) {
      if (c.coapLegacy) {
        configuration.port = c.coapPort;
        configuration.resourceName = c.resourceName;
      } else {
        // PE's wire shape: endpoints are full URIs ending in the routing key, and the runtime
        // derives the resource path from them.
        const clientConfiguration: any = { securityMode: c.coapSecurityMode };
        if (c.coapSecurityMode === CoapSecurityMode.NO_SECURE || c.coapSecurityMode === CoapSecurityMode.MIXED) {
          clientConfiguration.coapEndpoint = coapIntegrationEndpointUrl(routingKey, c.coapBaseUrl);
        }
        if (c.coapSecurityMode === CoapSecurityMode.DTLS || c.coapSecurityMode === CoapSecurityMode.MIXED) {
          clientConfiguration.dtlsCoapEndpoint = coapIntegrationEndpointUrl(routingKey, c.coapDtlsBaseUrl);
        }
        configuration.clientConfiguration = clientConfiguration;
      }
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
        maxRecords: c.kinesisMaxRecords,
        useCredentialsFromInstanceMetadata: c.kinesisUseInstanceMetadata,
        requestTimeout: c.kinesisRequestTimeout
      };
    } else if (type === IntegrationType.AZURE_EVENT_HUB) {
      configuration.clientConfiguration = {
        connectionString: c.aehConnectionString, iotHubName: c.aehIotHubName,
        consumerGroup: c.aehConsumerGroup, connectTimeoutSec: c.aehConnectTimeoutSec,
        enablePersistentCheckpoints: c.aehPersistentCheckpoints,
        // Only persisted while the toggle is on, so a half-filled container name can never make
        // the runtime fail its required() check on the next restart.
        storageConnectionString: c.aehPersistentCheckpoints ? c.aehStorageConnectionString : '',
        containerName: c.aehPersistentCheckpoints ? c.aehContainerName : ''
      };
    } else if (type === IntegrationType.AZURE_SERVICE_BUS) {
      configuration.clientConfiguration = {
        connectionString: c.asbConnectionString, topicName: c.asbTopicName, subName: c.asbSubName,
        downlinkConnectionString: c.asbDownlinkConnectionString, downlinkTopicName: c.asbDownlinkTopicName
      };
    } else if (type === IntegrationType.PUB_SUB) {
      configuration.clientConfiguration = {
        projectId: c.pubsubProjectId, subscriptionId: c.pubsubSubscriptionId, serviceAccountKey: c.pubsubServiceAccountKey
      };
    } else if (HTTP_WEBHOOK_INTEGRATION_TYPES.includes(type)) {
      // PE keeps all of these top-level in `configuration` (BasicHttpIntegration reads json.get(...)
      // directly); only ChirpStack's application-server trio is nested under clientConfiguration.
      configuration.baseUrl = c.baseUrl;
      configuration.httpEndpoint = integrationHttpEndpointUrl(routingKey, type, c.baseUrl);
      if (HTTP_HEADER_SECURITY_TYPES.includes(type)) {
        configuration.enableSecurity = c.enableSecurity;
        // PE only persists the filter map while security is on, and ignores it otherwise.
        configuration.headersFilter = c.enableSecurity ? (c.headersFilter || {}) : {};
      }
      if (HTTP_REPLACE_NO_CONTENT_TYPES.includes(type)) {
        configuration.replaceNoContentToOk = c.replaceNoContentToOk;
      }
      if (type === IntegrationType.CHIRPSTACK) {
        configuration.clientConfiguration = {
          applicationServerUrl: c.chirpApplicationServerUrl,
          applicationServerAPIToken: c.chirpApplicationServerAPIToken,
          useAPI4Plus: c.chirpUseAPI4Plus
        };
      } else if (type === IntegrationType.PARTICLE) {
        configuration.allowDownlink = c.allowDownlink;
        configuration.credentials = { token: c.particleToken };
      } else if (type === IntegrationType.KPN) {
        configuration.destinationSharedSecret = c.kpnDestinationSharedSecret;
        configuration.allowDownlink = c.allowDownlink;
        configuration.gripTenantId = c.kpnGripTenantId;
        configuration.apiId = c.kpnApiId;
        configuration.apiKey = c.kpnApiKey;
      } else if (type === IntegrationType.LORIOT) {
        configuration.createLoriotOutput = c.loriotCreateOutput;
        configuration.sendDownlink = c.loriotSendDownlink;
        configuration.server = c.loriotServer;
        configuration.domain = c.loriotDomain;
        configuration.appId = c.loriotAppId;
        configuration.credentials = c.loriotCredentialsType === LoriotCredentialsType.TOKEN
          ? { type: LoriotCredentialsType.TOKEN, token: c.loriotToken }
          : { type: LoriotCredentialsType.BASIC, username: c.loriotUsername, password: c.loriotPassword };
      } else if (THINGPARK_INTEGRATION_TYPES.includes(type)) {
        configuration.enableSecurity = c.enableSecurity;
        configuration.enableSecurityNew = c.tpEnableSecurityNew;
        // PE swaps the AS ID key with the toggle: the auto-token flow reads asIdNew and adds a
        // client id/secret pair (ThingParkIntegrationEnterprise#init).
        if (c.tpEnableSecurityNew) {
          configuration.asIdNew = c.tpAsIdNew;
          configuration.clientIdNew = c.tpClientIdNew;
          configuration.clientSecret = c.tpClientSecret;
        } else {
          configuration.asId = c.tpAsId;
        }
        configuration.asKey = c.tpAsKey;
        configuration.maxTimeDiffInSeconds = c.tpMaxTimeDiffInSeconds;
        configuration.downlinkUrl = c.tpDownlinkUrl;
      }
    }
    // Free-form metadata lives alongside the type-specific config, same as PE.
    if (c.metadata && Object.keys(c.metadata).length) {
      configuration.metadata = c.metadata;
    }
    return configuration;
  }

  private static buildMqttCredentials(c: any): any {
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
