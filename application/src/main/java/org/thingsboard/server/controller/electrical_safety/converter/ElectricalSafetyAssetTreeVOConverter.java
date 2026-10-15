/** 文件职责：将电气安全服务资产树 DTO 递归映射为 HTTP 响应 VO。 */
package org.thingsboard.server.controller.electrical_safety.converter;

import org.thingsboard.server.controller.electrical_safety.vo.ElectricalSafetyAssetTreeVO;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyAssetTreeDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 只转换资产树结构，不读取关系或执行产品分类筛选。 */
public final class ElectricalSafetyAssetTreeVOConverter {

    /** 转换器通过静态方法使用，不持有服务或请求状态。 */
    private ElectricalSafetyAssetTreeVOConverter() {
    }

    /** 将服务层树转换为页面接口响应；空树统一返回空根列表。 */
    public static ElectricalSafetyAssetTreeVO toVO(ElectricalSafetyAssetTreeDTO source) {
        if (source == null || source.roots() == null || source.roots().isEmpty()) {
            return new ElectricalSafetyAssetTreeVO(Collections.emptyList());
        }
        List<ElectricalSafetyAssetTreeVO.Node> roots = new ArrayList<>(source.roots().size());
        for (ElectricalSafetyAssetTreeDTO.Node root : source.roots()) {
            if (root != null) {
                roots.add(toVO(root));
            }
        }
        return new ElectricalSafetyAssetTreeVO(roots);
    }

    /** 递归映射单个节点，并将意外缺失的子节点列表规范化为空列表。 */
    private static ElectricalSafetyAssetTreeVO.Node toVO(ElectricalSafetyAssetTreeDTO.Node source) {
        List<ElectricalSafetyAssetTreeVO.Node> children = new ArrayList<>();
        if (source.children() != null) {
            for (ElectricalSafetyAssetTreeDTO.Node child : source.children()) {
                if (child != null) {
                    children.add(toVO(child));
                }
            }
        }
        return new ElectricalSafetyAssetTreeVO.Node(
                source.id(), source.name(), source.entityType(), children);
    }
}
