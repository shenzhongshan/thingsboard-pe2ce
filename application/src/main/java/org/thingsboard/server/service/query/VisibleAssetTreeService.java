/** 文件职责：为告警等跨业务模块提供当前用户可见资产树的中性查询端口。 */
package org.thingsboard.server.service.query;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 当前用户资产、设备关系树查询接口。 */
public interface VisibleAssetTreeService {

    /** 返回当前用户可见的资产和设备树。 */
    VisibleAssetTree getVisibleAssetTree(SecurityUser user) throws ThingsboardException;
}
