/** 文件职责：集中完成监测设备 BO 到服务层 DTO 的转换。 */
package org.thingsboard.server.service.monitoring.converter;

import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;

/** 监测设备 BO/DTO 转换器，禁止在 Controller 中复制业务字段转换逻辑。 */
public final class MonitoringDeviceDTOConverter {

    private MonitoringDeviceDTOConverter() {
    }

    /** 将服务内部设备上下文转换为服务层传输对象。 */
    public static SmartLightningDeviceContextDTO toDTO(MonitoringDeviceContextBO source) {
        return SmartLightningDeviceContextDTO.builder()
                .deviceId(source.getDeviceId())
                .deviceName(source.getDeviceName())
                .deviceProfileName(source.getDeviceProfileName())
                .productCategory(source.getProductCategory())
                .productName(source.getProductName())
                .functionCategory(source.getFunctionCategory())
                .deviceProtocol(source.getDeviceProtocol())
                .active(source.getActive())
                .lastActivityTime(source.getLastActivityTime())
                .gatewayIdentifier(source.getGatewayIdentifier())
                .meterAddress(source.getMeterAddress())
                .circuitName(source.getCircuitName())
                .timeSetting(source.getTimeSetting())
                .latitude(source.getLatitude())
                .longitude(source.getLongitude())
                .build();
    }
}
