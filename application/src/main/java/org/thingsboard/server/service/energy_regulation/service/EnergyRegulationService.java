/** 文件职责：能源监管设备上下文与用电统计查询的业务接口。 */
package org.thingsboard.server.service.energy_regulation.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationDeviceContextDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationEnergyConsumptionDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationRootAssetDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationServiceTreeDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.UUID;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationPowerTrendDTO;

/** 能源监管专用查询能力，分类口径为 SERVER_SCOPE.productCategory。 */
public interface EnergyRegulationService {

    /** 查询当前用户可见的完整资产和设备关系树，供能源监管页面定位范围。
     * @param user 发起查询的当前安全用户
     * @return 按当前用户读取权限过滤后的树结构
     * @throws ThingsboardException 平台资产树查询失败时抛出
     */
    EnergyRegulationServiceTreeDTO getVisibleAssetTree(SecurityUser user) throws ThingsboardException;

    /** 查询当前用户可见资产树的根资产，供能源监管公司筛选使用。
     * @param user 发起查询的当前安全用户
     * @return 当前用户可见的根资产选项
     * @throws ThingsboardException 平台资产树查询失败时抛出
     */
    List<EnergyRegulationRootAssetDTO> getVisibleRootAssets(SecurityUser user) throws ThingsboardException;

    /** 返回当前用户可见且 productCategory=energy_regulation 的设备上下文。
     * @param user 发起查询的当前安全用户
     * @return 当前用户有权查看的能源监管设备上下文
     * @throws ThingsboardException 平台设备查询失败时抛出
     */
    List<EnergyRegulationDeviceContextDTO> getDeviceContexts(SecurityUser user) throws ThingsboardException;

    /** 按总正向有功电能候选键（含 ESA 的 EPI0）读取当前用户可见电表的用电统计。
     * @param user 发起查询的当前安全用户
     * @return 汇总结果、趋势序列和遥测读取状态
     * @throws ThingsboardException 平台设备或遥测查询失败时抛出
     */
    EnergyRegulationEnergyConsumptionDTO getEnergyConsumption(SecurityUser user) throws ThingsboardException;
    /** 在权限范围内按资产子树统计用电，assetId为空表示全部可见能源电表。 */
    EnergyRegulationEnergyConsumptionDTO getEnergyConsumption(SecurityUser user, UUID assetId) throws ThingsboardException;

    /** 查询今日/昨日小时平均总功率，资产及设备权限口径与用电统计相同。 */
    EnergyRegulationPowerTrendDTO getPowerTrend(SecurityUser user, UUID assetId) throws ThingsboardException;
}
