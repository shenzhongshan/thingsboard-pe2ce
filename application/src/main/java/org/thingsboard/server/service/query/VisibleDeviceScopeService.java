/** 文件职责：为跨业务模块提供当前用户可见设备范围查询的中性端口。 */
package org.thingsboard.server.service.query;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Set;
import java.util.UUID;

/**
 * 当前用户设备权限范围查询接口。
 *
 * <p>告警模块只需要设备 ID，不应依赖智能防雷设备服务的全部产品分类能力；
 * 通过这个中性接口可以避免告警服务直接绑定 monitoring 业务接口。</p>
 */
public interface VisibleDeviceScopeService {

    /** 返回当前用户有读取权限的全部设备 ID，不按产品分类筛选。 */
    Set<UUID> getVisibleDeviceIds(SecurityUser user) throws ThingsboardException;
}
