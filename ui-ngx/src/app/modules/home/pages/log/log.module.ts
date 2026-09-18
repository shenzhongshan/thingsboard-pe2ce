/** 文件职责：设备日志自定义页面模块，集中声明列表和右侧详情抽屉。 */
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { SharedModule } from '@app/shared/shared.module';
import { LogComponent } from './log.component';
import { LogDetailDrawerComponent } from './log-detail-drawer.component';
import { LogRoutingModule } from './log-routing.module';

@NgModule({
  declarations: [LogComponent, LogDetailDrawerComponent],
  imports: [CommonModule, FormsModule, SharedModule, LogRoutingModule]
})
export class LogModule {}
