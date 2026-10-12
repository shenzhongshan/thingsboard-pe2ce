/** 文件职责：管理电气安全数据查询页的资产范围、筛选条件、表格选择和分页状态。 */
import {ChangeDetectionStrategy, Component} from '@angular/core';
import {
  getSmartLightningDefaultExpandedAssetIds,
  getSmartLightningMonitoringRootAssetIds,
  getSmartLightningMonitoringSelectedAssetIds,
  SmartLightningMonitoringAssetNode
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';
import {
  ElectricalSafetyQueryRow,
  ElectricalSafetyQueryStatus,
  filterElectricalSafetyQueryRows,
  ELECTRICAL_SAFETY_QUERY_ROWS
} from './electrical-safety-data-query.data';
import {ELECTRICAL_SAFETY_MONITORING_ASSET_TREE} from '../monitoring/electrical-safety-monitoring.data';

/** 电气安全数据查询页；当前用页面已有的电气安全设备数据呈现完整查询 UI。 */
@Component({
  selector: 'tb-electrical-safety-data-query',
  templateUrl: './electrical-safety-data-query.component.html',
  styleUrls: ['./electrical-safety-data-query.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ElectricalSafetyDataQueryComponent {
  /** 左侧资产树与监测页共用同一份电气安全分类范围。 */
  readonly assetTree = ELECTRICAL_SAFETY_MONITORING_ASSET_TREE;
  readonly statuses: Array<'全部' | ElectricalSafetyQueryStatus> = ['全部', '报警', '离线', '正常'];
  readonly pageSize = 20;

  /** 默认选择全部根资产，并展开树的首层，供页面首次浏览设备。 */
  selectedRootAssetId = '';
  selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, ''));
  expandedAssetIds = getSmartLightningDefaultExpandedAssetIds(this.assetTree);
  selectedAssetName = '全部根资产';
  assetSearch = '';

  /** 查询表单状态在本页独立维护，重置筛选不会清除资产树范围。 */
  productNameFilter = '';
  functionType = '';
  keyword = '';
  activeStatus: '全部' | ElectricalSafetyQueryStatus = '全部';
  page = 1;
  selectedRowIds = new Set<string>();

  /** 当前样例设备内存在报警的设备 ID，供共享资产树沿用防雷页的标记样式。 */
  readonly activeAlarmDeviceIds = new Set(
    ELECTRICAL_SAFETY_QUERY_ROWS.filter((row) => row.status === '报警').map((row) => row.id)
  );

  /** 返回资产树当前范围内的设备行，产品分类在数据定义中已限制为 electrical_safety。 */
  get scopedRows(): ElectricalSafetyQueryRow[] {
    return ELECTRICAL_SAFETY_QUERY_ROWS.filter((row) => this.selectedAssetIds.has(row.assetId));
  }

  /** 按资产范围、状态、产品、功能和关键词筛选当前设备行。 */
  get filteredRows(): ElectricalSafetyQueryRow[] {
    return filterElectricalSafetyQueryRows(ELECTRICAL_SAFETY_QUERY_ROWS, this.selectedAssetIds, {
      status: this.activeStatus,
      productName: this.productNameFilter,
      functionCategory: this.functionType,
      keyword: this.keyword
    });
  }

  /** 当前页展示行，供序号、全选和表格正文共用。 */
  get pageRows(): ElectricalSafetyQueryRow[] {
    return this.filteredRows.slice((this.page - 1) * this.pageSize, this.page * this.pageSize);
  }

  /** 查询结果至少显示一页，空结果时分页按钮仍保持稳定状态。 */
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize));
  }

  /** 从当前资产范围生成产品名称选项，避免筛选出范围外的设备类型。 */
  get productNames(): string[] {
    return [...new Set(this.scopedRows.map((row) => row.productName))]
      .sort((left, right) => left.localeCompare(right, 'zh-CN'));
  }

  /** 从当前资产范围生成设备功能分类选项。 */
  get functionCategories(): string[] {
    return [...new Set(this.scopedRows.map((row) => row.functionCategory))]
      .sort((left, right) => left.localeCompare(right, 'zh-CN'));
  }

  /** 判断当前页是否已全部勾选。 */
  get allPageRowsSelected(): boolean {
    return this.pageRows.length > 0 && this.pageRows.every((row) => this.selectedRowIds.has(row.id));
  }

  /** 根资产变化后同步资产树选择范围，并清空旧的行选择。 */
  selectRootAsset(rootAssetId: string): void {
    this.selectedRootAssetId = rootAssetId;
    this.selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    const root = this.assetTree.roots.find((node) => node.id === rootAssetId);
    this.selectedAssetName = root?.name || '全部根资产';
    this.clearSelectionAndResetPage();
  }

  /** 选择资产节点及其全部后代，确保表格和筛选下拉项使用同一资产范围。 */
  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name;
    this.clearSelectionAndResetPage();
  }

  /** 展开或收起资产分支时替换集合引用，触发 OnPush 页面更新。 */
  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    const nextExpandedIds = new Set(this.expandedAssetIds);
    if (nextExpandedIds.has(node.id)) {
      nextExpandedIds.delete(node.id);
    } else {
      nextExpandedIds.add(node.id);
    }
    this.expandedAssetIds = nextExpandedIds;
  }

  /** 全选当前根资产范围内的节点，不会跨越根资产筛选边界。 */
  selectAllAssets(): void {
    this.selectRootAsset(this.selectedRootAssetId);
  }

  /** 切换结果状态并回到第一页，避免当前页超出筛选结果。 */
  setStatus(status: '全部' | ElectricalSafetyQueryStatus): void {
    this.activeStatus = status;
    this.clearSelectionAndResetPage();
  }

  /** 重置表单和状态页签，保留左侧当前资产范围。 */
  reset(): void {
    this.productNameFilter = '';
    this.functionType = '';
    this.keyword = '';
    this.activeStatus = '全部';
    this.clearSelectionAndResetPage();
  }

  /** 将页码限制在当前结果的有效范围内。 */
  setPage(page: number): void {
    this.page = Math.min(Math.max(1, page), this.totalPages);
  }

  /** 勾选或取消勾选单台设备，并创建新集合触发变更检测。 */
  toggleRow(rowId: string): void {
    const nextSelectedRowIds = new Set(this.selectedRowIds);
    if (nextSelectedRowIds.has(rowId)) {
      nextSelectedRowIds.delete(rowId);
    } else {
      nextSelectedRowIds.add(rowId);
    }
    this.selectedRowIds = nextSelectedRowIds;
  }

  /** 对当前页执行全选或取消全选，不影响其他页已选行。 */
  togglePageRows(): void {
    const nextSelectedRowIds = new Set(this.selectedRowIds);
    for (const row of this.pageRows) {
      if (this.allPageRowsSelected) {
        nextSelectedRowIds.delete(row.id);
      } else {
        nextSelectedRowIds.add(row.id);
      }
    }
    this.selectedRowIds = nextSelectedRowIds;
  }

  /** 导出已勾选设备；未勾选时导出当前筛选结果，文件编码兼容常见表格软件。 */
  exportRows(): void {
    const exportRows = this.selectedRowIds.size
      ? this.filteredRows.filter((row) => this.selectedRowIds.has(row.id))
      : this.filteredRows;
    if (!exportRows.length) {
      return;
    }
    const escapeCsvCell = (value: string): string => `"${value.replace(/"/g, '""')}"`;
    const lines = [
      ['序号', '设备名称', '设备类型', '设备协议', '状态', '产品名称', '所属资产', '最近上报时间'],
      ...exportRows.map((row, index) => [
        String(index + 1), row.deviceName, row.deviceType, row.protocol, row.status,
        row.productName, row.assetPath, row.lastActivityText
      ])
    ];
    const csvContent = `\uFEFF${lines.map((line) => line.map(escapeCsvCell).join(',')).join('\r\n')}`;
    const csvBlob = new Blob([csvContent], {type: 'text/csv;charset=utf-8'});
    const objectUrl = URL.createObjectURL(csvBlob);
    const downloadLink = document.createElement('a');
    downloadLink.href = objectUrl;
    downloadLink.download = `电气安全数据查询-${Date.now()}.csv`;
    downloadLink.click();
    URL.revokeObjectURL(objectUrl);
  }

  /** 清空勾选并回到第一页，供范围变化和筛选变化共用。 */
  private clearSelectionAndResetPage(): void {
    this.selectedRowIds = new Set<string>();
    this.page = 1;
  }
}
