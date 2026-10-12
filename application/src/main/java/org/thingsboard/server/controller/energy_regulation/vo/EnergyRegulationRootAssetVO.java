/** 文件职责：能源监管根资产选项的 HTTP 响应对象。 */
package org.thingsboard.server.controller.energy_regulation.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 提供当前用户可见的根资产标识和名称。 */
@Getter
@Builder
public class EnergyRegulationRootAssetVO {

    /** 根资产实体标识，供能源统计范围筛选使用。 */
    private final UUID assetId;
    /** 根资产名称，显示在公司筛选下拉框中。 */
    private final String assetName;
}
