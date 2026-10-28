/** 文件职责：平台自定义首页的设备上下文查询接口。 */
package org.thingsboard.server.service.homeoverview;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/**
 * 平台总览首页业务接口。
 *
 * <p>分类由设备 SERVER_SCOPE 的 productCategory 和 functionCategory 决定，
 * 不在首页重新按照设备名称或型号猜测分类。</p>
 */
public interface HomeOverviewService {

    /** 返回当前用户可见的全部设备上下文，包含首页地图和分类统计所需字段。 */
    List<SmartLightningDeviceContextDTO> getVisibleDeviceContexts(SecurityUser user)
            throws ThingsboardException;
}
