/** 文件职责：管理电气安全数据监测页的资产选择、设备筛选、视图切换和分页状态。 */
import {ChangeDetectionStrategy, Component} from '@angular/core';
import {
  getSmartLightningDefaultExpandedAssetIds,
  getSmartLightningMonitoringRootAssetIds,
  getSmartLightningMonitoringSelectedAssetIds,
  SmartLightningMonitoringAssetNode
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';
import {
  buildElectricalSafetyMonitoringScopeSummary,
  ELECTRICAL_SAFETY_MONITORING_ASSET_TREE,
  ELECTRICAL_SAFETY_MONITORING_CATEGORIES,
  ELECTRICAL_SAFETY_MONITORING_DEVICES_BY_PRODUCT_CATEGORY,
  ELECTRICAL_SAFETY_MONITORING_SUMMARY_CARDS,
  ElectricalSafetyMonitoringCategoryKey,
  ElectricalSafetyMonitoringDevice,
  filterElectricalSafetyMonitoringDevices
} from './electrical-safety-monitoring.data';

/** 电气安全数据监测页面；当前版本只维护 UI 展示状态，不调用后端接口。 */
@Component({
  selector: 'tb-electrical-safety-monitoring',
  templateUrl: './electrical-safety-monitoring.component.html',
  styleUrls: ['./electrical-safety-monitoring.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ElectricalSafetyMonitoringComponent {
  /** 共享资产树组件使用的本地页面树；后续接入真实接口时替换此数据源即可。 */
  readonly assetTree = ELECTRICAL_SAFETY_MONITORING_ASSET_TREE;
  readonly categories = ELECTRICAL_SAFETY_MONITORING_CATEGORIES;
  readonly summaryCards = ELECTRICAL_SAFETY_MONITORING_SUMMARY_CARDS;
  readonly pageSizeOptions = [6, 9, 12];

  /** 初始范围选择全部根资产，并展开首两层以便快速定位楼栋。 */
  selectedRootAssetId = '';
  selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, ''));
  expandedAssetIds = getSmartLightningDefaultExpandedAssetIds(this.assetTree);
  selectedAssetName = '全部资产';
  assetSearch = '';

  /** 分类、名称搜索、卡片/列表和分页状态由本页面独立维护。 */
  activeCategory: ElectricalSafetyMonitoringCategoryKey = 'all';
  deviceSearch = '';
  viewMode: 'grid' | 'list' = 'grid';
  page = 1;
  pageSize = 6;

  /** 返回当前资产子树中的设备，供统计、筛选和分页共用同一范围。 */
  get scopedDevices(): ElectricalSafetyMonitoringDevice[] {
    return ELECTRICAL_SAFETY_MONITORING_DEVICES_BY_PRODUCT_CATEGORY.filter(
      (device) => this.selectedAssetIds.has(device.assetId)
    );
  }

  /** 当前资产范围内的告警和在线统计。 */
  get scopeSummary() {
    return buildElectricalSafetyMonitoringScopeSummary(this.scopedDevices);
  }

  /** 按资产、分类和名称关键词筛选设备；过滤变化后总是从第一页显示。 */
  get filteredDevices(): ElectricalSafetyMonitoringDevice[] {
    return filterElectricalSafetyMonitoringDevices(
      ELECTRICAL_SAFETY_MONITORING_DEVICES_BY_PRODUCT_CATEGORY,
      this.selectedAssetIds,
      this.activeCategory,
      this.deviceSearch
    );
  }

  /** 当前页卡片；筛选结果为空时返回空数组并由模板显示空状态。 */
  get pagedDevices(): ElectricalSafetyMonitoringDevice[] {
    const startIndex = (this.page - 1) * this.pageSize;
    return this.filteredDevices.slice(startIndex, startIndex + this.pageSize);
  }

  /** 页数至少为一页，保证分页按钮边界始终有稳定状态。 */
  get pageCount(): number {
    return Math.max(1, Math.ceil(this.filteredDevices.length / this.pageSize));
  }

  /** 当前结果集在页脚中的显示起止位置。 */
  get rangeStart(): number {
    return this.filteredDevices.length ? (this.page - 1) * this.pageSize + 1 : 0;
  }

  /** 当前页最后一条记录位置；结果为空时显示零。 */
  get rangeEnd(): number {
    return Math.min(this.page * this.pageSize, this.filteredDevices.length);
  }

  /** 根公司变化后同步根筛选、树节点选中范围与标题。 */
  selectRootAsset(rootAssetId: string): void {
    this.selectedRootAssetId = rootAssetId;
    this.selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    const root = this.assetTree.roots.find((node) => node.id === rootAssetId);
    this.selectedAssetName = root?.name || '全部资产';
    this.resetDevicePage();
  }

  /** 选择资产节点后纳入该节点的全部下级，并同步页面概况标题。 */
  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name;
    this.resetDevicePage();
  }

  /** 展开或收起树分支时替换集合引用，触发共享组件更新。 */
  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    const nextExpandedIds = new Set(this.expandedAssetIds);
    if (nextExpandedIds.has(node.id)) {
      nextExpandedIds.delete(node.id);
    } else {
      nextExpandedIds.add(node.id);
    }
    this.expandedAssetIds = nextExpandedIds;
  }

  /** 将选择恢复到当前公司根节点或全部资产。 */
  selectAllAssets(): void {
    this.selectRootAsset(this.selectedRootAssetId);
  }

  /** 切换分类后回到第一页，避免页码超出筛选结果范围。 */
  selectCategory(category: ElectricalSafetyMonitoringCategoryKey): void {
    this.activeCategory = category;
    this.resetDevicePage();
  }

  /** 搜索框变化和清除操作共用此方法以同步分页位置。 */
  resetDevicePage(): void {
    this.page = 1;
  }

  /** 更新每页条数并回到第一页。 */
  setPageSize(value: string): void {
    const nextPageSize = Number(value);
    if (Number.isFinite(nextPageSize) && nextPageSize > 0) {
      this.pageSize = nextPageSize;
      this.resetDevicePage();
    }
  }

  /** 在有效页码范围内切换上一页或下一页。 */
  movePage(offset: number): void {
    this.page = Math.min(this.pageCount, Math.max(1, this.page + offset));
  }
}
