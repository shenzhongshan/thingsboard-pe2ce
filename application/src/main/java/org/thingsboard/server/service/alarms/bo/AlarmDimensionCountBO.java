/** 文件职责：保存告警类型或严重程度的内部聚合结果。 */
package org.thingsboard.server.service.alarms.bo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 服务内部使用的维度名称和数量，不作为 HTTP DTO 返回。 */
@Getter
@RequiredArgsConstructor
public class AlarmDimensionCountBO {

    private final String dimension;
    private final long count;
}
