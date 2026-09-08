import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DeviceInfo } from '@shared/models/device.models';
import { PageData } from '@shared/models/page/page-data';
import { SmartLightningMonitoringTreeResponseNode } from './smart-lightning-monitoring.data';
import { SmartLightningPeriod } from '../dashboard/smart-lightning-dashboard.data';

export interface SmartLightningMonitoringTreeResponse {
  roots: SmartLightningMonitoringTreeResponseNode[];
}

/** 后端批量返回的设备基础信息与服务器属性。 */
export interface SmartLightningDeviceContext {
  deviceId: string;
  deviceName: string;
  label?: string;
  deviceProfileName?: string;
  productCategory?: string;
  productName?: string;
  functionCategory?: string;
  deviceProtocol?: string;
  active?: boolean;
  lastActivityTime?: number | null;
  gatewayIdentifier?: string;
  meterAddress?: string;
  circuitName?: string;
  timeSetting?: string;
}

/** 全局顶部栏使用的设备状态汇总，不限定产品分类。 */
export interface SmartLightningDeviceStatusSummary {
  normal: number;
  abnormal: number;
}

/** 地网监测模块的当前快照；阻值没有有效遥测时为 null。 */
export interface SmartLightningGroundMonitoringSummary {
  total: number;
  online: number;
  offline: number;
  alarmCount: number;
  maxResistance: number | null;
  minResistance: number | null;
}

export interface SmartLightningAlarmTypeTotal {
  type: string;
  count: number;
}

export interface SmartLightningAlarmSummary {
  activeCount: number;
  typeTotals: SmartLightningAlarmTypeTotal[];
}

/**
 * 数据监测页的聚合接口。后端已按当前登录用户过滤权限并拼接完整层级，
 * 因此组件不再逐个资产请求通用关系接口。
 */
@Injectable({providedIn: 'root'})
export class SmartLightningMonitoringApiService {

  constructor(private readonly http: HttpClient) {}

  getTree(): Observable<SmartLightningMonitoringTreeResponse> {
    return this.http.get<SmartLightningMonitoringTreeResponse>('/api/smart-lightning/monitoring/tree');
  }

  /**
   * 查询当前用户全部可见设备的状态汇总。
   * active 的解释由后端统一读取 SERVER_SCOPE 属性，避免误用 DeviceInfo.active。
   */
  getDeviceStatusSummary(): Observable<SmartLightningDeviceStatusSummary> {
    return this.http.get<SmartLightningDeviceStatusSummary>('/api/smart-lightning/monitoring/device-status-summary');
  }

  /**
   * 地网模块只发起一次聚合请求。period 仅筛选按触发时间统计的告警数，
   * 设备状态和阻值范围始终是最新快照。
   */
  getGroundMonitoringSummary(period: SmartLightningPeriod): Observable<SmartLightningGroundMonitoringSummary> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SmartLightningGroundMonitoringSummary>(
      '/api/smart-lightning/monitoring/ground-summary', {params}
    );
  }

  /** 查询当前用户可见防雷设备的未清除告警总数及按类型汇总。 */
  getLightningAlarmSummary(): Observable<SmartLightningAlarmSummary> {
    return this.http.get<SmartLightningAlarmSummary>('/api/smart-lightning/monitoring/alarm-summary');
  }

  /**
   * 查询当前登录用户可见且 productCategory=lightning 的设备。
   * 过滤由后端完成，前端只负责传递分页和可选的名称搜索条件。
   */
  getLightningDevices(pageSize: number, page: number, textSearch?: string): Observable<PageData<DeviceInfo>> {
    let params = new HttpParams()
      .set('pageSize', String(pageSize))
      .set('page', String(page));
    if (textSearch?.trim()) {
      params = params.set('textSearch', textSearch.trim());
    }
    return this.http.get<PageData<DeviceInfo>>('/api/smart-lightning/monitoring/devices', {params});
  }

  /** 一次返回设备上下文，替代前端按设备逐个读取服务器属性。 */
  getLightningDeviceContexts(pageSize: number, page: number, textSearch?: string): Observable<PageData<SmartLightningDeviceContext>> {
    let params = new HttpParams()
      .set('pageSize', String(pageSize))
      .set('page', String(page));
    if (textSearch?.trim()) {
      params = params.set('textSearch', textSearch.trim());
    }
    return this.http.get<PageData<SmartLightningDeviceContext>>(
      '/api/smart-lightning/monitoring/device-contexts', {params});
  }
}
