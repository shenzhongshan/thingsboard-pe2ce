/** 文件职责：呈现电气火灾、安全用电和电力质量三类设备状态卡片。 */
import {Component} from '@angular/core';
import {SmartLightningPeriod} from '../../smart-lightning/dashboard/smart-lightning-dashboard.data';
import type {SmartLightningMapDevice} from '../../smart-lightning/shared/map/smart-lightning-device-map.models';
import type {DeviceAlarmDisplayColumn, DeviceAlarmDisplayRow} from '@home/pages/deviceAlarms/shared/alarm-list/device-alarm-records.model';
import {
  buildElectricalSafetyDashboardCards,
  ELECTRICAL_SAFETY_ALARM_COLUMNS,
  ELECTRICAL_SAFETY_DASHBOARD_DATA,
  ELECTRICAL_SAFETY_MAP_DEVICES,
  ElectricalSafetyDashboardCard,
  mapElectricalSafetyAlarmRecords
} from './electrical-safety-dashboard.data';

@Component({
  selector: 'tb-electrical-safety-dashboard',
  templateUrl: './electrical-safety-dashboard.component.html',
  styleUrls: ['./electrical-safety-dashboard.component.scss'],
  standalone: false
})
export class ElectricalSafetyDashboardComponent {
  /** 整体设备统计、图表、告警记录和地图点位均由页面样例数据提供。 */
  readonly dashboardData = ELECTRICAL_SAFETY_DASHBOARD_DATA;
  /** 告警列表使用共享展示组件；数据仍来自当前看板已有样例记录。 */
  readonly alarmRecords: DeviceAlarmDisplayRow[] = mapElectricalSafetyAlarmRecords(this.dashboardData.alarmRecords);
  /** 保留电气安全看板原来的告警列名称、顺序与列宽。 */
  readonly alarmRecordColumns: readonly DeviceAlarmDisplayColumn[] = ELECTRICAL_SAFETY_ALARM_COLUMNS;
  /** 设备点位复用防雷看板的公共地图绘制组件。 */
  readonly mapDevices: SmartLightningMapDevice[] = ELECTRICAL_SAFETY_MAP_DEVICES;
  /** 防雷公共地图组件保持通用；由本页面按电气安全产品分类筛选点位。 */
  readonly includeElectricalSafetyDevice = (device: SmartLightningMapDevice): boolean =>
    device.productCategory?.trim().toLowerCase() === 'electrical_safety';
  /** 看板按设备类别展示在线、正常、离线、告警数量和对应占比。 */
  readonly monitoringCards: ElectricalSafetyDashboardCard[] = buildElectricalSafetyDashboardCards();
  /** 报警图表独立维护周期，避免影响运维工单图表。 */
  alarmPeriod = SmartLightningPeriod.day;
  /** 运维工单图表独立维护周期，避免影响报警图表。 */
  workOrderPeriod = SmartLightningPeriod.day;

  /** 使用稳定的类别键追踪卡片，避免刷新时重复创建卡片 DOM。 */
  trackByCategory(_index: number, card: ElectricalSafetyDashboardCard): string {
    return card.key;
  }

  /** 更新报警信息图的展示周期。 */
  setAlarmPeriod(period: SmartLightningPeriod): void {
    this.alarmPeriod = period;
  }

  /** 更新运维工单图的展示周期。 */
  setWorkOrderPeriod(period: SmartLightningPeriod): void {
    this.workOrderPeriod = period;
  }
}
