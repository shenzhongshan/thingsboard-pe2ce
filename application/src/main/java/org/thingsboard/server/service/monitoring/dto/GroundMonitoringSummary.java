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

import java.util.List;

/**
 * 地网监测看板的聚合返回 DTO。
 *
 * <p>设备数和在线状态是当前快照；告警数仅统计请求的日、周或月窗口内，
 * 且以告警创建（触发）时间为准；告警类型列表按数量降序返回。阻值使用同一
 * 日/周/月窗口内全部有效的 groundResistance 遥测，汇总最大值、最小值及其来源。</p>
 */
@Getter
@RequiredArgsConstructor
public class GroundMonitoringSummary {

    private final int total;
    private final int online;
    private final int offline;
    private final int alarmCount;
    /** 所选周期内按告警类型汇总的数量，按数量降序排列。 */
    private final List<GroundAlarmTypeTotalDTO> alarmTypeTotals;
    /** 周期内出现告警的地网设备占全部地网设备的百分比，按设备 ID 去重。 */
    private final double alarmRate;
    /** 所选窗口内的最大阻值；没有有效遥测时为 null。 */
    private final Double maxResistance;
    /** 所选窗口内的最小阻值；没有有效遥测时为 null。 */
    private final Double minResistance;
    /** 所选窗口内阻值最值及其设备、参数和时间来源。 */
    private final GroundResistanceExtrema resistanceExtrema;
}
