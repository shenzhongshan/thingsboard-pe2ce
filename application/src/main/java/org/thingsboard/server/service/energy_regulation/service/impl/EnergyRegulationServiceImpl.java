/** 文件职责：能源监管设备上下文与标准用电遥测查询实现。 */
package org.thingsboard.server.service.energy_regulation.service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.energy_regulation.service.EnergyRegulationService;
import org.thingsboard.server.service.energy_regulation.converter.EnergyRegulationDeviceContextConverter;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationDeviceContextDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationEnergyConsumptionDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationRootAssetDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationServiceTreeDTO;
import org.thingsboard.server.service.energy_regulation.util.EnergyRegulationEnergyConsumptionCalculator;
import org.thingsboard.server.service.energy_regulation.util.EnergyRegulationTelemetryQueryFactory;
import org.thingsboard.server.service.query.VisibleDeviceContextService;
import org.thingsboard.server.service.query.VisibleAssetTree;
import org.thingsboard.server.service.query.VisibleAssetTreeService;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.ZonedDateTime;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationPowerTrendDTO;
import org.thingsboard.server.service.energy_regulation.util.EnergyRegulationPowerTrendCalculator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/** 能源监管设备查询业务实现，不复制 ThingsBoard 设备权限和批量属性读取逻辑。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EnergyRegulationServiceImpl implements EnergyRegulationService {

    /** 能源监管设备的产品大类服务器属性值。 */
    private static final String ENERGY_REGULATION_PRODUCT_CATEGORY = "energy_regulation";
    /** 单批并发遥测查询的设备上限。 */
    private static final int TELEMETRY_BATCH_SIZE = 50;

    /** 提供当前用户可见设备及服务器属性上下文的共享查询服务。 */
    private final VisibleDeviceContextService deviceContextService;
    /** 查询平台权限范围内的中性资产树，不依赖防雷接口 DTO。 */
    private final VisibleAssetTreeService visibleAssetTreeService;
    /** 读取已持久化的设备时序遥测。 */
    private final TimeseriesService timeseriesService;

    /** 按能源监管设备 ID 裁剪中性共享树，仅保留命中设备及其祖先资产。
     * @param user 发起查询的当前安全用户
     * @return 当前用户可见能源设备所在的资产分支和设备节点
     * @throws ThingsboardException 平台资产树查询失败时抛出
     */
    @Override
    public EnergyRegulationServiceTreeDTO getVisibleAssetTree(SecurityUser user) throws ThingsboardException {
        // 先复用分类查询服务取得用户可读且 productCategory=energy_regulation 的设备集合。
        List<VisibleDeviceContextBO> energyDevices = findVisibleEnergyDevices(user);
        Set<UUID> energyDeviceIds = new HashSet<>();
        for (VisibleDeviceContextBO device : energyDevices) {
            if (device.getDeviceId() != null) {
                energyDeviceIds.add(device.getDeviceId());
            }
        }
        if (energyDeviceIds.isEmpty()) {
            // 当前用户没有能源监管设备时无需读取整棵资产关系树。
            return new EnergyRegulationServiceTreeDTO(new ArrayList<>());
        }
        // 再读取按用户权限过滤的中性树，绝不把无能源设备的资产分支返回给前端。
        VisibleAssetTree tree = visibleAssetTreeService.getVisibleAssetTree(user);
        List<EnergyRegulationServiceTreeDTO.Node> roots = new ArrayList<>();
        if (tree == null || tree.roots() == null) {
            return new EnergyRegulationServiceTreeDTO(roots);
        }
        for (VisibleAssetTree.Node root : tree.roots()) {
            if (root != null) {
                EnergyRegulationServiceTreeDTO.Node energyRoot = toEnergyTreeNode(root, energyDeviceIds);
                if (energyRoot != null) {
                    roots.add(energyRoot);
                }
            }
        }
        return new EnergyRegulationServiceTreeDTO(roots);
    }

    /** 递归裁剪共享树：只保留能源设备，并保留通往这些设备的资产祖先。
     * @param source 共享权限树中的源节点
     * @param energyDeviceIds 当前用户可见的能源监管设备 ID
     * @return 能源监管服务边界节点；无目标设备的资产分支返回 null
     */
    private EnergyRegulationServiceTreeDTO.Node toEnergyTreeNode(
            VisibleAssetTree.Node source, Set<UUID> energyDeviceIds) {
        if (source.entityType() == EntityType.DEVICE) {
            return energyDeviceIds.contains(source.id())
                    ? new EnergyRegulationServiceTreeDTO.Node(
                            source.id(), source.name(), source.entityType(), Collections.emptyList())
                    : null;
        }
        if (source.entityType() != EntityType.ASSET) {
            return null;
        }
        List<EnergyRegulationServiceTreeDTO.Node> children = new ArrayList<>();
        if (source.children() != null) {
            for (VisibleAssetTree.Node child : source.children()) {
                if (child != null) {
                    EnergyRegulationServiceTreeDTO.Node energyChild = toEnergyTreeNode(child, energyDeviceIds);
                    if (energyChild != null) {
                        children.add(energyChild);
                    }
                }
            }
        }
        return children.isEmpty() ? null
                : new EnergyRegulationServiceTreeDTO.Node(source.id(), source.name(), source.entityType(), children);
    }

    /** 提取当前用户可见资产树的根资产，供能源监管筛选器展示公司选项。
     * @param user 发起查询的当前安全用户
     * @return 当前用户可见的根资产选项
     * @throws ThingsboardException 资产树查询失败时抛出
     */
    @Override
    public List<EnergyRegulationRootAssetDTO> getVisibleRootAssets(SecurityUser user)
            throws ThingsboardException {
        // 中性树服务按平台授权规则组装资产层级；这里只读取根节点，不附带设备分类限制。
        VisibleAssetTree tree = visibleAssetTreeService.getVisibleAssetTree(user);
        List<EnergyRegulationRootAssetDTO> roots = new ArrayList<>();
        if (tree == null || tree.roots() == null) {
            return roots;
        }
        for (VisibleAssetTree.Node node : tree.roots()) {
            // 公司筛选只展示根资产，忽略意外返回的非资产根节点。
            if (node != null && node.entityType() == EntityType.ASSET && node.id() != null) {
                roots.add(EnergyRegulationRootAssetDTO.builder()
                        .assetId(node.id())
                        .assetName(node.name())
                        .build());
            }
        }
        return roots;
    }

    /** 按产品大类复用共享设备上下文服务，再映射为能源监管专属 DTO。
     * @param user 发起查询的当前安全用户
     * @return 当前用户可见的能源监管设备上下文
     * @throws ThingsboardException 共享设备查询失败时抛出
     */
    @Override
    public List<EnergyRegulationDeviceContextDTO> getDeviceContexts(SecurityUser user)
            throws ThingsboardException {
        // 先通过共享查询服务完成当前用户权限过滤和能源产品分类过滤。
        List<VisibleDeviceContextBO> contexts = findVisibleEnergyDevices(user);
        // 在服务边界将通用查询对象转换为能源监管自己的 DTO，避免依赖其他业务模块的 DTO。
        List<EnergyRegulationDeviceContextDTO> result = new ArrayList<>(contexts.size());
        for (VisibleDeviceContextBO context : contexts) {
            result.add(EnergyRegulationDeviceContextConverter.toDTO(context));
        }
        return result;
    }

    /** 按自然日、周和月读取标准累计表底，统一汇总不同电表类型的标准键。
     * @param user 发起查询的当前安全用户
     * @return 用电周期汇总、趋势和读取状态
     * @throws ThingsboardException 设备查询或遥测读取被中断、失败时抛出
     */
    @Override
    public EnergyRegulationEnergyConsumptionDTO getEnergyConsumption(SecurityUser user)
            throws ThingsboardException {
        return getEnergyConsumption(user, null);
    }

    /** 资产筛选只缩小当前用户可见电表范围，不允许扩大权限。 */
    @Override
    public EnergyRegulationEnergyConsumptionDTO getEnergyConsumption(SecurityUser user, UUID assetId)
            throws ThingsboardException {
        // 复用设备上下文查询结果，确保遥测统计仅覆盖当前用户可见的能源设备。
        List<VisibleDeviceContextBO> devices = findScopedEnergyDevices(user, assetId);
        // 统一固定本次统计的上海时区时间，避免多个周期边界各自取当前时间造成不一致。
        ZonedDateTime now = ZonedDateTime.now(EnergyRegulationEnergyConsumptionCalculator.TIME_ZONE);
        if (devices.isEmpty()) {
            // 空设备范围仍返回完整的空统计结构，保持前端响应契约稳定。
            return EnergyRegulationEnergyConsumptionCalculator.calculate(Collections.emptyList(), 0, now);
        }

        // 只读取汇总、小时/自然日/七天趋势所需的边界表底，不扫描上月以来的全部小时桶。
        List<ReadTsKvQuery> queries = EnergyRegulationTelemetryQueryFactory.createConsumptionQueries(now);
        // 按设备分批读取遥测，并保留每台设备的失败状态供响应统计。
        TelemetryBatchResult batchResult = readEnergyTelemetryInBatches(user, devices, queries);
        // 将查询结果交给纯计算器统一生成汇总和趋势，不在此处持久化派生结果。
        return EnergyRegulationEnergyConsumptionCalculator.calculate(
                batchResult.entriesByDevice(), batchResult.failedDeviceCount(), now);
    }

    /** 按自然日分段读取所需的小时平均功率，保留昨日曲线及本周、本月峰值。 */
    @Override
    public EnergyRegulationPowerTrendDTO getPowerTrend(SecurityUser user, UUID assetId) throws ThingsboardException {
        List<VisibleDeviceContextBO> devices = findScopedEnergyDevices(user, assetId);
        ZonedDateTime now = ZonedDateTime.now(EnergyRegulationEnergyConsumptionCalculator.TIME_ZONE);
        List<ReadTsKvQuery> queries = EnergyRegulationTelemetryQueryFactory.createPowerQueries(now);
        TelemetryBatchResult batch = readEnergyTelemetryInBatches(user, devices, queries);
        return EnergyRegulationPowerTrendCalculator.calculate(batch.entriesByDevice(), now);
    }

    /** 根据公共权限树定位所选资产/设备后代，并与分类设备集合取交集。 */
    private List<VisibleDeviceContextBO> findScopedEnergyDevices(SecurityUser user, UUID assetId)
            throws ThingsboardException {
        List<VisibleDeviceContextBO> devices = findVisibleEnergyDevices(user);
        if (assetId == null) {
            return devices;
        }
        Set<UUID> ids = new HashSet<>();
        VisibleAssetTree tree = visibleAssetTreeService.getVisibleAssetTree(user);
        if (tree != null && tree.roots() != null) {
            for (VisibleAssetTree.Node root : tree.roots()) {
                collectScopedDeviceIds(root, assetId, false, ids);
            }
        }
        List<VisibleDeviceContextBO> result = new ArrayList<>();
        for (VisibleDeviceContextBO device : devices) {
            if (ids.contains(device.getDeviceId())) {
                result.add(device);
            }
        }
        return result;
    }

    /** 只收集命中节点子树中的设备，未授权或不存在的节点得到空集合。 */
    private void collectScopedDeviceIds(VisibleAssetTree.Node node, UUID assetId, boolean inScope, Set<UUID> ids) {
        if (node == null) {
            return;
        }
        boolean selected = inScope || assetId.equals(node.id());
        if (selected && node.entityType() == EntityType.DEVICE) {
            ids.add(node.id());
        }
        for (VisibleAssetTree.Node child : node.children() == null ? Collections.<VisibleAssetTree.Node>emptyList() : node.children()) {
            collectScopedDeviceIds(child, assetId, selected, ids);
        }
    }

    /** 获取统一的能源监管可见设备范围，分类依据固定为 productCategory。
     * @param user 发起查询的当前安全用户
     * @return 当前用户可见且属于能源监管分类的设备上下文
     * @throws ThingsboardException 共享设备查询失败时抛出
     */
    private List<VisibleDeviceContextBO> findVisibleEnergyDevices(SecurityUser user)
            throws ThingsboardException {
        // 共享服务负责平台权限、设备可见性和 productCategory=energy_regulation 属性筛选。
        List<VisibleDeviceContextBO> devices = deviceContextService.findVisibleDeviceContexts(
                user, ENERGY_REGULATION_PRODUCT_CATEGORY);
        // 将共享服务可能返回的 null 统一为可安全遍历的空集合。
        return devices == null ? Collections.emptyList() : devices;
    }

    /** 按固定并发上限读取设备时序；单台设备失败会在响应中显式计数。
     * @param user 发起查询的当前安全用户
     * @param devices 当前用户可见的能源监管设备
     * @param queries 每台设备共用的累计电能查询条件
     * @return 按设备顺序排列的遥测结果及单设备失败数
     * @throws ThingsboardException 批次等待被中断或批量查询失败时抛出
     */
    private TelemetryBatchResult readEnergyTelemetryInBatches(
            SecurityUser user,
            List<VisibleDeviceContextBO> devices,
            List<ReadTsKvQuery> queries) throws ThingsboardException {
        // 结果列表按设备输入顺序保存，使计算器可以稳定地将结果对应回设备。
        List<List<TsKvEntry>> entriesByDevice = new ArrayList<>(devices.size());
        int failedDeviceCount = 0;
        for (int batchStart = 0; batchStart < devices.size(); batchStart += TELEMETRY_BATCH_SIZE) {
            // 限制同时提交的设备数，避免设备规模扩大时产生无界并发查询。
            int batchEnd = Math.min(batchStart + TELEMETRY_BATCH_SIZE, devices.size());
            List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
            for (int index = batchStart; index < batchEnd; index++) {
                VisibleDeviceContextBO device = devices.get(index);
                // 当前批次内每台设备共享同一组键和时间窗口查询条件。
                futures.add(timeseriesService.findAll(
                        user.getTenantId(), new DeviceId(device.getDeviceId()), queries));
            }
            try {
                // 单台失败以 null 结果呈现，不丢弃同批次其他设备成功返回的数据。
                List<List<TsKvEntry>> batchEntries = Futures.successfulAsList(futures).get(30, java.util.concurrent.TimeUnit.SECONDS);
                for (List<TsKvEntry> entries : batchEntries) {
                    if (entries == null) {
                        // null 表示查询失败，和成功但无遥测的空集合分别统计。
                        failedDeviceCount++;
                        entriesByDevice.add(Collections.emptyList());
                    } else {
                        // 成功结果即使为空也按正常设备结果保留。
                        entriesByDevice.add(entries);
                    }
                }
            } catch (InterruptedException e) {
                // 恢复线程中断标记，供上层调用链正确感知取消信号。
                Thread.currentThread().interrupt();
                throw new ThingsboardException("读取能源监管用电遥测被中断", e, ThingsboardErrorCode.DATABASE);
            } catch (java.util.concurrent.TimeoutException e) {
                for (ListenableFuture<List<TsKvEntry>> future : futures) {
                    future.cancel(true);
                }
                throw new ThingsboardException("读取能源监管遥测超时", e, ThingsboardErrorCode.DATABASE);
            } catch (ExecutionException e) {
                // 批次等待本身失败时按平台数据库异常统一向上抛出。
                throw new ThingsboardException("读取能源监管用电遥测失败", e, ThingsboardErrorCode.DATABASE);
            }
        }
        return new TelemetryBatchResult(entriesByDevice, failedDeviceCount);
    }

    /** 每批设备的遥测结果及读取失败数，供聚合层区分缺数和查询失败。
     * @param entriesByDevice 按输入设备顺序排列的各设备遥测记录
     * @param failedDeviceCount 遥测查询失败的设备数量
     */
    private record TelemetryBatchResult(List<List<TsKvEntry>> entriesByDevice, int failedDeviceCount) {
    }
}
