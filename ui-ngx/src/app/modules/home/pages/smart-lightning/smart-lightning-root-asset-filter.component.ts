/** 文件职责：智能防雷页面公共层模块的 smart-lightning-root-asset-filter.component，负责页面状态管理、接口调用与用户交互。 */
import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {SmartLightningMonitoringAssetNode} from './monitoring/smart-lightning-monitoring.data';

/**
 * 智能防雷页面共用的根资产选择器。
 * 组件只负责展示和派发选择值，具体的树节点递归及列表筛选由宿主页面处理。
 */
@Component({
  selector: 'tb-smart-lightning-root-asset-filter',
  templateUrl: './smart-lightning-root-asset-filter.component.html',
  styleUrls: ['./smart-lightning-root-asset-filter.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true
})
export class SmartLightningRootAssetFilterComponent {
  @Input() roots: SmartLightningMonitoringAssetNode[] = [];
  @Input() selectedRootAssetId = '';
  @Output() rootAssetChange = new EventEmitter<string>();

  onSelectionChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.rootAssetChange.emit(select.value);
  }
}
/** 文件职责：提供智能防雷页面共用的根资产筛选组件，并向宿主页面派发筛选结果。 */
