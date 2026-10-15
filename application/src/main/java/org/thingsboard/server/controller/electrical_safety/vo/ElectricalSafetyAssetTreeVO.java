/** 文件职责：定义电气安全资产树接口的 HTTP 响应结构。 */
package org.thingsboard.server.controller.electrical_safety.vo;

import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.UUID;

/** 只暴露前端资产树需要的实体标识、名称、类型和层级关系。 */
public record ElectricalSafetyAssetTreeVO(List<Node> roots) {

    /** 资产或电气安全设备树节点。 */
    public record Node(UUID id, String name, EntityType entityType, List<Node> children) {
    }
}
