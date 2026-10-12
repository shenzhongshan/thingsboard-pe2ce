/** 文件职责：承载能源监管接口使用的可见资产和设备层级。 */
package org.thingsboard.server.service.energy_regulation.dto;

import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.UUID;

/** 资产树只包含当前用户可读取的实体和有效层级关系。 */
public record EnergyRegulationServiceTreeDTO(List<Node> roots) {

    /** 统一表示资产或设备节点，设备节点仅作为层级叶子展示。 */
    public record Node(UUID id, String name, EntityType entityType, List<Node> children) {
    }
}
