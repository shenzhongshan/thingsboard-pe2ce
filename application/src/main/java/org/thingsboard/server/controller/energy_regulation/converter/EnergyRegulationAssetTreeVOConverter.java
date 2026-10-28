/** 文件职责：将能源监管资产树 DTO 转换为 HTTP VO。 */
package org.thingsboard.server.controller.energy_regulation.converter;

import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationAssetTreeVO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationServiceTreeDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 只负责递归字段映射，不执行资产查询或权限判断。 */
public final class EnergyRegulationAssetTreeVOConverter {

    /** 转换器仅通过静态方法使用。 */
    private EnergyRegulationAssetTreeVOConverter() {
    }

    /** 将服务树及其所有后代节点映射为 HTTP 响应。 */
    public static EnergyRegulationAssetTreeVO toVO(EnergyRegulationServiceTreeDTO source) {
        if (source == null || source.roots() == null) {
            return new EnergyRegulationAssetTreeVO(Collections.emptyList());
        }
        List<EnergyRegulationAssetTreeVO.Node> roots = new ArrayList<>(source.roots().size());
        for (EnergyRegulationServiceTreeDTO.Node root : source.roots()) {
            roots.add(toVO(root));
        }
        return new EnergyRegulationAssetTreeVO(roots);
    }

    /** 递归映射单个节点，空子节点统一返回空列表。 */
    private static EnergyRegulationAssetTreeVO.Node toVO(EnergyRegulationServiceTreeDTO.Node source) {
        List<EnergyRegulationAssetTreeVO.Node> children = new ArrayList<>();
        if (source.children() != null) {
            for (EnergyRegulationServiceTreeDTO.Node child : source.children()) {
                children.add(toVO(child));
            }
        }
        return new EnergyRegulationAssetTreeVO.Node(source.id(), source.name(), source.entityType(), children);
    }
}
