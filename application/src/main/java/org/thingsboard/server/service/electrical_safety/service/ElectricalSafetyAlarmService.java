/** 文件职责：定义电气安全模块告警分页和看板统计能力。 */
package org.thingsboard.server.service.electrical_safety.service;

import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyDashboardAlarmStatisticsDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 提供电气安全专属告警边界，底层查询继续复用 ThingsBoard 告警服务。 */
public interface ElectricalSafetyAlarmService {

    /**
     * 按当前用户可见电气安全设备范围读取平台告警分页。
     *
     * @param user 当前登录用户，决定租户或客户数据范围
     * @param query 原生告警过滤条件和分页信息
     * @param productCategory 调用方声明的产品分类，必须为 electrical_safety
     * @return 仅来自电气安全设备的原生告警分页
     * @throws ThingsboardException 权限设备范围或告警查询失败时抛出
     */
    PageData<AlarmInfo> findAlarms(SecurityUser user, AlarmQueryV2 query, String productCategory)
            throws ThingsboardException;

    /**
     * 汇总电气安全看板所需的活动告警设备和周期告警数量。
     *
     * @param user 当前登录用户，决定可见设备范围
     * @param productCategory 调用方声明的产品分类，必须为 electrical_safety
     * @param period 统计周期，只接受 day、week 或 month
     * @return 当前活动告警设备 ID 及周期内按 functionCategory 聚合的事件数量
     * @throws ThingsboardException 分类、周期、权限或数据库统计失败时抛出
     */
    ElectricalSafetyDashboardAlarmStatisticsDTO getDashboardStatistics(
            SecurityUser user, String productCategory, String period) throws ThingsboardException;
}
