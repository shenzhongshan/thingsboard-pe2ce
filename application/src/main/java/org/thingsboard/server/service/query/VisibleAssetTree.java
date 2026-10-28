/** 文件职责：跨业务模块传递可见资产和设备层级的中性 DTO。 */
package org.thingsboard.server.service.query;

import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.UUID;

/** 当前用户可见的资产、设备树，不携带智能防雷专有字段。 */
public record VisibleAssetTree(List<Node> roots) {

    /** 树节点的最小公共字段。 */
    public record Node(UUID id, String name, EntityType entityType, List<Node> children) {
    }
}
