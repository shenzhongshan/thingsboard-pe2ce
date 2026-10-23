/** 文件职责：把共享设备查询 BO 转换为能源监管专属 DTO。 */
package org.thingsboard.server.service.energy_regulation.converter;

import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationDeviceContextDTO;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;

/** 只执行显式字段映射，不查询实体或判断业务权限。 */
public final class EnergyRegulationDeviceContextConverter {

    private EnergyRegulationDeviceContextConverter() {
    }

    /** 将共享设备上下文映射为能源监管服务 DTO。
     * @param source 查询层返回的通用可见设备上下文
     * @return 不依赖防雷 DTO 的能源监管设备上下文 DTO
     */
    public static EnergyRegulationDeviceContextDTO toDTO(VisibleDeviceContextBO source) {
        // 将共享查询层的可见设备字段映射为能源监管模块自己的服务 DTO。
        return EnergyRegulationDeviceContextDTO.builder()
                .deviceId(source.getDeviceId())
                .deviceName(source.getDeviceName())
                .deviceProfileName(source.getDeviceProfileName())
                .productCategory(source.getProductCategory())
                .productName(source.getProductName())
                .functionCategory(source.getFunctionCategory())
                .deviceProtocol(source.getDeviceProtocol())
                .lastActivityTime(source.getLastActivityTime())
                .active(source.getActive())
                .latitude(source.getLatitude())
                .longitude(source.getLongitude())
                .build();
    }
}
