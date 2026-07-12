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

// CE port: same 28 locales shipped as base translations (SUPPORTED_LANGS, derived at build time
// from ui-ngx/src/assets/locale/*.json). Name/country metadata mirrors PE's custom-translation
// list page.
export interface LocaleMeta {
  code: string;
  name: string;
  country: string;
}

export const LOCALE_METAS: LocaleMeta[] = [
  {code: 'ar_AE', name: 'العربية (阿拉伯语)', country: '阿拉伯联合酋长国'},
  {code: 'ca_ES', name: 'Català (加泰罗尼亚语)', country: '西班牙'},
  {code: 'cs_CZ', name: 'Čeština (捷克语)', country: '捷克'},
  {code: 'da_DK', name: 'Dansk (丹麦语)', country: '丹麦'},
  {code: 'de_DE', name: 'Deutsch (德语)', country: '德国'},
  {code: 'el_GR', name: 'Ελληνικά (希腊语)', country: '希腊'},
  {code: 'en_US', name: 'English (英语)', country: '美国'},
  {code: 'es_ES', name: 'Español (西班牙语)', country: '西班牙'},
  {code: 'fa_IR', name: 'فارسی (波斯语)', country: '伊朗'},
  {code: 'fr_FR', name: 'Français (法语)', country: '法国'},
  {code: 'hi_IN', name: 'हिंदी (印地语)', country: '印度'},
  {code: 'it_IT', name: 'Italiano (意大利语)', country: '意大利'},
  {code: 'ja_JP', name: '日本語 (日语)', country: '日本'},
  {code: 'ka_GE', name: 'ქართული (格鲁吉亚语)', country: '格鲁吉亚'},
  {code: 'ko_KR', name: '한국어 (韩语)', country: '韩国'},
  {code: 'lt_LT', name: 'Lietuvių (立陶宛语)', country: '立陶宛'},
  {code: 'lv_LV', name: 'Latviešu (拉脱维亚语)', country: '拉脱维亚'},
  {code: 'nl_BE', name: 'Nederlands (荷兰语)', country: '比利时'},
  {code: 'nl_NL', name: 'Nederlands (荷兰语)', country: '荷兰'},
  {code: 'no_NO', name: 'Norsk (挪威语)', country: '挪威'},
  {code: 'pl_PL', name: 'Polski (波兰语)', country: '波兰'},
  {code: 'pt_BR', name: 'Português (葡萄牙语)', country: '巴西'},
  {code: 'ro_RO', name: 'Română (罗马尼亚语)', country: '罗马尼亚'},
  {code: 'sl_SI', name: 'Slovenščina (斯洛文尼亚语)', country: '斯洛文尼亚'},
  {code: 'tr_TR', name: 'Türkçe (土耳其语)', country: '土耳其'},
  {code: 'uk_UA', name: 'Українська (乌克兰语)', country: '乌克兰'},
  {code: 'zh_CN', name: '中文 (中文)', country: '中国'},
  {code: 'zh_TW', name: '繁體中文 (繁体中文)', country: '台湾'}
];

export function localeMeta(code: string): LocaleMeta {
  return LOCALE_METAS.find(l => l.code === code) || {code, name: code, country: ''};
}

// Flattened dot-path -> string map, used by the basic grid editor and progress computation.
export type FlatTranslation = {[key: string]: string};

export function flattenTranslation(obj: any, prefix = ''): FlatTranslation {
  const result: FlatTranslation = {};
  if (!obj || typeof obj !== 'object') {
    return result;
  }
  for (const key of Object.keys(obj)) {
    const value = obj[key];
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      Object.assign(result, flattenTranslation(value, path));
    } else {
      result[path] = String(value);
    }
  }
  return result;
}

export function setAtPath(root: any, path: string, value: string): void {
  const parts = path.split('.');
  let current = root;
  for (let i = 0; i < parts.length - 1; i++) {
    if (typeof current[parts[i]] !== 'object' || current[parts[i]] === null) {
      current[parts[i]] = {};
    }
    current = current[parts[i]];
  }
  current[parts[parts.length - 1]] = value;
}

export function deleteAtPath(root: any, path: string): void {
  const parts = path.split('.');
  let current = root;
  for (let i = 0; i < parts.length - 1; i++) {
    if (typeof current[parts[i]] !== 'object' || current[parts[i]] === null) {
      return;
    }
    current = current[parts[i]];
  }
  delete current[parts[parts.length - 1]];
}

export type TranslationRowFilter = 'translated' | 'untranslated' | 'customized' | 'added';

export interface TranslationRow {
  key: string;
  english: string;
  value: string;
  translated: boolean; // shipped target-locale base file has a value for this key
  customized: boolean; // the owner-level override has a value for this key
  added: boolean;       // key doesn't exist in the base en_US catalog at all
}

export interface CustomTranslationRow extends LocaleMeta {
  progress: number; // base-locale translation completeness, 0-100 (unrelated to custom overrides)
  customized: boolean;
}
