import {Injectable} from '@angular/core';
import {AttributeService} from '@core/http/attribute.service';
import {DeviceId} from '@shared/models/id/device-id';
import {DataSortOrder, TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {AggregationType} from '@shared/models/time/time.models';
import {Observable} from 'rxjs';
import {SMART_LIGHTNING_WAVEFORM_KEY} from '../shared/telemetry/smart-lightning-measurements';

/** 历史波形通过平台遥测接口读取，设备访问权限沿用平台校验，不另建后端接口。 */
@Injectable({providedIn: 'root'})
export class SmartLightningWaveformHistoryApiService {
  constructor(private readonly attributeService: AttributeService) {}

  /** 按上报时间倒序分页读取完整 WF；时间范围为 [startTs, endTs)，禁用聚合以保留每次事件。 */
  getHistory(deviceId: string, startTs: number, endTs: number, limit: number): Observable<TimeseriesData> {
    return this.attributeService.getEntityTimeseries(
      new DeviceId(deviceId), [SMART_LIGHTNING_WAVEFORM_KEY], startTs, endTs,
      limit, AggregationType.NONE, undefined, DataSortOrder.DESC, true
    );
  }
}
