import {Injectable} from '@angular/core';
import {defer, firstValueFrom, forkJoin, map, Observable} from 'rxjs';
import {CustomMenuService} from './custom-menu.service';
import {PageLink} from '@shared/models/page/page-link';
import {CMAssigneeType, CustomMenu} from '@shared/models/custom-menu.models';
import {
  canonicalCustomMenuConfig, createCustomMenuTransfer, CustomMenuTransferError,
  CustomMenuTransferPayload, parseCustomMenuTransfer
} from '@shared/models/custom-menu-transfer.data';

/** 菜单交换流程：复用原有鉴权接口，不读取登录令牌，不改变已有菜单及其分配。 */
@Injectable({providedIn: 'root'})
export class CustomMenuTransferService {
  constructor(private menus: CustomMenuService) {}

  /** 通过菜单ID读取最新名称和完整配置，不能只导出列表行或页面内未保存的草稿。 */
  exportMenu(id: string): Observable<CustomMenuTransferPayload> {
    return forkJoin({
      menu: this.menus.getCustomMenuInfoById(id),
      config: this.menus.getCustomMenuConfigById(id)
    }).pipe(map(({menu, config}) => createCustomMenuTransfer(menu, config)));
  }

  /** 分页检查同范围重名，创建未分配菜单后保存、回读配置；失败只清理本次新建对象。 */
  importMenu(payload: CustomMenuTransferPayload, name: string): Observable<CustomMenu> {
    return defer(async () => {
      const transfer = parseCustomMenuTransfer({...payload, name});
      for (let page = 0; ; page++) {
        const result = await firstValueFrom(this.menus.getCustomMenuInfos(
          new PageLink(100, page), transfer.scope, {ignoreErrors: true}));
        if (result.data.some(menu => menu.name === transfer.name)) {
          throw new CustomMenuTransferError('custom-menu.transfer-name-conflict');
        }
        if (!result.hasNext) {
          break;
        }
      }
      const created = await firstValueFrom(this.menus.createCustomMenu({
        name: transfer.name, scope: transfer.scope, assigneeType: CMAssigneeType.NO_ASSIGN
      }, [], false, {ignoreErrors: true}));
      const id = created.id?.id;
      if (!id) {
        throw new CustomMenuTransferError('custom-menu.transfer-create-unknown');
      }
      try {
        const saved = await firstValueFrom(this.menus.updateCustomMenuConfig(id, transfer.config, {ignoreErrors: true}));
        const config = await firstValueFrom(this.menus.getCustomMenuConfigById(id, {ignoreErrors: true}));
        if (canonicalCustomMenuConfig(config) !== canonicalCustomMenuConfig(transfer.config)) {
          throw new Error('Menu configuration verification failed');
        }
        return saved;
      } catch {
        let cleanupConfirmed = false;
        try {
          // force=false：若菜单已被其他会话分配，后端会阻止删除，不影响现有用户。
          const removed = await firstValueFrom(this.menus.deleteCustomMenu(id, false, {ignoreErrors: true}));
          cleanupConfirmed = removed.success;
        } catch {
          // 清理请求失败不能假装回滚完成，下面携带新菜单ID提示人工核对。
          cleanupConfirmed = false;
        }
        if (cleanupConfirmed) {
          throw new CustomMenuTransferError('custom-menu.transfer-import-failed-cleaned');
        }
        throw new CustomMenuTransferError('custom-menu.transfer-import-partial', id);
      }
    });
  }
}
