/** 文件职责：按能源监管设备范围执行模块告警查询。 */
package org.thingsboard.server.service.energy_regulation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.service.DeviceAlarmQueryService;
import org.thingsboard.server.service.energy_regulation.service.EnergyRegulationAlarmService;
import org.thingsboard.server.service.query.VisibleDeviceContextService;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 能源监管告警查询实现；只向通用告警服务传递当前用户可见且属于能源监管分类的设备 ID。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EnergyRegulationAlarmServiceImpl implements EnergyRegulationAlarmService {

    /** 能源监管设备 SERVER_SCOPE.productCategory 的约定值。 */
    private static final String ENERGY_REGULATION_PRODUCT_CATEGORY = "energy_regulation";

    /** 复用统一设备可见性服务，按当前用户权限和服务器属性产品类别筛选设备。 */
    private final VisibleDeviceContextService visibleDeviceContextService;
    /** 复用平台告警数据查询及分页转换，不修改全局原生告警接口。 */
    private final DeviceAlarmQueryService deviceAlarmQueryService;

    /**
     * 按用户可见设备和模块产品分类查询告警，不改变全局原生告警接口的查询范围。
     *
     * @param user 当前登录用户，用于计算设备 READ 可见范围并限定租户
     * @param query ThingsBoard v2 告警条件，包含分页、时间、状态、严重程度、类型和受理人
     * @param productCategory 请求声明的产品大类；必须等于本服务固定类别 energy_regulation
     * @return 命中可见能源监管设备范围的告警分页；类别不匹配或设备范围为空时返回空页
     * @throws ThingsboardException 设备可见范围读取或底层告警查询失败时抛出
     */
    @Override
    public PageData<AlarmInfo> findAlarms(SecurityUser user, AlarmQueryV2 query, String productCategory)
            throws ThingsboardException {
        // 将分类参数作为模块边界校验；URL 被改成其他类别时不允许查询其他模块数据。
        if (!ENERGY_REGULATION_PRODUCT_CATEGORY.equals(productCategory)) {
            return PageData.emptyPageData();
        }
        // 共享服务先按当前用户的设备 READ 权限裁剪，再仅保留 SERVER_SCOPE.productCategory=energy_regulation 的设备。
        List<VisibleDeviceContextBO> deviceContexts = visibleDeviceContextService
                .findVisibleDeviceContexts(user, ENERGY_REGULATION_PRODUCT_CATEGORY);
        // 把上下文对象收敛为告警查询所需的设备 ID，不把设备展示字段传入查询层。
        List<UUID> deviceIds = new ArrayList<>(deviceContexts.size());
        for (VisibleDeviceContextBO deviceContext : deviceContexts) {
            if (deviceContext != null && deviceContext.getDeviceId() != null) {
                deviceIds.add(deviceContext.getDeviceId());
            }
        }
        // 通用告警查询将设备 ID 作为 originator 查询条件下推到 AlarmService，不先查询全租户告警再过滤。
        return deviceAlarmQueryService.findPageForDevices(user, query, deviceIds);
    }
}
