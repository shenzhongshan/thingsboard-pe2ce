/** 文件职责：设备日志时间线单个字段的展示对象。 */
package org.thingsboard.server.service.deviceLog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 时间线字段只保留页面展示所需的标签和值。 */
@Getter
@AllArgsConstructor
public class DeviceLogTimelineFieldDTO {

    private String label;
    private String value;
}
