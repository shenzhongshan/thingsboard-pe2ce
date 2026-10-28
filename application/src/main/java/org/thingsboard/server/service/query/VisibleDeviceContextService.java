/** 文件职责：提供按当前用户权限和产品大类查询设备上下文的共享服务契约。 */
package org.thingsboard.server.service.query;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/** 当前用户可见设备上下文的通用查询能力，不绑定具体业务模块。 */
public interface VisibleDeviceContextService {

    /**
     * 查询当前用户可见的设备上下文。
     *
     * @param user 当前登录用户，用于应用 ThingsBoard 设备读取权限
     * @param productCategory SERVER_SCOPE 产品大类；为 null 或空白时不按产品大类筛选
     * @return 按权限和可选产品大类过滤后的设备上下文
     */
    List<VisibleDeviceContextBO> findVisibleDeviceContexts(SecurityUser user, String productCategory)
            throws ThingsboardException;
}
