/** 文件职责：电气安全页面查询接口入口，只负责权限、用户上下文和响应转换。 */
package org.thingsboard.server.controller.electrical_safety;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.controller.electrical_safety.converter.ElectricalSafetyDashboardAlarmStatisticsVOConverter;
import org.thingsboard.server.controller.electrical_safety.converter.ElectricalSafetyAssetTreeVOConverter;
import org.thingsboard.server.controller.electrical_safety.converter.ElectricalSafetyDeviceContextVOConverter;
import org.thingsboard.server.controller.electrical_safety.vo.ElectricalSafetyAssetTreeVO;
import org.thingsboard.server.controller.electrical_safety.vo.ElectricalSafetyDashboardAlarmStatisticsVO;
import org.thingsboard.server.controller.electrical_safety.vo.ElectricalSafetyDeviceContextVO;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.electrical_safety.service.ElectricalSafetyAlarmService;
import org.thingsboard.server.service.electrical_safety.service.ElectricalSafetyService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 电气安全模块专属查询接口，不修改首页、防雷或能源监管的既有路由。 */
@RestController
@TbCoreComponent
@RequestMapping("/api/electrical-safety")
@RequiredArgsConstructor
public class ElectricalSafetyController extends BaseController {

    /** 提供电气安全模块自己的设备分类过滤和资产树裁剪实现。 */
    private final ElectricalSafetyService electricalSafetyService;
    /** 提供当前用户可见电气安全设备范围内的告警查询和看板统计。 */
    private final ElectricalSafetyAlarmService electricalSafetyAlarmService;

    /**
     * 返回当前用户可见电气安全设备所在的真实资产路径。
     *
     * @return 已按用户权限和 electrical_safety 分类裁剪的资产树
     * @throws ThingsboardException 资产授权或查询失败时抛出
     */
    @GetMapping("/asset-tree")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public ElectricalSafetyAssetTreeVO getAssetTree() throws ThingsboardException {
        // 用户身份由平台安全上下文提供，Controller 不自行查询或拼接资产关系。
        return ElectricalSafetyAssetTreeVOConverter.toVO(
                electricalSafetyService.getVisibleAssetTree(getCurrentUser()));
    }

    /**
     * 返回当前用户可读且 productCategory=electrical_safety 的设备基础信息。
     *
     * @return 电气安全模块自己的设备响应对象列表
     * @throws ThingsboardException 设备权限、属性读取或查询失败时抛出
     */
    @GetMapping("/device-contexts")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<ElectricalSafetyDeviceContextVO> getDeviceContexts() throws ThingsboardException {
        // 服务层已应用用户设备权限与产品类别筛选，接口层只负责 DTO 到 VO 转换。
        return ElectricalSafetyDeviceContextVOConverter.toVOList(
                electricalSafetyService.getDeviceContexts(getCurrentUser()));
    }

    /**
     * 查询电气安全模块的告警记录，过滤参数沿用 ThingsBoard v2 原生告警查询语义。
     *
     * <p>productCategory 在 Controller 接收并交给模块 Service 校验；实际结果同时受当前用户的设备权限限制。</p>
     *
     * @param productCategory 调用方声明的设备产品分类，必须为 electrical_safety
     * @param statusList 原生告警状态筛选；为空时不限制状态
     * @param severityList 原生告警严重程度筛选；为空时不限制严重程度
     * @param typeList 原生告警类型筛选；为空时不限制告警类型
     * @param assigneeId 可选告警受理人用户 ID
     * @param pageSize 每页告警条数
     * @param page 从 0 开始的页码
     * @param textSearch 可选原生告警文本搜索词
     * @param sortProperty 可选排序字段；空值时按创建时间倒序
     * @param sortOrder 可选排序方向，只允许 ASC 或 DESC
     * @param startTime 可选告警触发开始时间，Unix 毫秒
     * @param endTime 可选告警触发结束时间，Unix 毫秒
     * @return 当前用户可见电气安全设备产生的原生告警分页
     * @throws ThingsboardException 用户权限、分页参数或告警查询失败时抛出
     */
    @GetMapping("/alarms")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<AlarmInfo> getAlarms(
            @RequestParam String productCategory,
            @RequestParam(required = false) String[] statusList,
            @RequestParam(required = false) String[] severityList,
            @RequestParam(required = false) String[] typeList,
            @RequestParam(required = false) String assigneeId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime
    ) throws ThingsboardException {
        // 将状态字符串转换为平台枚举，跳过空白条件以保持无条件查询的原生含义。
        List<AlarmSearchStatus> alarmStatusList = new ArrayList<>();
        if (statusList != null) {
            for (String status : statusList) {
                if (status != null && !status.isBlank()) {
                    alarmStatusList.add(AlarmSearchStatus.valueOf(status));
                }
            }
        }
        // 将严重程度筛选值转换为平台枚举，避免把请求字符串直接传入查询服务。
        List<AlarmSeverity> alarmSeverityList = new ArrayList<>();
        if (severityList != null) {
            for (String severity : severityList) {
                if (severity != null && !severity.isBlank()) {
                    alarmSeverityList.add(AlarmSeverity.valueOf(severity));
                }
            }
        }
        // 未指定类型时使用空集合，表示沿用原生接口的不限制类型语义。
        List<String> alarmTypeList = typeList == null ? Collections.emptyList() : Arrays.asList(typeList);
        // 受理人仅在调用方提供时解析为 ThingsBoard 用户 ID。
        UserId alarmAssigneeId = assigneeId == null ? null : new UserId(UUID.fromString(assigneeId));
        // 统一校验排序字段和方向，并构造平台时间分页对象。
        TimePageLink pageLink = createElectricalSafetyTimePageLink(
                pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        // 不绑定单一实体；模块告警服务会把查询限制到当前用户可见的电气安全设备 ID。
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, alarmAssigneeId);
        // 将固定模块类别交给服务层二次校验，避免调用方篡改参数越过模块边界。
        return electricalSafetyAlarmService.findAlarms(getCurrentUser(), query, productCategory);
    }

