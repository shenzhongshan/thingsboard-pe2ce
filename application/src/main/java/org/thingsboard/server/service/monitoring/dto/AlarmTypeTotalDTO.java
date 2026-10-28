/** 文件职责：定义智能防雷监测服务中的告警类型数量传输片段。 */
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 防雷监测服务传输的告警类型及数量，不依赖告警模块的展示对象。 */
@Getter
@RequiredArgsConstructor
public class AlarmTypeTotalDTO {

    /** 告警类型；空类型已在告警统计服务中统一为“未分类”。 */
    private final String type;
    /** 当前设备和周期范围内该类型的告警数量。 */
    private final int count;
}
