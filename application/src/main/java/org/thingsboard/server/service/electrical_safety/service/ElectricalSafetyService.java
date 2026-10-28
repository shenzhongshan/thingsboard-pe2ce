/** 文件职责：定义电气安全模块的设备上下文和资产树查询能力。 */
package org.thingsboard.server.service.electrical_safety.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyAssetTreeDTO;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyDeviceContextDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/** 电气安全专用服务契约；查询范围始终受当前用户权限和产品分类限制。 */
public interface ElectricalSafetyService {

    /**
     * 查询当前用户可见的电气安全资产分支及其设备叶节点。
     *
     * @param user 发起查询的当前安全用户
     * @return 仅包含关联电气安全设备的资产树；无设备时返回空根列表
     * @throws ThingsboardException 平台可见资产树查询失败时抛出
     */
    ElectricalSafetyAssetTreeDTO getVisibleAssetTree(SecurityUser user) throws ThingsboardException;

    /**
     * 查询当前用户可见且 productCategory=electrical_safety 的设备基础信息。
     *
     * @param user 发起查询的当前安全用户
     * @return 电气安全设备上下文；没有匹配设备时返回空列表
     * @throws ThingsboardException 平台设备、属性或权限查询失败时抛出
     */
    List<ElectricalSafetyDeviceContextDTO> getDeviceContexts(SecurityUser user)
            throws ThingsboardException;
}
