/** 文件职责：把能源监管服务 DTO 转换为能源监管 HTTP VO。 */
package org.thingsboard.server.controller.energy_regulation.converter;

import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationDeviceContextVO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationDeviceContextDTO;

import java.util.ArrayList;
import java.util.List;

/** 只执行 Controller 边界的逐字段转换，不包含业务计算。 */
public final class EnergyRegulationDeviceContextVOConverter {

    /** 转换职责由静态映射方法承担。 */
    private EnergyRegulationDeviceContextVOConverter() {
    }

    /** 将能源监管 DTO 列表转换为 HTTP VO 列表。
     * @param sources 服务层设备上下文 DTO；为 null 时返回空列表
     * @return 与输入顺序一致的 HTTP VO 列表
     */
    public static List<EnergyRegulationDeviceContextVO> toVOList(
            List<EnergyRegulationDeviceContextDTO> sources) {
        // 初始化独立响应列表，避免将服务层集合直接暴露给 Controller 调用方。
        List<EnergyRegulationDeviceContextVO> result = new ArrayList<>();
        if (sources == null) {
            return result;
        }
        // 按服务返回顺序逐项映射，保持设备列表排序稳定。
        for (EnergyRegulationDeviceContextDTO source : sources) {
            result.add(toVO(source));
        }
        return result;
    }

    /** 将单个能源监管服务 DTO 映射为 Controller 返回的 VO。
     * @param source 服务层设备上下文 DTO
     * @return Controller 返回的设备上下文 VO
     */
    private static EnergyRegulationDeviceContextVO toVO(EnergyRegulationDeviceContextDTO source) {
        // 只映射页面需要的设备展示字段，不在 HTTP 边界增加业务判断。
        return EnergyRegulationDeviceContextVO.builder()
                .deviceId(source.getDeviceId())
                .deviceName(source.getDeviceName())
                .deviceProfileName(source.getDeviceProfileName())
                .productCategory(source.getProductCategory())
                .productName(source.getProductName())
                .active(source.getActive())
                .latitude(source.getLatitude())
                .longitude(source.getLongitude())
                .build();
    }
}
