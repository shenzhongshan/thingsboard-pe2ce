import {Injectable} from '@angular/core';

/** 只保存树的交互状态，不缓存设备、遥测、告警或权限查询结果。 */
export interface AssetTreeSelectionState {
  rootAssetId: string;
  selectedNodeId: string;
  expandedIds: readonly string[];
}

/** 按业务页面隔离树状态，支持从详情经顶部导航或浏览器返回后恢复选择。 */
@Injectable({providedIn: 'root'})
export class AssetTreeSelectionStateService {
  private readonly states = new Map<string, AssetTreeSelectionState>();

  /** 离开页面时保存副本；状态仅驻留本次应用会话，读取方仍须用最新权限树校验节点。 */
  save(page: string, state: AssetTreeSelectionState): void {
    this.states.set(page, {...state, expandedIds: [...state.expandedIds]});
  }

  /** 返回副本，避免某个页面修改共享状态后影响其他页面的恢复结果。 */
  get(page: string): AssetTreeSelectionState | undefined {
    const state = this.states.get(page);
    return state ? {...state, expandedIds: [...state.expandedIds]} : undefined;
  }
}
