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

export class ConverterId implements HasUUID {
  entityType = EntityType.CONVERTER;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

export enum ConverterType {
  UPLINK = 'UPLINK',
  DOWNLINK = 'DOWNLINK'
}

export const converterTypeTranslations = new Map<ConverterType, string>([
  [ConverterType.UPLINK, 'converter.type-uplink'],
  [ConverterType.DOWNLINK, 'converter.type-downlink']
]);

export enum ConverterScriptLanguage {
  TBEL = 'TBEL',
  JS = 'JS'
}

export interface ConverterConfiguration {
  scriptLang: ConverterScriptLanguage;
  decoder?: string;
  encoder?: string;
}

export interface Converter extends BaseData<ConverterId> {
  tenantId?: TenantId;
  name: string;
  type: ConverterType;
  integrationType?: string;
  debugMode: boolean;
  configuration: ConverterConfiguration;
  additionalInfo?: {description?: string; [key: string]: any};
}

export const DEFAULT_TBEL_DECODER =
`// Decode an uplink message from a buffer
// payload - array of bytes
// metadata - key/value object

/** Decoder **/

// decode payload to string
// var payloadStr = decodeToString(payload);

// decode payload to JSON
var data = decodeToJson(payload);

var deviceName = data.deviceName;
var deviceType = data.deviceType;

// Result object with device attributes/telemetry data
var result = {
   deviceName: deviceName,
   deviceType: deviceType,
   attributes: {
       integrationName: metadata['integrationName']
   },
   telemetry: {
       temperature: data.temperature,
       humidity: data.humidity
   }
};

return result;`;

export const DEFAULT_JS_DECODER =
`// Decode an uplink message from a buffer
// payload - array of bytes
// metadata - key/value object

/** Decoder **/

// decode payload to JSON
var data = decodeToJson(payload);

var deviceName = data.deviceName;
var deviceType = data.deviceType;

// Result object with device attributes/telemetry data
var result = {
   deviceName: deviceName,
   deviceType: deviceType,
   attributes: {
       integrationName: metadata['integrationName']
   },
   telemetry: {
       temperature: data.temperature,
       humidity: data.humidity
   }
};

/** Helper functions **/

function decodeToString(payload) {
   return String.fromCharCode.apply(String, payload);
}

function decodeToJson(payload) {
   return JSON.parse(decodeToString(payload));
}

return result;`;

export const DEFAULT_TBEL_ENCODER =
`// Encode downlink data from incoming Rule Engine message

// msg - JSON message payload downlink message json
// msgType - type of message, for ex. 'ATTRIBUTES_UPDATED', 'POST_TELEMETRY_REQUEST', etc.
// metadata - list of key-value pairs with additional data about the message
// integrationMetadata - list of key-value pairs with additional data defined in Integration executing this converter

/** Encoder **/

var data = {};
data.tempValue = msg.temp;

// Result object with encoded downlink payload
var result = {
    // downlink data content type: JSON, TEXT or BINARY (base64 format)
    contentType: "JSON",
    // downlink data
    data: JSON.stringify(data),
    // Optional metadata object presented in key/value format
    metadata: {}
};

return result;`;

export const DEFAULT_JS_ENCODER =
`// Encode downlink data from incoming Rule Engine message

// msg - JSON message payload downlink message json
// msgType - type of message, for ex. 'ATTRIBUTES_UPDATED', 'POST_TELEMETRY_REQUEST', etc.
// metadata - list of key-value pairs with additional data about the message
// integrationMetadata - list of key-value pairs with additional data defined in Integration executing this converter

/** Encoder **/

var data = {};
data.tempValue = msg.temp;

// Result object with encoded downlink payload
var result = {
    // downlink data content type: JSON, TEXT or BINARY (base64 format)
    contentType: "JSON",
    // downlink data
    data: JSON.stringify(data),
    // Optional metadata object presented in key/value format
    metadata: {}
};

return result;`;

export function newConverter(): Converter {
  return {
    name: '',
    type: ConverterType.UPLINK,
    debugMode: true,
    configuration: {
      scriptLang: ConverterScriptLanguage.TBEL,
      decoder: DEFAULT_TBEL_DECODER
    },
    additionalInfo: {}
  };
}
