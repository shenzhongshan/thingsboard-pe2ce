/** 文件职责：共享资产树模块的 smart-lightning-asset-tree.module，声明 Angular 模块、组件与依赖导入。 */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {SharedModule} from '@shared/shared.module';
import {SmartLightningAssetTreeComponent} from './smart-lightning-asset-tree.component';

/** 声明并导出共享资产树组件，供智能防雷各页面模块导入。 */
@NgModule({
  declarations: [SmartLightningAssetTreeComponent],
  imports: [CommonModule, SharedModule],
  exports: [SmartLightningAssetTreeComponent]
})
export class SmartLightningAssetTreeModule {}
