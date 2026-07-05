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

export class IntegrationId implements HasUUID {
  entityType = EntityType.INTEGRATION;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

// CE port: HTTP, MQTT and TCP are implemented.
export enum IntegrationType {
  HTTP = 'HTTP',
  MQTT = 'MQTT',
  TCP = 'TCP'
}

export enum TcpHandlerType {
  TEXT = 'TEXT',
  JSON = 'JSON',
  BINARY = 'BINARY'
}

export interface IntegrationTypeInfo {
  name: string;
  description: string;
  icon: string;
}

export const integrationTypeInfoMap = new Map<IntegrationType, IntegrationTypeInfo>([
  [IntegrationType.HTTP, {
    name: 'HTTP',
    description: 'integration.type-http-description',
    icon: 'http'
  }],
  [IntegrationType.MQTT, {
    name: 'MQTT',
    description: 'integration.type-mqtt-description',
    icon: 'wifi'
  }],
  [IntegrationType.TCP, {
    name: 'TCP',
    description: 'integration.type-tcp-description',
    icon: 'lan'
  }]
]);

export interface MqttTopicFilter {
  filter: string;
  qos: number;
}

export interface IntegrationConfiguration {
  // MQTT
  host?: string;
  port?: number;
  clientId?: string;
  credentials?: {type: 'anonymous' | 'basic'; username?: string; password?: string};
  topicFilters?: MqttTopicFilter[];
  // TCP
  handlerType?: TcpHandlerType;
  maxFrameLength?: number;
  charsetName?: string;
  delimiter?: string;
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
  debugMode: boolean;
  configuration: IntegrationConfiguration;
  additionalInfo?: {description?: string; [key: string]: any};
}

export interface IntegrationStatus {
  status: 'ACTIVE' | 'FAILED' | 'STOPPED';
  error?: string;
}

export function newIntegration(): Integration {
  return {
    name: '',
    type: null,
    defaultConverterId: null,
    enabled: true,
    allowCreateDevicesOrAssets: true,
    debugMode: false,
    configuration: {},
    additionalInfo: {}
  };
}

export function integrationHttpEndpointUrl(routingKey: string): string {
  return `${window.location.origin}/api/v1/integrations/http/${routingKey}`;
}
