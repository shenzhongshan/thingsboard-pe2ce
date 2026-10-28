/** 文件职责：将电气安全设备服务 DTO 映射为 HTTP 响应对象。 */
package org.thingsboard.server.controller.electrical_safety.converter;

import org.thingsboard.server.controller.electrical_safety.vo.ElectricalSafetyDeviceContextVO;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyDeviceContextDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 只执行服务 DTO 到接口 VO 的显式字段映射。 */
public final class ElectricalSafetyDeviceContextVOConverter {

    /** 转换器通过静态方法使用，不持有服务或请求状态。 */
    private ElectricalSafetyDeviceContextVOConverter() {
    }

    /** 将设备上下文列表转换为稳定的 HTTP 响应列表。 */
    public static List<ElectricalSafetyDeviceContextVO> toVOList(
            List<ElectricalSafetyDeviceContextDTO> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<ElectricalSafetyDeviceContextVO> result = new ArrayList<>(source.size());
        for (ElectricalSafetyDeviceContextDTO context : source) {
            if (context != null) {
                result.add(toVO(context));
            }
        }
        return result;
    }

    /** 显式映射单个电气安全设备上下文，不执行字段推断。 */
    private static ElectricalSafetyDeviceContextVO toVO(ElectricalSafetyDeviceContextDTO source) {
        return ElectricalSafetyDeviceContextVO.builder()
                .deviceId(source.getDeviceId())
                .deviceName(source.getDeviceName())
                .deviceProfileName(source.getDeviceProfileName())
                .productCategory(source.getProductCategory())
                .productName(source.getProductName())
                .functionCategory(source.getFunctionCategory())
                .deviceProtocol(source.getDeviceProtocol())
                .active(source.getActive())
                .lastActivityTime(source.getLastActivityTime())
                .latitude(source.getLatitude())
                .longitude(source.getLongitude())
                .build();
    }
}
