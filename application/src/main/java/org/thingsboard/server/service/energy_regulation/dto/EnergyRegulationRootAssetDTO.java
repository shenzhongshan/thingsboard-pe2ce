/** 文件职责：传递能源监管页面根资产选择项的数据。 */
package org.thingsboard.server.service.energy_regulation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 根资产下拉选项仅包含标识和名称，不暴露树查询内部模型。 */
@Getter
@Builder
public class EnergyRegulationRootAssetDTO {

    /** 根资产实体标识。 */
    private final UUID assetId;
    /** 根资产显示名称。 */
    private final String assetName;
}
