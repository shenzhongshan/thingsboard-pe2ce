/** 文件职责：定义智能防雷监测接口中的告警类型数量响应片段。 */
package org.thingsboard.server.service.monitoring.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 防雷看板返回的告警类型及数量，不依赖告警模块的 HTTP DTO。 */
@Getter
@RequiredArgsConstructor
public class AlarmTypeTotalVO {

    /** 告警类型；空类型已在告警统计服务中统一为“未分类”。 */
    private final String type;
    /** 当前设备和周期范围内该类型的告警数量。 */
    private final int count;
}
