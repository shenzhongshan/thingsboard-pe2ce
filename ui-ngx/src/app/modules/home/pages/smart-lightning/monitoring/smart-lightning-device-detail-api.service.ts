/** 文件职责：设备详情页趋势数据的请求适配，只负责调用 ThingsBoard 遥测接口。 */
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {AttributeService} from '@core/http/attribute.service';
import {EntityService} from '@core/http/entity.service';
import {DeviceId} from '@shared/models/id/device-id';
import {DataKeyType, DataSortOrder, TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {AggregationType} from '@shared/models/time/time.models';

@Injectable({providedIn: 'root'})
export class SmartLightningDeviceDetailApiService {
  constructor(
    private readonly entityService: EntityService,
    private readonly attributeService: AttributeService
  ) {}

  /** 读取设备真实存在的遥测键，避免为设备不存在的曲线发起历史查询。 */
  getTimeseriesKeys(deviceId: string): Observable<string[]> {
    return this.entityService.getEntityKeys(new DeviceId(deviceId), '', DataKeyType.timeseries);
  }

  /** 按单张图的日/周/月时间窗口批量读取历史遥测。 */
  getHistory(deviceId: string, keys: string[], startTs: number, endTs: number): Observable<TimeseriesData> {
    return this.attributeService.getEntityTimeseries(
      new DeviceId(deviceId), keys, startTs, endTs, 1000, AggregationType.NONE, undefined, DataSortOrder.ASC, true
    );
  }
}
