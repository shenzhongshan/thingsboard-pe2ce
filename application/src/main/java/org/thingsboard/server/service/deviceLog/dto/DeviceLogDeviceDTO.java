/** 文件职责：设备日志列表的传输对象，只保留列表需要的设备名称和设备配置类型。 */
package org.thingsboard.server.service.deviceLog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class DeviceLogDeviceDTO {

    private UUID deviceId;
    private String deviceName;
    private String deviceProfileName;
}
