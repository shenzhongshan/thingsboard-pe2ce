/** 文件职责：集中声明并导出防雷及其他设备模块共用的历史遥测回顾弹窗。 */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {SharedModule} from '@app/shared/shared.module';
import {SmartLightningHistoryReviewDialogComponent} from './smart-lightning-history-review-dialog.component';

@NgModule({
  declarations: [SmartLightningHistoryReviewDialogComponent],
  imports: [CommonModule, FormsModule, SharedModule],
  exports: [SmartLightningHistoryReviewDialogComponent]
})
export class SmartLightningHistoryReviewDialogModule {}
