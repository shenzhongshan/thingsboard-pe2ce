/** 文件职责：共享资产树模块的 smart-lightning-asset-tree.component，负责页面状态管理、接口调用与用户交互。 */
import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {
  getSmartLightningAlarmAssetIds,
  getSmartLightningMonitoringVisibleRoots,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree
} from '../../monitoring/smart-lightning-monitoring.data';

/**
 * 智能防雷模块共用的资产树组件。
 *
 * <p>采用受控组件模式：组件只负责渲染（搜索过滤、展开/折叠、选中高亮、
 * 告警标记、锁定置灰）和事件派发，树数据与展开/选中状态全部由宿主页面持有，
 * 保证监测、查询、告警、详情四个页面的树交互完全一致。</p>
 */
@Component({
  selector: 'tb-smart-lightning-asset-tree',
  templateUrl: './smart-lightning-asset-tree.component.html',
  styleUrls: ['./smart-lightning-asset-tree.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAssetTreeComponent {
  /** 资产树数据（索引结构），由宿主页面的树接口返回值构建。 */
  @Input() tree: SmartLightningMonitoringAssetTree = {roots: [], nodes: new Map()};
  /** 当前选中的节点 ID 集合（含选中资产的全部后代）。 */
  @Input() selectedIds: ReadonlySet<string> = new Set<string>();
  /** 当前展开的资产节点 ID 集合。 */
  @Input() expandedIds: ReadonlySet<string> = new Set<string>();
  /** 根资产筛选值；为空时显示全部根节点，否则只显示对应根子树。 */
  @Input() rootAssetId = '';
  /** 搜索关键词（受控），宿主负责保存与回显。 */
  @Input() search = '';
  /** 资产树加载中标记。 */
  @Input() loading = false;
  /** 资产树加载失败标记，失败时显示错误空状态。 */
  @Input() loadError = false;
  /** 存在未清除告警的设备 ID 集合，用于树节点红/绿状态条。 */
  @Input() alarmDeviceIds: ReadonlySet<string> = new Set<string>();
  /** 普通监测/查询页隐藏设备行；详情页打开此项以便定位具体设备。 */
  @Input() showDeviceNodes = true;
  /** 详情页需要在设备未选中其父资产时仍显示直属告警标记；普通页保持选中后显示。 */
  @Input() showUnselectedAlarmMarkers = false;
  /** 可选中节点白名单；null 表示全部可选（详情页锁定模式只放行当前设备）。 */
  @Input() selectableIds: ReadonlySet<string> | null = null;
  /** 搜索框占位文案。 */
  @Input() searchPlaceholder = '搜索资产名称';
  /** 树容器的无障碍标签。 */
  @Input() treeAriaLabel = '资产层级';

  /** 搜索词变化事件，宿主负责更新受控的 search 值。 */
  @Output() readonly searchChange = new EventEmitter<string>();
  /** 节点点击（选中）事件，只派发可选中节点。 */
  @Output() readonly selectNode = new EventEmitter<SmartLightningMonitoringAssetNode>();
  /** 展开/折叠事件，宿主负责维护 expandedIds。 */
  @Output() readonly toggleNode = new EventEmitter<SmartLightningMonitoringAssetNode>();
  /** 一键选择当前资产树范围及其全部下属节点，宿主负责更新选中集合。 */
  @Output() readonly selectAllNodes = new EventEmitter<void>();

  /** 资产告警标记集合缓存，避免每次变更检测都重算。 */
  private alarmAssetIdsCache = new Set<string>();
  /** 缓存对应的树引用，树对象变化时缓存失效。 */
  private alarmAssetIdsCacheTree: SmartLightningMonitoringAssetTree | null = null;
  /** 缓存对应的设备告警集合引用，集合变化时缓存失效。 */
  private alarmAssetIdsCacheDevices: ReadonlySet<string> | null = null;

  /**
   * 按展开状态与搜索词计算当前应渲染的树节点列表。
   *
   * <p>搜索时自动展开并保留所有命中关键词的子树；
   * 普通状态下只渲染 expandedIds 中的资产分支；
   * showDeviceNodes 为 false 时整体隐藏设备行。</p>
   */
  get visibleAssetNodes(): Array<{node: SmartLightningMonitoringAssetNode; depth: number}> {
    const result: Array<{node: SmartLightningMonitoringAssetNode; depth: number}> = [];
    const query = this.search.trim().toLowerCase();
    const hasVisibleDescendant = (node: SmartLightningMonitoringAssetNode): boolean =>
      (this.isNodeVisible(node) && node.name.toLowerCase().includes(query)) || node.childIds.some((id) => {
        const child = this.tree.nodes.get(id);
        return child ? hasVisibleDescendant(child) : false;
      });
    const append = (node: SmartLightningMonitoringAssetNode, depth: number): void => {
      if (!this.isNodeVisible(node)) {
        return;
      }
      if (query && !hasVisibleDescendant(node)) {
        return;
      }
      result.push({node, depth});
      const shouldShowChildren = !!query || this.expandedIds.has(node.id);
      if (shouldShowChildren) {
        node.childIds.forEach((id) => {
          const child = this.tree.nodes.get(id);
          if (child) {
            append(child, depth + 1);
          }
        });
      }
    };
    getSmartLightningMonitoringVisibleRoots(this.tree, this.rootAssetId).forEach((node) => append(node, 0));
    return result;
  }

  /** 带缓存的资产告警标记集合：树或设备告警集合变化时才重算。 */
  get alarmAssetIds(): ReadonlySet<string> {
    if (this.alarmAssetIdsCacheTree !== this.tree || this.alarmAssetIdsCacheDevices !== this.alarmDeviceIds) {
      this.alarmAssetIdsCache = getSmartLightningAlarmAssetIds(this.tree, this.alarmDeviceIds);
      this.alarmAssetIdsCacheTree = this.tree;
      this.alarmAssetIdsCacheDevices = this.alarmDeviceIds;
    }
    return this.alarmAssetIdsCache;
  }

  /** 判断节点是否命中告警：设备看自身告警集合，资产看其下属设备是否有告警。 */
  isAlarmMarked(node: SmartLightningMonitoringAssetNode): boolean {
    return node.entityType === 'DEVICE'
      ? this.alarmDeviceIds.has(node.id)
      : this.alarmAssetIds.has(node.id);
  }

  /** 判断告警标记是否应当展示（结合选中状态与 showUnselectedAlarmMarkers 开关）。 */
  shouldShowAlarmMarker(node: SmartLightningMonitoringAssetNode): boolean {
    if (!this.isAlarmMarked(node)) {
      return false;
    }
    // 详情页允许直属上级资产在未选中时显示；设备自身仍遵循选中后显示，避免整棵树常驻红色。
    return this.selectedIds.has(node.id) ||
      (node.entityType !== 'DEVICE' && this.showUnselectedAlarmMarkers);
  }

  /** 节点是否参与渲染：受 showDeviceNodes 开关控制。 */
  private isNodeVisible(node: SmartLightningMonitoringAssetNode): boolean {
    return this.showDeviceNodes || node.entityType !== 'DEVICE';
  }

  /** 判断节点是否还有可渲染的子节点，决定展开箭头或占位符。 */
  hasVisibleChildren(node: SmartLightningMonitoringAssetNode): boolean {
    return node.childIds.some((childId) => {
      const child = this.tree.nodes.get(childId);
      return !!child && this.isNodeVisible(child);
    });
  }

  /** 节点是否可选中：selectableIds 为 null 时全部可选，否则按白名单锁定。 */
  isSelectable(node: SmartLightningMonitoringAssetNode): boolean {
    return !this.selectableIds || this.selectableIds.has(node.id);
  }

  /** 搜索框输入回调，把输入值派发给宿主。 */
  onSearchInput(event: Event): void {
    this.searchChange.emit((event.target as HTMLInputElement).value);
  }

  /**
   * 判断“全选”按钮是否可用。
   * 详情页通过 selectableIds 锁定了设备定位范围，不允许在该模式下覆盖当前选择。
   */
  canSelectAll(): boolean {
    return !this.selectableIds && !this.loading && !this.loadError && this.tree.roots.length > 0;
  }

  /** 派发全选事件，由宿主按当前根资产筛选范围完成实际选择和数据刷新。 */
  onSelectAllNodes(): void {
    if (this.canSelectAll()) {
      this.selectAllNodes.emit();
    }
  }

  /** 节点点击回调；锁定节点不派发事件。 */
  onSelectNode(node: SmartLightningMonitoringAssetNode): void {
    if (this.isSelectable(node)) {
      this.selectNode.emit(node);
    }
  }

  /** 展开/折叠回调；阻止事件冒泡，避免触发节点选中。 */
  onToggleNode(event: Event, node: SmartLightningMonitoringAssetNode): void {
    event.preventDefault();
    event.stopPropagation();
    this.toggleNode.emit(node);
  }
}
