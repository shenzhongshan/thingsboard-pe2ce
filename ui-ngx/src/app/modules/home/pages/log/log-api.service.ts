/**
 * 文件职责：设备日志页面的 HTTP 数据适配层。
 *
 * <p>设备列表使用自定义只读接口；属性和最新遥测直接使用平台公开的遥测接口，
 * 不复制或改造 ThingsBoard 原生设备页面。</p>
 */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeData, AttributeScope, TimeseriesData } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { LogDevicePage, LogTimelinePage } from './log.models';

@Injectable({providedIn: 'root'})
export class LogApiService {

  constructor(private readonly http: HttpClient,
              private readonly attributeService: AttributeService) {}

  /** 查询当前用户可见设备，服务端会再次执行 READ 权限过滤。 */
  getDevices(pageSize: number, page: number, textSearch: string): Observable<LogDevicePage> {
    let url = `/api/device-log/devices?pageSize=${pageSize}&page=${page}`;
    if (textSearch?.trim()) {
      url += `&textSearch=${encodeURIComponent(textSearch.trim())}`;
    }
    return this.http.get<LogDevicePage>(url);
  }

  /** 读取设备 SERVER_SCOPE 属性。 */
  getServerAttributes(entityId: EntityId): Observable<AttributeData[]> {
    return this.attributeService.getEntityAttributes(entityId, AttributeScope.SERVER_SCOPE);
  }

  /** 读取设备最新遥测快照，不在列表页预取，避免无关设备产生额外时序查询。 */
  getLatestTelemetry(entityId: EntityId): Observable<TimeseriesData> {
    return this.attributeService.getEntityTimeseriesLatest(entityId);
  }

  /** 读取最近窗口内的审计和告警事件，遥测逐条更新不在此时间线中。 */
  getRecentTimeline(deviceId: string, days = 30): Observable<LogTimelinePage> {
    return this.http.get<LogTimelinePage>(`/api/device-log/devices/${deviceId}/timeline/recent`, {
      params: {days, pageSize: 50}
    });
  }

  /** 按时间游标读取更早的历史事件，由“查看更多”按钮触发。 */
  getHistoryTimeline(deviceId: string, beforeTs: string): Observable<LogTimelinePage> {
    return this.http.get<LogTimelinePage>(`/api/device-log/devices/${deviceId}/timeline/history`, {
      params: {pageSize: 50, beforeTs}
    });
  }
}
