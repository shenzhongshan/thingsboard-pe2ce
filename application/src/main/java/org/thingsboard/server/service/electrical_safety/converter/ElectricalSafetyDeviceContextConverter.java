/** 文件职责：把共享可见设备查询结果映射为电气安全服务 DTO。 */
package org.thingsboard.server.service.electrical_safety.converter;

import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyDeviceContextDTO;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;

/** 只执行显式字段映射，不进行设备查询、权限判断或页面格式化。 */
public final class ElectricalSafetyDeviceContextConverter {

    /** 转换器通过静态映射方法使用，不保存请求状态。 */
    private ElectricalSafetyDeviceContextConverter() {
    }

    /**
     * 将共享查询层的通用设备上下文映射为电气安全模块 DTO。
     * 缺失 active 按离线处理，避免把无状态设备误显示为在线。
     *
     * @param source 当前用户有权查看的通用设备上下文
     * @return 电气安全模块自己的设备上下文；调用方应先排除空对象
     */
    public static ElectricalSafetyDeviceContextDTO toDTO(VisibleDeviceContextBO source) {
        return ElectricalSafetyDeviceContextDTO.builder()
                .deviceId(source.getDeviceId())
                .deviceName(source.getDeviceName())
                .deviceProfileName(source.getDeviceProfileName())
                .productCategory(source.getProductCategory())
                .productName(source.getProductName())
                .functionCategory(source.getFunctionCategory())
                .deviceProtocol(source.getDeviceProtocol())
                .active(Boolean.TRUE.equals(source.getActive()))
                .lastActivityTime(source.getLastActivityTime())
                .latitude(source.getLatitude())
                .longitude(source.getLongitude())
                .build();
    }
}
