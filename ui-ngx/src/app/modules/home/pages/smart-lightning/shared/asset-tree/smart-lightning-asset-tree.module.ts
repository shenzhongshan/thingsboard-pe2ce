import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {SharedModule} from '@shared/shared.module';
import {SmartLightningAssetTreeComponent} from './smart-lightning-asset-tree.component';

@NgModule({
  declarations: [SmartLightningAssetTreeComponent],
  imports: [CommonModule, SharedModule],
  exports: [SmartLightningAssetTreeComponent]
})
export class SmartLightningAssetTreeModule {}
