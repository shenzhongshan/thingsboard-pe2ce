import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {
  getSmartLightningMonitoringVisibleRoots,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree
} from '../../monitoring/smart-lightning-monitoring.data';

@Component({
  selector: 'tb-smart-lightning-asset-tree',
  templateUrl: './smart-lightning-asset-tree.component.html',
  styleUrls: ['./smart-lightning-asset-tree.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAssetTreeComponent {
  @Input() tree: SmartLightningMonitoringAssetTree = {roots: [], nodes: new Map()};
  @Input() selectedIds: ReadonlySet<string> = new Set<string>();
  @Input() expandedIds: ReadonlySet<string> = new Set<string>();
  @Input() rootAssetId = '';
  @Input() search = '';
  @Input() loading = false;
  @Input() loadError = false;
  @Input() alarmDeviceIds: ReadonlySet<string> = new Set<string>();
  @Input() selectableIds: ReadonlySet<string> | null = null;
  @Input() searchPlaceholder = '搜索资产名称';
  @Input() treeAriaLabel = '资产层级';

  @Output() readonly searchChange = new EventEmitter<string>();
  @Output() readonly selectNode = new EventEmitter<SmartLightningMonitoringAssetNode>();
  @Output() readonly toggleNode = new EventEmitter<SmartLightningMonitoringAssetNode>();

  get visibleAssetNodes(): Array<{node: SmartLightningMonitoringAssetNode; depth: number}> {
    const result: Array<{node: SmartLightningMonitoringAssetNode; depth: number}> = [];
    const query = this.search.trim().toLowerCase();
    const hasVisibleDescendant = (node: SmartLightningMonitoringAssetNode): boolean =>
      node.name.toLowerCase().includes(query) || node.childIds.some((id) => {
        const child = this.tree.nodes.get(id);
        return child ? hasVisibleDescendant(child) : false;
      });
    const append = (node: SmartLightningMonitoringAssetNode, depth: number): void => {
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

  isSelectable(node: SmartLightningMonitoringAssetNode): boolean {
    return !this.selectableIds || this.selectableIds.has(node.id);
  }

  onSearchInput(event: Event): void {
    this.searchChange.emit((event.target as HTMLInputElement).value);
  }

  onSelectNode(node: SmartLightningMonitoringAssetNode): void {
    if (this.isSelectable(node)) {
      this.selectNode.emit(node);
    }
  }

  onToggleNode(event: Event, node: SmartLightningMonitoringAssetNode): void {
    event.preventDefault();
    event.stopPropagation();
    this.toggleNode.emit(node);
  }
}
