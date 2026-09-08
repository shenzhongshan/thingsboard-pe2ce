/**
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
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 地网监测看板的聚合返回 DTO。
 *
 * <p>设备数和在线状态是当前快照；告警数仅统计请求的日、周或月窗口内，
 * 且以告警创建（触发）时间为准。阻值取每台地网设备的最新 groundResistance
 * 遥测，再求其中最大和最小值。没有有效阻值时两个字段均返回 null。</p>
 */
@Getter
@RequiredArgsConstructor
public class GroundMonitoringSummary {

    private final int total;
    private final int online;
    private final int offline;
    private final int alarmCount;
    private final Double maxResistance;
    private final Double minResistance;
}
