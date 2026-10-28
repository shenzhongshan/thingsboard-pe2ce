/** 文件职责：承载电气安全服务层按模块设备裁剪后的资产关系树。 */
package org.thingsboard.server.service.electrical_safety.dto;

import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.UUID;

/** 资产树只包含当前用户可见且关联电气安全设备的资产路径与设备节点。 */
public record ElectricalSafetyAssetTreeDTO(List<Node> roots) {

    /** 资产或设备节点；设备节点作为筛选设备列表所需的叶节点。 */
    public record Node(UUID id, String name, EntityType entityType, List<Node> children) {
    }
}
