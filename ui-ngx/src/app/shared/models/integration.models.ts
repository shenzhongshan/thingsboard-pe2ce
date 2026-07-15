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

export enum TcpHandlerType {
  TEXT = 'TEXT',
  JSON = 'JSON',
  BINARY = 'BINARY'
}

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

export enum KinesisInitialPosition {
  LATEST = 'LATEST',
  TRIM_HORIZON = 'TRIM_HORIZON'
}

export interface IntegrationTypeInfo {
  name: string;
  description: string;
  icon: string;
}

export const integrationTypeInfoMap = new Map<IntegrationType, IntegrationTypeInfo>([
  [IntegrationType.HTTP, { name: 'HTTP', description: 'integration.type-http-description', icon: 'http' }],
  [IntegrationType.MQTT, { name: 'MQTT', description: 'integration.type-mqtt-description', icon: 'mqtt' }],
  [IntegrationType.TCP, { name: 'TCP', description: 'integration.type-tcp-description', icon: 'tcp' }],
  [IntegrationType.UDP, { name: 'UDP', description: 'integration.type-udp-description', icon: 'udp' }],
  [IntegrationType.COAP, { name: 'CoAP', description: 'integration.type-coap-description', icon: 'coap' }],
  [IntegrationType.OPC_UA, { name: 'OPC-UA', description: 'integration.type-opcua-description', icon: 'opc-ua' }],
  [IntegrationType.CUSTOM, { name: 'integration.type-custom-name', description: '', icon: 'custom' }],
  [IntegrationType.AWS_IOT, { name: 'AWS IoT', description: 'integration.type-aws-iot-description', icon: 'aws-iot' }],
  [IntegrationType.AZURE_IOT_HUB, { name: 'Azure IoT Hub', description: 'integration.type-azure-iot-hub-description', icon: 'azure-iot-hub' }],
  [IntegrationType.TTN, { name: 'The Things Stack Community', description: 'integration.type-ttn-description', icon: 'ttn' }],
  [IntegrationType.TTI, { name: 'The Things Stack Industries', description: 'integration.type-tti-description', icon: 'things-stack-industries' }],
  [IntegrationType.AWS_SQS, { name: 'AWS SQS', description: 'integration.type-aws-sqs-description', icon: 'aws-sqs' }],
  [IntegrationType.AWS_KINESIS, { name: 'AWS Kinesis', description: 'integration.type-aws-kinesis-description', icon: 'aws-kinesis' }],
  [IntegrationType.AZURE_EVENT_HUB, { name: 'Azure Event Hub', description: 'integration.type-azure-event-hub-description', icon: 'azure-event-hub' }],
  [IntegrationType.AZURE_SERVICE_BUS, { name: 'Azure Service Bus', description: 'integration.type-azure-service-bus-description', icon: 'azure-service-bus' }],
  [IntegrationType.PUB_SUB, { name: 'Pub/Sub', description: 'integration.type-pubsub-description', icon: 'pub-sub' }],
  [IntegrationType.KAFKA, { name: 'Kafka', description: 'integration.type-kafka-description', icon: 'kafka' }],
  [IntegrationType.RABBITMQ, { name: 'RabbitMQ', description: 'integration.type-rabbitmq-description', icon: 'rabbitmq' }],
  [IntegrationType.APACHE_PULSAR, { name: 'Apache Pulsar', description: 'integration.type-apache-pulsar-description', icon: 'apache-pulsar' }],
  [IntegrationType.TUYA, { name: 'Tuya', description: 'integration.type-tuya-description', icon: 'tuya' }],
  [IntegrationType.SIGFOX, { name: 'SigFox', description: 'integration.type-sigfox-description', icon: 'sigfox' }],
  [IntegrationType.CHIRPSTACK, { name: 'ChirpStack', description: 'integration.type-chirpstack-description', icon: 'chirpstack' }],
  [IntegrationType.PARTICLE, { name: 'Particle', description: 'integration.type-particle-description', icon: 'particle' }],
  [IntegrationType.KPN, { name: 'KPN Things', description: 'integration.type-kpn-description', icon: 'kpn' }],
  [IntegrationType.LORIOT, { name: 'Loriot', description: 'integration.type-loriot-description', icon: 'loriot' }],
  [IntegrationType.OCEANCONNECT, { name: 'OceanConnect', description: 'integration.type-oceanconnect-description', icon: 'ocean-connect' }],
  [IntegrationType.THINGPARK, { name: 'ThingPark', description: 'integration.type-thingpark-description', icon: 'thingpark' }],
  [IntegrationType.TPE, { name: 'ThingParkEnterprise', description: 'integration.type-tpe-description', icon: 'thingpark-enterprise' }],
  [IntegrationType.TMOBILE_IOT_CDP, { name: 'iotcreators.com (T-Mobile IoT CDP)', description: 'integration.type-tmobile-iot-cdp-description', icon: 'iotcreators.com' }]
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
  // MQTT family (MQTT, AWS_IOT, AZURE_IOT_HUB, TTN, TTI)
  host?: string;
  port?: number;
  customHost?: boolean;
  clientId?: string;
  credentials?: {
    type: MqttCredentialsType;
    username?: string; password?: string;
    caCert?: string; cert?: string; privateKey?: string; privateKeyPassword?: string;
    sasKey?: string;
  };
  topicFilters?: MqttTopicFilter[];
  downlinkTopic?: string;
  downlinkTopicPattern?: string;
  // TCP / UDP
  handlerType?: TcpHandlerType;
  maxFrameLength?: number;
  charsetName?: string;
  delimiter?: string;
  // CoAP
  resourceName?: string;
  // OPC-UA
  applicationName?: string;
  applicationUri?: string;
  endpoint?: string;
  scanPeriodInSeconds?: number;
  security?: OpcUaSecurityPolicy;
  identity?: { type: OpcUaIdentityType; username?: string; password?: string };
  mapping?: OpcUaDeviceMapping[];
  // CUSTOM
  clazz?: string;
  // Kafka / RabbitMQ / Pulsar / Tuya / AWS SQS / Kinesis / Azure Event Hub / Service Bus / Pub/Sub
  // (all nested under clientConfiguration on the wire, except AWS_SQS which uses sqsConfiguration)
  clientConfiguration?: {
    // Kafka
    clientId?: string; groupId?: string; topics?: string; bootstrapServers?: string; autoCreateTopics?: string;
    pollInterval?: number; otherProperties?: { [key: string]: string };
    // RabbitMQ
    exchangeName?: string; host?: string; port?: number; virtualHost?: string; username?: string; password?: string;
    downlinkTopic?: string; queues?: string; connectionTimeout?: number; handshakeTimeout?: number;
    durable?: boolean; exclusive?: boolean; autoDelete?: boolean;
    // Pulsar
    serviceUrl?: string; subscriptionName?: string; maxNumMessages?: number; maxNumBytes?: number;
    timeoutInMs?: number; credentials?: { type: PulsarCredentialsType; token?: string };
    // Tuya
    region?: TuyaRegion; accessId?: string; accessKey?: string; env?: TuyaEnv;
    // AWS Kinesis
    streamName?: string; accessKeyId?: string; secretAccessKey?: string;
    initialPositionInStream?: KinesisInitialPosition; maxRecords?: number;
    // Azure Event Hub / Service Bus
    connectionString?: string; consumerGroup?: string; topicName?: string; subName?: string;
    // GCP Pub/Sub
    projectId?: string; subscriptionId?: string; serviceAccountKey?: string;
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
  debugSettings?: EntityDebugSettings;
  configuration: IntegrationConfiguration;
  additionalInfo?: {description?: string; [key: string]: any};
}

export interface IntegrationStatus {
  status: 'ACTIVE' | 'FAILED' | 'STOPPED';
  error?: string;
  eventsCountToday?: number;
}

export function newIntegration(): Integration {
  return {
    name: '',
    type: null,
    defaultConverterId: null,
    enabled: true,
    allowCreateDevicesOrAssets: true,
    configuration: {},
    additionalInfo: {}
  };
}

export function integrationHttpEndpointUrl(routingKey: string): string {
  return `${window.location.origin}/api/v1/integrations/http/${routingKey}`;
}
