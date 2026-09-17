/** 文件职责：保存数据库按设备聚合后的告警统计结果。 */
package org.thingsboard.server.service.alarms.bo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** 服务内部使用的单设备告警总数和已处理数量，不作为 HTTP DTO 返回。 */
@Getter
@RequiredArgsConstructor
public class AlarmDeviceCountBO {

    private final UUID deviceId;
    private final long alarmCount;
    private final long handledCount;
}
