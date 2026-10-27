/** 文件职责：封装能源监管设备详情使用的平台原生遥测与单设备告警查询。 */
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {AlarmService} from '@core/http/alarm.service';
import {AttributeService} from '@core/http/attribute.service';
import {AlarmInfo, AlarmQuery, AlarmSearchStatus} from '@shared/models/alarm.models';
import {DeviceId} from '@shared/models/id/device-id';
import {PageData} from '@shared/models/page/page-data';
import {TimePageLink} from '@shared/models/page/page-link';
import {Direction} from '@shared/models/page/sort-order';
import {DataSortOrder, TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {AggregationType} from '@shared/models/time/time.models';

/** 只接收通过能源监管设备上下文校验的设备 ID；权限仍由平台服务再次校验。 */
@Injectable({providedIn: 'root'})
export class EnergyRegulationDeviceDetailApiService {
  constructor(
    private readonly attributeService: AttributeService,
    private readonly alarmService: AlarmService
  ) {}

  /** 一次读取当前ESA设备已确认的规范遥测键最新值，缺失键由页面显示 --。 */
  getLatest(deviceId: string, keys: string[]): Observable<TimeseriesData> {
    return this.attributeService.getEntityTimeseriesLatest(new DeviceId(deviceId), keys);
  }

  /** 按独立窗口读取每键最近1000条原始采样；不做聚合，页面明确标注数量上限。 */
  getHistory(deviceId: string, keys: string[], startTs: number, endTs: number): Observable<TimeseriesData> {
    return this.attributeService.getEntityTimeseries(
      new DeviceId(deviceId), keys, startTs, endTs, 1000, AggregationType.NONE, undefined, DataSortOrder.DESC, true
    );
  }

  /** 查询本设备最近十条告警，包括活动和已清除事件。 */
  getDeviceAlarms(deviceId: string): Observable<PageData<AlarmInfo>> {
    const query = new AlarmQuery(
      new DeviceId(deviceId),
      new TimePageLink(10, 0, null, {property: 'createdTime', direction: Direction.DESC}),
      AlarmSearchStatus.ANY,
      null,
      false
    );
    return this.alarmService.getAlarms(query);
  }
}
