/** 文件职责：实现电气安全模块的分类设备查询与资产树裁剪。 */
package org.thingsboard.server.service.electrical_safety.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.electrical_safety.converter.ElectricalSafetyDeviceContextConverter;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyAssetTreeDTO;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyDeviceContextDTO;
import org.thingsboard.server.service.electrical_safety.service.ElectricalSafetyService;
import org.thingsboard.server.service.query.VisibleAssetTree;
import org.thingsboard.server.service.query.VisibleAssetTreeService;
import org.thingsboard.server.service.query.VisibleDeviceContextService;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 电气安全设备查询实现。
 *
 * <p>权限和设备属性读取复用中性的可见设备查询服务；资产关系复用中性可见树服务。
 * 本实现负责固定 electrical_safety 分类、组装本模块 DTO，并裁剪无关资产分支。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class ElectricalSafetyServiceImpl implements ElectricalSafetyService {

    /** 本模块产品大类服务器属性值，与 thingsboard.yml 产品映射保持一致。 */
    private static final String ELECTRICAL_SAFETY_PRODUCT_CATEGORY = "electrical_safety";

    /** 查询当前用户可读设备及其服务器属性，并按产品大类过滤。 */
    private final VisibleDeviceContextService visibleDeviceContextService;
    /** 查询当前用户有权限访问的中性资产和设备关系树。 */
    private final VisibleAssetTreeService visibleAssetTreeService;

    /**
     * 查询电气安全设备所在的资产分支，保留设备叶节点和通往设备的资产祖先。
     *
     * @param user 发起查询的当前安全用户
     * @return 当前用户可见的电气安全资产树
     * @throws ThingsboardException 共享设备或资产树查询失败时抛出
     */
    @Override
    public ElectricalSafetyAssetTreeDTO getVisibleAssetTree(SecurityUser user) throws ThingsboardException {
        // 先确定设备权限与 productCategory 范围，后续只允许这些设备进入模块树。
        List<VisibleDeviceContextBO> electricalDevices = findVisibleElectricalSafetyDevices(user);
        Set<UUID> electricalDeviceIds = new HashSet<>();
        for (VisibleDeviceContextBO device : electricalDevices) {
            if (device != null && device.getDeviceId() != null) {
                electricalDeviceIds.add(device.getDeviceId());
            }
        }
        if (electricalDeviceIds.isEmpty()) {
            // 没有符合分类的设备时，不必读取整棵用户资产树。
            return new ElectricalSafetyAssetTreeDTO(new ArrayList<>());
        }

        // 共享树服务先按平台授权裁剪，避免模块服务自行重做租户或客户权限判断。
        VisibleAssetTree visibleTree = visibleAssetTreeService.getVisibleAssetTree(user);
        List<ElectricalSafetyAssetTreeDTO.Node> roots = new ArrayList<>();
        if (visibleTree == null || visibleTree.roots() == null) {
            return new ElectricalSafetyAssetTreeDTO(roots);
        }
        for (VisibleAssetTree.Node root : visibleTree.roots()) {
            if (root != null) {
                ElectricalSafetyAssetTreeDTO.Node filteredRoot = toElectricalSafetyTreeNode(root, electricalDeviceIds);
                if (filteredRoot != null) {
                    roots.add(filteredRoot);
                }
            }
        }
        return new ElectricalSafetyAssetTreeDTO(roots);
    }

    /**
     * 查询本模块需要展示的真实设备资料。
     *
     * @param user 发起查询的当前安全用户
     * @return 经过本模块分类过滤的设备 DTO 列表
     * @throws ThingsboardException 共享设备查询失败时抛出
     */
    @Override
    public List<ElectricalSafetyDeviceContextDTO> getDeviceContexts(SecurityUser user)
            throws ThingsboardException {
        // 共享查询层负责当前用户权限、设备读取与服务器属性批量获取。
        List<VisibleDeviceContextBO> contexts = findVisibleElectricalSafetyDevices(user);
        List<ElectricalSafetyDeviceContextDTO> result = new ArrayList<>(contexts.size());
        for (VisibleDeviceContextBO context : contexts) {
            if (context != null) {
                // 在业务边界转换为电气安全 DTO，不向 Controller 泄露通用 BO。
                result.add(ElectricalSafetyDeviceContextConverter.toDTO(context));
            }
        }
        return result;
    }

    /**
     * 统一取得当前用户可见的电气安全设备范围。
     *
     * @param user 发起查询的当前安全用户
     * @return 共享查询层返回的电气安全设备；共享实现若返回 null 则统一为空集合
     * @throws ThingsboardException 当前用户权限或共享设备查询失败时抛出
     */
    private List<VisibleDeviceContextBO> findVisibleElectricalSafetyDevices(SecurityUser user)
            throws ThingsboardException {
        // 产品类别仅由服务器属性判定，不根据设备名称、型号或遥测键推断。
        List<VisibleDeviceContextBO> devices = visibleDeviceContextService.findVisibleDeviceContexts(
                user, ELECTRICAL_SAFETY_PRODUCT_CATEGORY);
        return devices == null ? Collections.emptyList() : devices;
    }

    /**
     * 递归裁剪共享树：只输出电气安全设备，并保留其所有资产祖先。
     *
     * @param source 当前用户可见树中的源节点
     * @param electricalDeviceIds 当前用户可见的电气安全设备 ID
     * @return 模块专用树节点；不含电气安全设备的资产分支返回 null
     */
    private ElectricalSafetyAssetTreeDTO.Node toElectricalSafetyTreeNode(
            VisibleAssetTree.Node source, Set<UUID> electricalDeviceIds) {
        if (source.entityType() == EntityType.DEVICE) {
            if (!electricalDeviceIds.contains(source.id())) {
                return null;
            }
            return new ElectricalSafetyAssetTreeDTO.Node(
                    source.id(), source.name(), source.entityType(), Collections.emptyList());
        }
        if (source.entityType() != EntityType.ASSET) {
            return null;
        }

        List<ElectricalSafetyAssetTreeDTO.Node> children = new ArrayList<>();
        if (source.children() != null) {
            for (VisibleAssetTree.Node child : source.children()) {
                if (child != null) {
                    ElectricalSafetyAssetTreeDTO.Node filteredChild =
                            toElectricalSafetyTreeNode(child, electricalDeviceIds);
                    if (filteredChild != null) {
                        children.add(filteredChild);
                    }
                }
            }
        }
        if (children.isEmpty()) {
            return null;
        }
        return new ElectricalSafetyAssetTreeDTO.Node(
                source.id(), source.name(), source.entityType(), children);
    }
}
