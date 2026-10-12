/** 文件职责：提供能源监管模块专用的设备告警分页查询。 */
package org.thingsboard.server.service.energy_regulation.service;

import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 能源监管告警接口的业务边界。
 * 负责校验模块类别、取得当前用户可见的能源监管设备，并把限定后的设备范围交给通用告警查询服务。
 */
public interface EnergyRegulationAlarmService {

    /**
     * 查询当前用户可见能源监管设备产生的告警，并保留 ThingsBoard v2 的筛选及分页语义。
     *
     * @param user 当前登录用户，设备集合的可见性按该用户的 READ 权限确定
     * @param query 原生 v2 告警过滤条件和分页参数，不包含模块设备集合这一额外限制
     * @param productCategory URL 参数中的产品大类，必须为 energy_regulation；其他值返回空页
     * @return 当前用户有权读取且属于能源监管分类的设备告警分页；空设备范围返回空页
     * @throws ThingsboardException 设备权限或告警查询失败时抛出
     */
    PageData<AlarmInfo> findAlarms(SecurityUser user, AlarmQueryV2 query, String productCategory)
            throws ThingsboardException;
}
