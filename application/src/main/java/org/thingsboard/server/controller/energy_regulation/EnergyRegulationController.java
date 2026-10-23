/** 文件职责：能源监管查询的 HTTP 入口，只处理路由和权限。 */
package org.thingsboard.server.controller.energy_regulation;

import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationPowerTrendVO;
import org.thingsboard.server.controller.energy_regulation.converter.EnergyRegulationPowerTrendVOConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.service.energy_regulation.service.EnergyRegulationAlarmService;
import org.thingsboard.server.service.energy_regulation.service.EnergyRegulationService;
import org.thingsboard.server.controller.energy_regulation.converter.EnergyRegulationDeviceContextVOConverter;
import org.thingsboard.server.controller.energy_regulation.converter.EnergyRegulationEnergyConsumptionVOConverter;
import org.thingsboard.server.controller.energy_regulation.converter.EnergyRegulationRootAssetVOConverter;
import org.thingsboard.server.controller.energy_regulation.converter.EnergyRegulationAssetTreeVOConverter;
import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationDeviceContextVO;
import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationEnergyConsumptionVO;
import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationRootAssetVO;
import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationAssetTreeVO;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

/** 能源监管页面专用接口，设备可见范围仍由平台授权服务控制。 */
@RestController
@TbCoreComponent
@RequestMapping("/api/energy-regulation")
@RequiredArgsConstructor
public class EnergyRegulationController extends BaseController {

    /** 提供按当前用户权限过滤后的能源监管查询结果。 */
    private final EnergyRegulationService energyRegulationService;
    /** 提供当前用户可见能源监管设备范围内的告警查询。 */
    private final EnergyRegulationAlarmService energyRegulationAlarmService;

    /** 返回当前用户可见的完整资产和设备层级，供能源监管资产树使用。
     * @return 当前用户有权读取的资产树
     * @throws ThingsboardException 平台授权或资产树查询失败时抛出
     */
    @GetMapping("/asset-tree")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public EnergyRegulationAssetTreeVO getAssetTree() throws ThingsboardException {
        // 当前用户上下文交由共享可见树服务处理，不在 Controller 内查询实体关系。
        return EnergyRegulationAssetTreeVOConverter.toVO(
                energyRegulationService.getVisibleAssetTree(getCurrentUser()));
    }

    /** 返回当前用户可见资产树中的根资产，供公司范围下拉框使用。
     * @return 当前用户可见的根资产选项
     * @throws ThingsboardException 平台授权或资产树查询失败时抛出
     */
    @GetMapping("/root-assets")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<EnergyRegulationRootAssetVO> getRootAssets() throws ThingsboardException {
        // 当前用户上下文传递到服务层，由共享资产树服务执行可见性控制。
        return EnergyRegulationRootAssetVOConverter.toVOList(
                energyRegulationService.getVisibleRootAssets(getCurrentUser()));
    }

    /** 返回 productCategory=energy_regulation 的当前用户可见设备上下文。
     * @return 当前用户有权查看的能源监管设备列表
     * @throws ThingsboardException 平台权限或设备查询失败时抛出
     */
    @GetMapping("/device-contexts")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<EnergyRegulationDeviceContextVO> getDeviceContexts() throws ThingsboardException {
        // 从平台上下文取得当前用户并委托服务层执行权限范围内的设备查询。
        // Controller 边界只负责将服务 DTO 转为 HTTP VO 后返回。
        return EnergyRegulationDeviceContextVOConverter.toVOList(
                energyRegulationService.getDeviceContexts(getCurrentUser()));
    }

    /** 返回能源监管电表的日、周、月用电统计和对应趋势。
     * @return 按当前用户可见设备计算的用电响应
     * @throws ThingsboardException 平台权限或遥测查询失败时抛出
     */
    @GetMapping("/energy-consumption")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public EnergyRegulationEnergyConsumptionVO getEnergyConsumption(
            @RequestParam(required = false) UUID assetId) throws ThingsboardException {
        // 将当前用户交给服务层读取可见能源设备的累计电能并完成聚合。
        // 将内部统计 DTO 映射为稳定的 HTTP 响应契约。
        return EnergyRegulationEnergyConsumptionVOConverter.toVO(
                energyRegulationService.getEnergyConsumption(getCurrentUser(), assetId));
    }

