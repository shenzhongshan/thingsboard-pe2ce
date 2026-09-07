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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityType } from '@shared/models/entity-type.models';
import { HasUUID } from '@shared/models/id/has-uuid';
import { ConverterId } from '@shared/models/converter.models';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { generateSecret, guid } from '@core/utils';

export class IntegrationId implements HasUUID {
  entityType = EntityType.INTEGRATION;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

// Full PE protocol list (29 types).
export enum IntegrationType {
  HTTP = 'HTTP',
  MQTT = 'MQTT',
  TCP = 'TCP',
  UDP = 'UDP',
  COAP = 'COAP',
  OPC_UA = 'OPC_UA',
  CUSTOM = 'CUSTOM',
  AWS_IOT = 'AWS_IOT',
  AZURE_IOT_HUB = 'AZURE_IOT_HUB',
  TTN = 'TTN',
  TTI = 'TTI',
  AWS_SQS = 'AWS_SQS',
  AWS_KINESIS = 'AWS_KINESIS',
  AZURE_EVENT_HUB = 'AZURE_EVENT_HUB',
  AZURE_SERVICE_BUS = 'AZURE_SERVICE_BUS',
  PUB_SUB = 'PUB_SUB',
  KAFKA = 'KAFKA',
  RABBITMQ = 'RABBITMQ',
  APACHE_PULSAR = 'APACHE_PULSAR',
  TUYA = 'TUYA',
  SIGFOX = 'SIGFOX',
  CHIRPSTACK = 'CHIRPSTACK',
  PARTICLE = 'PARTICLE',
  KPN = 'KPN',
  LORIOT = 'LORIOT',
  OCEANCONNECT = 'OCEANCONNECT',
  THINGPARK = 'THINGPARK',
  TPE = 'TPE',
  TMOBILE_IOT_CDP = 'TMOBILE_IOT_CDP'
}

// Integration types whose uplink is a plain HTTP webhook (routing-key URL) — HTTP itself plus
// every LPWAN network-server type in this port that needs no dedicated connection-step UI.
export const HTTP_WEBHOOK_INTEGRATION_TYPES: IntegrationType[] = [
  IntegrationType.HTTP, IntegrationType.SIGFOX, IntegrationType.CHIRPSTACK, IntegrationType.PARTICLE,
  IntegrationType.KPN, IntegrationType.LORIOT, IntegrationType.OCEANCONNECT, IntegrationType.THINGPARK,
  IntegrationType.TPE, IntegrationType.TMOBILE_IOT_CDP
];

// Integration types backed by MqttIntegrationClient (share the same connection-step fields).
export const MQTT_FAMILY_INTEGRATION_TYPES: IntegrationType[] = [
  IntegrationType.MQTT, IntegrationType.AWS_IOT, IntegrationType.AZURE_IOT_HUB,
  IntegrationType.TTN, IntegrationType.TTI
];

/**
 * URL segment each webhook type is served on, mirroring PE's one-controller-per-type layout
 * (HttpIntegrationController -> /http, SigFoxIntegrationController -> /sigfox, ...). These end up
 * in the endpoint URL the user pastes into a third-party network server, so they are contract, not
 * cosmetics — note `tpe` and `tmobile_iot_cdp` do not match their enum names.
 * Kept in lockstep with IntegrationHttpController.TYPE_BY_PATH on the backend.
 */
export const HTTP_WEBHOOK_URL_SEGMENT = new Map<IntegrationType, string>([
  [IntegrationType.HTTP, 'http'],
  [IntegrationType.SIGFOX, 'sigfox'],
  [IntegrationType.CHIRPSTACK, 'chirpstack'],
  [IntegrationType.PARTICLE, 'particle'],
  [IntegrationType.KPN, 'kpn'],
  [IntegrationType.LORIOT, 'loriot'],
  [IntegrationType.OCEANCONNECT, 'oceanconnect'],
  [IntegrationType.THINGPARK, 'thingpark'],
  [IntegrationType.TPE, 'tpe'],
  [IntegrationType.TMOBILE_IOT_CDP, 'tmobile_iot_cdp']
]);

/**
 * Webhook types PE offers the 启用安全 (请求头过滤器) panel for. ThingPark/TPE also have an
 * 启用安全 panel but it is a different mechanism (HMAC signature, not a header allow-list), and
 * OceanConnect / T-Mobile / ChirpStack have none at all.
 */
export const HTTP_HEADER_SECURITY_TYPES: IntegrationType[] = [
  IntegrationType.HTTP, IntegrationType.SIGFOX, IntegrationType.PARTICLE,
  IntegrationType.KPN, IntegrationType.LORIOT
];

/** Webhook types whose 高级设置 carries PE's replaceNoContentToOk toggle. */
export const HTTP_REPLACE_NO_CONTENT_TYPES: IntegrationType[] = [
  IntegrationType.HTTP, IntegrationType.SIGFOX, IntegrationType.LORIOT,
  IntegrationType.OCEANCONNECT, IntegrationType.THINGPARK, IntegrationType.TPE,
  IntegrationType.TMOBILE_IOT_CDP
];

/** PE's ThingPark/TPE pair — same form, same config keys, different URL segment. */
export const THINGPARK_INTEGRATION_TYPES: IntegrationType[] = [
  IntegrationType.THINGPARK, IntegrationType.TPE
];

export enum LoriotCredentialsType {
  BASIC = 'basic',
  TOKEN = 'token'
}

export const loriotCredentialsTypeLabels = new Map<LoriotCredentialsType, string>([
  [LoriotCredentialsType.BASIC, 'integration.loriot-credentials-basic'],
  [LoriotCredentialsType.TOKEN, 'integration.loriot-credentials-token']
]);

export enum TcpHandlerType {
  TEXT = 'TEXT',
  JSON = 'JSON',
  BINARY = 'BINARY'
}

/**
 * PE's HandlerConfigurationType as used by tb-udp-integration-form. Distinct from TcpHandlerType:
 * it includes HEX, and for UDP it selects payload presentation rather than framing.
 */
export enum UdpHandlerType {
  BINARY = 'BINARY',
  TEXT = 'TEXT',
  JSON = 'JSON',
  HEX = 'HEX'
}

export enum TcpMessageSeparator {
  SYSTEM_LINE_SEPARATOR = 'SYSTEM_LINE_SEPARATOR',
  NUL = 'NUL',
  CUSTOM = 'CUSTOM'
}

export enum TcpByteOrder {
  BIG_ENDIAN = 'BIG_ENDIAN',
  LITTLE_ENDIAN = 'LITTLE_ENDIAN'
}

/** Netty's MqttVersion names — the backend parses this string with MqttVersion.valueOf(). */
export enum MqttProtocolVersion {
  MQTT_3_1 = 'MQTT_3_1',
  MQTT_3_1_1 = 'MQTT_3_1_1',
  MQTT_5 = 'MQTT_5'
}

export const mqttProtocolVersionLabels = new Map<MqttProtocolVersion, string>([
  [MqttProtocolVersion.MQTT_3_1, 'MQTT 3.1'],
  [MqttProtocolVersion.MQTT_3_1_1, 'MQTT 3.1.1'],
  [MqttProtocolVersion.MQTT_5, 'MQTT 5']
]);

export enum MqttCredentialsType {
  ANONYMOUS = 'anonymous',
  BASIC = 'basic',
  CERT_PEM = 'cert.PEM',
  SAS = 'sas'
}

export enum PulsarCredentialsType {
  ANONYMOUS = 'anonymous',
  TOKEN = 'token'
}

export enum TuyaRegion {
  CN = 'CN',
  US = 'US',
  EU = 'EU',
  IN = 'IN'
}

export enum TuyaEnv {
  PROD = 'PROD',
  TEST = 'TEST'
}

export enum OpcUaSecurityPolicy {
  None = 'None',
  Basic128Rsa15 = 'Basic128Rsa15',
  Basic256 = 'Basic256',
  Basic256Sha256 = 'Basic256Sha256',
  Aes128_Sha256_RsaOaep = 'Aes128_Sha256_RsaOaep',
  Aes256_Sha256_RsaPss = 'Aes256_Sha256_RsaPss'
}

export enum OpcUaIdentityType {
  ANONYMOUS = 'anonymous',
  USERNAME = 'username'
}

/** PE's CoapSecurityMode. MIXED serves both a plain and a DTLS endpoint. */
export enum CoapSecurityMode {
  NO_SECURE = 'NO_SECURE',
  DTLS = 'DTLS',
  MIXED = 'MIXED'
}

/**
 * CoAP counterpart of {@link integrationHttpEndpointUrl}: the platform's shared CoAP server serves
 * every integration under one short `/i/` path, keyed by routing key — no per-integration port.
 * <p>
 * The segment is `i`, taken verbatim from PE (`coapEndPointUrl(base, key) => `${base}/i/${key}``).
 * It deliberately does NOT live under `/api/v1/`: the device transport owns that subtree and its
 * resource returns itself from getChild() for any name, so anything nested there is swallowed and
 * answered with 4.00 instead of reaching the integration.
 * Kept in lockstep with CoapIntegrationServer#resourcePath on the backend, which strips the routing
 * key back out to find the resource hierarchy.
 */
export const COAP_INTEGRATION_PATH_SEGMENT = 'i';

export function coapIntegrationEndpointUrl(routingKey: string, baseUrl?: string): string {
  const origin = (baseUrl || defaultCoapBaseUrl(false)).replace(/\/+$/, '');
  return `${origin}/${COAP_INTEGRATION_PATH_SEGMENT}/${routingKey || ''}`;
}

/** Inverse of the above — recovers the base URL a stored endpoint was built from. */
export function coapBaseUrlOf(endpoint?: string): string {
  if (!endpoint) {
    return '';
  }
  const marker = `/${COAP_INTEGRATION_PATH_SEGMENT}/`;
  const idx = endpoint.lastIndexOf(marker);
  return idx >= 0 ? endpoint.substring(0, idx) : '';
}

/** PE's default: scheme + browser host, with no port — the user adds one if theirs is non-standard. */
export function defaultCoapBaseUrl(dtls: boolean): string {
  const host = window.location.hostname || 'localhost';
  return dtls ? `coaps://${host}` : `coap://${host}`;
}

export enum KinesisInitialPosition {
  LATEST = 'LATEST',
  TRIM_HORIZON = 'TRIM_HORIZON'
}

export interface IntegrationTypeInfo {
  name: string;
  description: string;
  icon: string;
  /** The type supports a pre-save connectivity probe (POST /api/integration/check). */
  checkConnection?: boolean;
  /** The type has no downlink direction, so the wizard skips the downlink converter step. */
  hideDownlink?: boolean;
  /** The type only runs in a separate remote process — `remote` is forced on and locked. */
  remote?: boolean;
}

// `checkConnection` and `hideDownlink` are copied verbatim from PE's own map.
// `remote` is deliberately NOT copied: PE marks TCP/UDP/CUSTOM remote-only because PE has no
// in-process runtime for them, whereas this build does (TcpIntegrationServer, UdpIntegrationServer,
// and the allow-listed CUSTOM loader). Forcing them remote here would break working integrations.
export const integrationTypeInfoMap = new Map<IntegrationType, IntegrationTypeInfo>([
  [IntegrationType.HTTP, { name: 'HTTP', description: 'integration.type-http-description', icon: 'http' }],
  [IntegrationType.MQTT, { name: 'MQTT', description: 'integration.type-mqtt-description', icon: 'mqtt', checkConnection: true }],
  [IntegrationType.TCP, { name: 'TCP', description: 'integration.type-tcp-description', icon: 'tcp' }],
  [IntegrationType.UDP, { name: 'UDP', description: 'integration.type-udp-description', icon: 'udp' }],
  [IntegrationType.COAP, { name: 'CoAP', description: 'integration.type-coap-description', icon: 'coap', hideDownlink: true }],
  [IntegrationType.OPC_UA, { name: 'OPC-UA', description: 'integration.type-opcua-description', icon: 'opc-ua' }],
  [IntegrationType.CUSTOM, { name: 'integration.type-custom-name', description: '', icon: 'custom' }],
  [IntegrationType.AWS_IOT, { name: 'AWS IoT', description: 'integration.type-aws-iot-description', icon: 'aws-iot' }],
  [IntegrationType.AZURE_IOT_HUB, { name: 'Azure IoT Hub', description: 'integration.type-azure-iot-hub-description', icon: 'azure-iot-hub' }],
  [IntegrationType.TTN, { name: 'The Things Stack Community', description: 'integration.type-ttn-description', icon: 'ttn', checkConnection: true }],
  [IntegrationType.TTI, { name: 'The Things Stack Industries', description: 'integration.type-tti-description', icon: 'things-stack-industries', checkConnection: true }],
  [IntegrationType.AWS_SQS, { name: 'AWS SQS', description: 'integration.type-aws-sqs-description', icon: 'aws-sqs', hideDownlink: true }],
  [IntegrationType.AWS_KINESIS, { name: 'AWS Kinesis', description: 'integration.type-aws-kinesis-description', icon: 'aws-kinesis' }],
  [IntegrationType.AZURE_EVENT_HUB, { name: 'Azure Event Hub', description: 'integration.type-azure-event-hub-description', icon: 'azure-event-hub', checkConnection: true }],
  [IntegrationType.AZURE_SERVICE_BUS, { name: 'Azure Service Bus', description: 'integration.type-azure-service-bus-description', icon: 'azure-service-bus', checkConnection: true }],
  [IntegrationType.PUB_SUB, { name: 'Pub/Sub', description: 'integration.type-pubsub-description', icon: 'pub-sub', hideDownlink: true }],
  [IntegrationType.KAFKA, { name: 'Kafka', description: 'integration.type-kafka-description', icon: 'kafka', hideDownlink: true }],
  [IntegrationType.RABBITMQ, { name: 'RabbitMQ', description: 'integration.type-rabbitmq-description', icon: 'rabbitmq', checkConnection: true }],
  [IntegrationType.APACHE_PULSAR, { name: 'Apache Pulsar', description: 'integration.type-apache-pulsar-description', icon: 'apache-pulsar', hideDownlink: true }],
  [IntegrationType.TUYA, { name: 'Tuya', description: 'integration.type-tuya-description', icon: 'tuya' }],
  [IntegrationType.SIGFOX, { name: 'SigFox', description: 'integration.type-sigfox-description', icon: 'sigfox' }],
  [IntegrationType.CHIRPSTACK, { name: 'ChirpStack', description: 'integration.type-chirpstack-description', icon: 'chirpstack' }],
  [IntegrationType.PARTICLE, { name: 'Particle', description: 'integration.type-particle-description', icon: 'particle' }],
  [IntegrationType.KPN, { name: 'KPN Things', description: 'integration.type-kpn-description', icon: 'kpn' }],
  [IntegrationType.LORIOT, { name: 'Loriot', description: 'integration.type-loriot-description', icon: 'loriot' }],
  [IntegrationType.OCEANCONNECT, { name: 'OceanConnect', description: 'integration.type-oceanconnect-description', icon: 'ocean-connect', hideDownlink: true }],
  [IntegrationType.THINGPARK, { name: 'ThingPark', description: 'integration.type-thingpark-description', icon: 'thingpark' }],
  [IntegrationType.TPE, { name: 'ThingParkEnterprise', description: 'integration.type-tpe-description', icon: 'thingpark-enterprise' }],
  // PE spells this with an en dash — kept verbatim so the type list reads identically.
  [IntegrationType.TMOBILE_IOT_CDP, { name: 'iotcreators.com (T-Mobile – IoT CDP)', description: 'integration.type-tmobile-iot-cdp-description', icon: 'iotcreators.com', hideDownlink: true }]
]);

export interface MqttTopicFilter {
  filter: string;
  qos: number;
}

export interface OpcUaSubscriptionTag {
  key: string;
  nodeId: string;
  required?: boolean;
}

export interface OpcUaDeviceMapping {
  deviceName: string;
  subscriptionTags: OpcUaSubscriptionTag[];
}

export interface IntegrationConfiguration {
  // MQTT family (MQTT, AWS_IOT, AZURE_IOT_HUB, TTN, TTI): everything but topicFilters/
  // downlinkTopicPattern lives under clientConfiguration, matching PE's wire shape exactly
  // (MqttClientConfiguration.java) rather than this port's earlier flattened one.
  topicFilters?: MqttTopicFilter[];
  downlinkTopic?: string;
  downlinkTopicPattern?: string;
  // TCP / UDP
  handlerType?: TcpHandlerType;
  maxFrameLength?: number;
  charsetName?: string;
  delimiter?: string;
  // TCP only — socket tuning, shared by every handler type
  soBacklogOption?: number;
  soRcvBuf?: number;
  soSndBuf?: number;
  soKeepaliveOption?: boolean;
  tcpNoDelay?: boolean;
  // TCP TEXT handler
  stripDelimiter?: boolean;
  messageSeparator?: TcpMessageSeparator;
  customSeparatorRawValue?: string;
  // TCP BINARY handler (Netty LengthFieldBasedFrameDecoder)
  lengthFieldOffset?: number;
  lengthFieldLength?: number;
  lengthAdjustment?: number;
  initialBytesToStrip?: number;
  byteOrder?: TcpByteOrder;
  failFast?: boolean;
  // CoAP
  resourceName?: string;
  // OPC-UA
  host?: string;
  port?: number;
  applicationName?: string;
  applicationUri?: string;
  endpoint?: string;
  scanPeriodInSeconds?: number;
  security?: OpcUaSecurityPolicy;
  identity?: { type: OpcUaIdentityType; username?: string; password?: string };
  mapping?: OpcUaDeviceMapping[];
  // CUSTOM
  clazz?: string;
  // MQTT family / Kafka / RabbitMQ / Pulsar / Tuya / Kinesis / Azure Event Hub / Service Bus /
  // Pub/Sub all nest their connection settings under clientConfiguration on the wire (AWS_SQS is
  // the one exception, using the separate sqsConfiguration key below). Every field here belongs to
  // exactly one of those types' shapes — harmless to declare together since only one type is ever
  // active per integration, same as how the rest of this loosely-typed interface works.
  clientConfiguration?: {
    // MQTT family
    host?: string; port?: number; customHost?: boolean; clientId?: string;
    ssl?: boolean; protocolVersion?: MqttProtocolVersion; cleanSession?: boolean; retainedMessage?: boolean;
    maxBytesInMessage?: number; connectTimeoutSec?: number;
    credentials?: {
      type: MqttCredentialsType | PulsarCredentialsType;
      username?: string; password?: string; token?: string;
      caCert?: string; cert?: string; privateKey?: string; privateKeyPassword?: string; sasKey?: string;
    };
    // Kafka
    groupId?: string; topics?: string; bootstrapServers?: string; autoCreateTopics?: string;
    pollInterval?: number; otherProperties?: { [key: string]: string };
    // RabbitMQ (its own username/password, not nested under credentials like MQTT/Pulsar)
    exchangeName?: string; virtualHost?: string; username?: string; password?: string;
    downlinkTopic?: string; queues?: string;
    connectionTimeout?: number; handshakeTimeout?: number;
    durable?: boolean; exclusive?: boolean; autoDelete?: boolean;
    // Pulsar
    serviceUrl?: string; subscriptionName?: string; maxNumMessages?: number; maxNumBytes?: number;
    timeoutInMs?: number;
    // Tuya
    region?: TuyaRegion; accessId?: string; accessKey?: string; env?: TuyaEnv;
    // AWS Kinesis
    streamName?: string; accessKeyId?: string; secretAccessKey?: string;
    initialPositionInStream?: KinesisInitialPosition; maxRecords?: number;
    useCredentialsFromInstanceMetadata?: boolean; requestTimeout?: number;
    // Azure Event Hub / Service Bus
    connectionString?: string; consumerGroup?: string; topicName?: string; subName?: string;
    iotHubName?: string;
    downlinkConnectionString?: string; downlinkTopicName?: string;
    enablePersistentCheckpoints?: boolean; storageConnectionString?: string; containerName?: string;
    // GCP Pub/Sub
    projectId?: string; subscriptionId?: string; serviceAccountKey?: string;
    // ChirpStack — the only webhook type PE nests anything under clientConfiguration
    applicationServerUrl?: string; applicationServerAPIToken?: string; useAPI4Plus?: boolean;
    // CoAP (shared-server mode)
    securityMode?: CoapSecurityMode; coapEndpoint?: string; dtlsCoapEndpoint?: string;
  };
  // AWS SQS (top-level key, matches PE)
  sqsConfiguration?: {
    queueUrl?: string; region?: string; accessKeyId?: string; secretAccessKey?: string;
    pollingPeriodSeconds?: number;
  };
  [key: string]: any;
}

export interface Integration extends BaseData<IntegrationId> {
  tenantId?: TenantId;
  name: string;
  type: IntegrationType;
  defaultConverterId: ConverterId;
  downlinkConverterId?: ConverterId;
  routingKey?: string;
  secret?: string;
  enabled: boolean;
  allowCreateDevicesOrAssets: boolean;
  /** Runs in a separate process that connects back over gRPC with this routingKey/secret. */
  remote?: boolean;
  debugSettings?: EntityDebugSettings;
  configuration: IntegrationConfiguration;
  additionalInfo?: {description?: string; [key: string]: any};
  /** Set once this integration has been exported to a VCS repository. */
  externalId?: IntegrationId;
}

export interface IntegrationStatus {
  status: 'ACTIVE' | 'FAILED' | 'STOPPED';
  error?: string;
  eventsCountToday?: number;
}

/** Runtime status as returned inside IntegrationInfo. Absent entirely means "not reported yet". */
export interface IntegrationRuntimeStatus {
  success: boolean;
  error?: string;
}

/** An Integration plus what the list needs to render the status pill and the activity sparkline. */
export interface IntegrationInfo extends Integration {
  status?: IntegrationRuntimeStatus;
  /** Per-hour message counts over the last 24h, oldest first. */
  stats?: number[];
}

export enum IntegrationState {
  ACTIVE = 'active',
  DISABLED = 'disabled',
  FAILED = 'failed',
  PENDING = 'pending'
}

/**
 * PE's state derivation, in PE's order: a disabled integration reads "disabled" whatever its last
 * runtime status was, and one that has never reported is "pending" rather than failed.
 */
export function integrationState(integration: IntegrationInfo): IntegrationState {
  if (!integration.enabled) {
    return IntegrationState.DISABLED;
  }
  if (!integration.status) {
    return IntegrationState.PENDING;
  }
  return integration.status.success ? IntegrationState.ACTIVE : IntegrationState.FAILED;
}

/** Background/foreground pairs copied from PE's integrationStatus/integrationStatusStyle. */
export const integrationStateStyle = new Map<IntegrationState, {background: string; color: string}>([
  [IntegrationState.ACTIVE, { background: 'rgba(25, 128, 56, 0.08)', color: '#198038' }],
  [IntegrationState.FAILED, { background: 'rgba(209, 39, 48, 0.08)', color: '#d12730' }],
  [IntegrationState.PENDING, { background: 'rgba(212, 125, 24, 0.08)', color: '#D47D18' }],
  [IntegrationState.DISABLED, { background: 'rgba(0, 0, 0, 0.08)', color: 'rgba(0, 0, 0, 0.54)' }]
]);

export function newIntegration(): Integration {
  return {
    name: '',
    type: null,
    defaultConverterId: null,
    enabled: true,
    allowCreateDevicesOrAssets: true,
    // PE mints both client-side at create time rather than waiting for the server, so the routing
    // key — and the HTTP endpoint URL derived from it — are visible while still filling the wizard.
    // BaseIntegrationService only generates them when absent, so a supplied pair is honoured.
    routingKey: guid(),
    secret: generateSecret(20),
    configuration: {},
    additionalInfo: {}
  };
}

/**
 * The URL a third-party network server posts uplinks to. PE builds it from the integration's own
 * `baseUrl` field (so it can point at a public gateway rather than whatever host the admin happens
 * to be browsing from) plus the per-type segment.
 */
export function integrationHttpEndpointUrl(routingKey: string, type?: IntegrationType, baseUrl?: string): string {
  const origin = (baseUrl || window.location.origin).replace(/\/+$/, '');
  const segment = HTTP_WEBHOOK_URL_SEGMENT.get(type) || 'http';
  return `${origin}/api/v1/integrations/${segment}/${routingKey}`;
}
