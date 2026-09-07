import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DeviceInfo } from '@shared/models/device.models';
import { PageData } from '@shared/models/page/page-data';
import { SmartLightningMonitoringTreeResponseNode } from './smart-lightning-monitoring.data';

export interface SmartLightningMonitoringTreeResponse {
  roots: SmartLightningMonitoringTreeResponseNode[];
}

/** 全局顶部栏使用的设备状态汇总，不限定产品分类。 */
export interface SmartLightningDeviceStatusSummary {
  normal: number;
  abnormal: number;
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
}