    /**
     * 返回电气安全看板的告警趋势、活动告警设备及告警处置状态统计。
     *
     * <p>待处理、未派发和已清除统计以告警事件为口径；未派发是待处理告警的子集，
     * 此数据不能替代独立维修工单系统的工单记录。</p>
     *
     * @param productCategory 调用方声明的设备产品分类，必须为 electrical_safety
     * @param period 统计周期，只接受 day、week 或 month
     * @return 当前用户可见电气安全设备的周期告警和处置统计
     * @throws ThingsboardException 分类、周期、权限或告警统计查询失败时抛出
     */
    @GetMapping("/dashboard-alarm-statistics")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public ElectricalSafetyDashboardAlarmStatisticsVO getDashboardAlarmStatistics(
            @RequestParam String productCategory,
            @RequestParam String period
    ) throws ThingsboardException {
        // 当前用户权限和产品分类过滤由模块告警服务执行，Controller 只映射服务 DTO。
        return ElectricalSafetyDashboardAlarmStatisticsVOConverter.toVO(
                electricalSafetyAlarmService.getDashboardStatistics(
                        getCurrentUser(), productCategory, period));
    }

    /**
     * 按 ThingsBoard 告警查询格式构造时间分页链接，并校验用户可控的排序条件。
     *
     * @param pageSize 每页记录数
     * @param page 从 0 开始的页码
     * @param textSearch 可选文本搜索内容
     * @param sortProperty 可选排序字段
     * @param sortOrder 可选排序方向，只允许 ASC 和 DESC
     * @param startTime 可选触发开始时间（Unix 毫秒）
     * @param endTime 可选触发结束时间（Unix 毫秒）
     * @return 平台告警服务使用的时间分页链接
     * @throws ThingsboardException 排序方向不支持时抛出参数错误
     */
    private TimePageLink createElectricalSafetyTimePageLink(int pageSize, int page, String textSearch,
            String sortProperty, String sortOrder, Long startTime, Long endTime) throws ThingsboardException {
        PageLink pageLink;
        if (StringUtils.isNotEmpty(sortProperty)) {
            // 只接受平台允许的字段名，避免非法字段进入底层排序表达式。
            if (!Validator.isValidProperty(sortProperty)) {
                throw new IllegalArgumentException("Invalid sort property");
            }
            SortOrder.Direction direction = SortOrder.Direction.DESC;
            if (StringUtils.isNotEmpty(sortOrder)) {
                try {
                    direction = SortOrder.Direction.valueOf(sortOrder.toUpperCase());
                } catch (IllegalArgumentException exception) {
                    throw new ThingsboardException("Unsupported sort order '" + sortOrder
                            + "'! Only 'ASC' or 'DESC' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            pageLink = new PageLink(pageSize, page, textSearch, new SortOrder(sortProperty, direction));
        } else {
            // 未指定排序字段时使用平台分页默认排序，搜索词仍按原样保留。
            pageLink = new PageLink(pageSize, page, textSearch);
        }
        return new TimePageLink(pageLink, startTime, endTime);
    }
}
