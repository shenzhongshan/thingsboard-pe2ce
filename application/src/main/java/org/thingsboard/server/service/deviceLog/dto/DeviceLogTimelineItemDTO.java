/** 文件职责：设备日志时间线单条事件的传输对象。 */
package org.thingsboard.server.service.deviceLog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 后端统一审计事件和告警发生事件的页面模型。 */
@Getter
@AllArgsConstructor
public class DeviceLogTimelineItemDTO {

    private long timestamp;
    private String category;
    private String key;
    private String value;
    private List<DeviceLogTimelineFieldDTO> fields;
}
