/*
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const forwardUrl = "http://localhost:8080";
const wsForwardUrl = "ws://localhost:8080";
const ruleNodeUiforwardUrl = forwardUrl;

const PROXY_CONFIG = {
  // WebSocket 订阅使用同一个 /api 前缀；必须在通用 HTTP 代理上开启 ws，
  // 否则开发服务器会先匹配 /api，导致 /api/ws 升级请求无法转发到 ThingsBoard。
  "/api/ws": {
    "target": wsForwardUrl,
    "ws": true,
    "secure": false
  },
  "/api": {
    "target": forwardUrl,
    "secure": false,
    "ws": true,
  },
  "/static/rulenode": {
    "target": ruleNodeUiforwardUrl,
    "secure": false,
  },
  "/static/widgets": {
    "target": forwardUrl,
    "secure": false,
  },
  "/oauth2": {
    "target": forwardUrl,
    "secure": false,
  },
  "/login/oauth2": {
    "target": forwardUrl,
    "secure": false,
  },
};

module.exports = PROXY_CONFIG;