    /**
     * 查询能源监管模块的告警页；筛选和分页参数沿用 ThingsBoard v2 告警查询格式。
     *
     * <p>productCategory 必须为 energy_regulation；实际查询还会限制在当前用户有 READ 权限的能源设备内。</p>
     *
     * @param productCategory 产品大类，只接受 energy_regulation，用于防止跨模块查询
     * @param statusList 告警状态筛选，可传多个原生 AlarmSearchStatus 枚举值
     * @param severityList 告警严重程度筛选，可传多个原生 AlarmSeverity 枚举值
     * @param typeList 告警类型筛选；为空时不按告警类型限制
     * @param assigneeId 受理人用户 ID；为空时不按受理人限制
     * @param pageSize 每页记录数
     * @param page 从 0 开始的页码
     * @param textSearch ThingsBoard 告警文本搜索条件
     * @param sortProperty 排序字段；未提供时按创建时间倒序
     * @param sortOrder 排序方向
     * @param startTime 查询开始时间，Unix 毫秒时间戳
     * @param endTime 查询结束时间，Unix 毫秒时间戳
     * @return 当前用户可见能源监管设备产生的告警分页，保留原生 AlarmInfo 字段和分页信息
     * @throws ThingsboardException 当前用户权限校验或告警查询失败时抛出
     */
    @GetMapping("/alarms")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<AlarmInfo> getEnergyRegulationAlarms(
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
        // 将 URL 中的状态名称转换成平台枚举；未传状态条件时保留空列表，表示不按状态过滤。
        List<AlarmSearchStatus> alarmStatusList = new java.util.ArrayList<>();
        if (statusList != null) {
            for (String status : statusList) {
                if (status != null && !status.isBlank()) {
                    alarmStatusList.add(AlarmSearchStatus.valueOf(status));
                }
            }
        }
        // 将严重程度名称转换成平台枚举；忽略空白项，避免它们进入底层查询条件。
        List<AlarmSeverity> alarmSeverityList = new java.util.ArrayList<>();
        if (severityList != null) {
            for (String severity : severityList) {
                if (severity != null && !severity.isBlank()) {
                    alarmSeverityList.add(AlarmSeverity.valueOf(severity));
                }
            }
        }
        // 保留调用方给出的告警类型；null 转为空列表以表达“不限制类型”。
        List<String> alarmTypeList = typeList == null ? Collections.emptyList() : Arrays.asList(typeList);
        // 将受理人字符串解析为 ThingsBoard 用户 ID；未指定受理人时传 null 表示不筛选。
        UserId alarmAssigneeId = assigneeId == null ? null : new UserId(UUID.fromString(assigneeId));
        // 统一构建平台时间分页链接，时间范围使用 Unix 毫秒并携带文本搜索和排序参数。
        TimePageLink pageLink = createEnergyRegulationTimePageLink(
                pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        // 该模块查询不绑定单一 affectedEntity，由模块 Service 再限制到当前用户可见的能源设备集合。
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, alarmAssigneeId);
        // 将当前用户、原生告警条件和模块类别交给业务层完成分类校验及设备权限过滤。
        return energyRegulationAlarmService.findAlarms(getCurrentUser(), query, productCategory);
    }

    /**
     * 在能源监管 Controller 所属子包中构造带时间范围的分页链接。
     *
     * <p>平台基类的同名帮助方法是包级可见，子包 Controller 无法调用；此处沿用平台相同的排序字段和排序方向校验。
     *
     * @param pageSize 每页记录数
     * @param page 从 0 开始的页码
     * @param textSearch 可选文本搜索内容
     * @param sortProperty 可选排序字段
     * @param sortOrder 可选排序方向，仅支持 ASC 或 DESC
     * @param startTime 可选起始时间，Unix 毫秒
     * @param endTime 可选结束时间，Unix 毫秒
     * @return 包含分页、排序和时间范围的查询链接
     * @throws IllegalArgumentException 排序字段无效时抛出
     * @throws ThingsboardException 排序方向不受支持时抛出参数错误
     */
    private TimePageLink createEnergyRegulationTimePageLink(int pageSize, int page, String textSearch,
            String sortProperty, String sortOrder, Long startTime, Long endTime) throws ThingsboardException {
        PageLink pageLink;
        if (StringUtils.isNotEmpty(sortProperty)) {
            if (!Validator.isValidProperty(sortProperty)) {
                throw new IllegalArgumentException("Invalid sort property");
            }
            SortOrder.Direction direction = SortOrder.Direction.ASC;
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
            pageLink = new PageLink(pageSize, page, textSearch);
        }
        return new TimePageLink(pageLink, startTime, endTime);
    }

    /** 当前用户权限范围内指定资产的小时平均总有功功率，单位kW。 */
    @GetMapping("/power-trend")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public EnergyRegulationPowerTrendVO getPowerTrend(
            @RequestParam(required = false) UUID assetId) throws ThingsboardException {
        return EnergyRegulationPowerTrendVOConverter.toVO(
                energyRegulationService.getPowerTrend(getCurrentUser(), assetId));
    }
}
