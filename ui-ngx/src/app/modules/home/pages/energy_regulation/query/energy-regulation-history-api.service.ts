import {Injectable} from '@angular/core';
import {AttributeService} from '@core/http/attribute.service';
import {DeviceId} from '@shared/models/id/device-id';
import {DataSortOrder} from '@shared/models/telemetry/telemetry.models';
import {AggregationType} from '@shared/models/time/time.models';

/** 能源回顾独立封装平台公共遥测接口，不调用防雷或电气安全的业务服务。 */
@Injectable({providedIn: 'root'})
export class EnergyRegulationHistoryApiService {
  constructor(private readonly attributes: AttributeService) {}
  /** 用户主动打开设备时读取规范键最新值，平台再次校验读取权限。 */
  latest(deviceId: string, keys: string[]) { return this.attributes.getEntityTimeseriesLatest(new DeviceId(deviceId), keys); }
  /** 查询最近24小时原始采样，明确最多显示最近1000条，按时间升序呈现。 */
  history(deviceId: string, key: string) {
    const now = Date.now();
    return this.attributes.getEntityTimeseries(new DeviceId(deviceId), [key], now - 86_400_000, now,
      1000, AggregationType.NONE, undefined, DataSortOrder.DESC, true);
  }
}
