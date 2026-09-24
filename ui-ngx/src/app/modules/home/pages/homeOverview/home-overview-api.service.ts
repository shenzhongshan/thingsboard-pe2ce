/**
 * 文件职责：首页设备上下文接口访问。
 * 分类字段由后端根据设备 SERVER_SCOPE 属性返回，页面不再自行读取并推断产品大类。
 */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HomeOverviewDeviceContext {
  deviceId: string;
  deviceName: string;
  deviceProfileName?: string;
  productCategory?: string;
  productName?: string;
  functionCategory?: string;
  active?: boolean;
  latitude?: number | null;
  longitude?: number | null;
}

@Injectable({providedIn: 'root'})
export class HomeOverviewApiService {
  constructor(private readonly http: HttpClient) {}

  /** 查询当前用户可见设备；权限和分类均由后端统一处理。 */
  getVisibleDeviceContexts(): Observable<HomeOverviewDeviceContext[]> {
    return this.http.get<HomeOverviewDeviceContext[]>('/api/home-overview/device-contexts');
  }
}
