/** 文件职责：将能源监管根资产 DTO 映射为 HTTP VO。 */
package org.thingsboard.server.controller.energy_regulation.converter;

import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationRootAssetVO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationRootAssetDTO;

import java.util.ArrayList;
import java.util.List;

/** 只负责服务层 DTO 到接口 VO 的字段转换。 */
public final class EnergyRegulationRootAssetVOConverter {

    /** 转换器仅通过静态方法使用。 */
    private EnergyRegulationRootAssetVOConverter() {
    }

    /** 转换根资产列表；空值输入按空列表返回，保持集合响应稳定。 */
    public static List<EnergyRegulationRootAssetVO> toVOList(List<EnergyRegulationRootAssetDTO> sources) {
        List<EnergyRegulationRootAssetVO> result = new ArrayList<>();
        if (sources == null) {
            return result;
        }
        for (EnergyRegulationRootAssetDTO source : sources) {
            result.add(EnergyRegulationRootAssetVO.builder()
                    .assetId(source.getAssetId())
                    .assetName(source.getAssetName())
                    .build());
        }
        return result;
    }
}
