/** 文件职责：定义能源监管资产树接口的 HTTP 响应结构。 */
package org.thingsboard.server.controller.energy_regulation.vo;

import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.UUID;

/** 资产树节点响应不暴露服务层或关系查询内部对象。 */
public record EnergyRegulationAssetTreeVO(List<Node> roots) {

    /** 资产或设备节点，以及按用户权限裁剪后的下级节点。 */
    public record Node(UUID id, String name, EntityType entityType, List<Node> children) {
    }
}
