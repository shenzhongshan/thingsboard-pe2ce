/** 文件职责：集中访问电气安全模块自己的设备上下文和资产树接口。 */
import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {EMPTY, Observable} from 'rxjs';
import {expand, reduce} from 'rxjs/operators';
import {EntityType} from '@shared/models/entity-type.models';
import type {AlarmInfo} from '@shared/models/alarm.models';
import type {PageData} from '@shared/models/page/page-data';

/** 汇总当前活动告警名称时每页读取的数量；有后续页时仍会继续查询。 */
const ELECTRICAL_SAFETY_ALARM_SUMMARY_PAGE_SIZE = 100;

/** 电气安全设备基础资料；不包含尚未接入的告警统计和测点值。 */
export interface ElectricalSafetyDeviceContext {
  deviceId: string;
  deviceName: string;
  deviceProfileName?: string | null;
  productCategory: string;
  productName?: string | null;
  functionCategory?: string | null;
  deviceProtocol?: string | null;
  active: boolean;
  lastActivityTime?: number | null;
  latitude?: number | null;
  longitude?: number | null;
}

/** 电气安全告警统计中的单个规范功能分类及周期事件次数。 */
export interface ElectricalSafetyFunctionAlarmCount {
  functionCategory: string;
  alarmCount: number;
  /** 所选周期内尚未清除的告警事件数。 */
  pendingCount: number;
  /** 尚未清除且没有受理人的告警数，是待处理数的子集。 */
  unassignedCount: number;
  /** 所选周期内已清除的告警事件数。 */
  processedCount: number;
}

/** 电气安全看板的告警统计响应；ID 范围已经由后端按当前用户权限过滤。 */
export interface ElectricalSafetyDashboardAlarmStatistics {
  activeAlarmDeviceIds: string[];
  functionCategoryCounts: ElectricalSafetyFunctionAlarmCount[];
}

/** 后端按权限与 electrical_safety 分类裁剪后的资产树节点。 */
export interface ElectricalSafetyAssetTreeNode {
  id: string;
  name: string;
  entityType: EntityType.ASSET | EntityType.DEVICE;
  children: ElectricalSafetyAssetTreeNode[];
}

/** 电气安全资产树接口响应。 */
export interface ElectricalSafetyAssetTree {
  roots: ElectricalSafetyAssetTreeNode[];
}

/**
 * 电气安全前端 API 客户端。
 * 所有请求走电气安全自己的路由，避免依赖首页、防雷或能源监管接口契约。
 */
@Injectable({providedIn: 'root'})
export class ElectricalSafetyApiService {
  constructor(private readonly http: HttpClient) {}

  /** 获取当前用户可见的 electrical_safety 设备基础信息。 */
  getDeviceContexts(): Observable<ElectricalSafetyDeviceContext[]> {
    return this.http.get<ElectricalSafetyDeviceContext[]>('/api/electrical-safety/device-contexts');
  }

  /** 获取仅包含电气安全设备分支的真实资产树。 */
  getAssetTree(): Observable<ElectricalSafetyAssetTree> {
    return this.http.get<ElectricalSafetyAssetTree>('/api/electrical-safety/asset-tree');
  }

  /**
   * 获取所选自然周期内的告警次数和仍有未清除告警的设备 ID。
   * 后端会再次验证 productCategory 并限制在当前用户可见设备范围。
   */
  getDashboardAlarmStatistics(
    productCategory: string,
    period: 'day' | 'week' | 'month'
  ): Observable<ElectricalSafetyDashboardAlarmStatistics> {
    const params = new HttpParams()
      .set('productCategory', productCategory)
      .set('period', period);
    return this.http.get<ElectricalSafetyDashboardAlarmStatistics>(
      '/api/electrical-safety/dashboard-alarm-statistics', {params});
  }

  /**
   * 获取电气安全模块最近告警，分页与排序遵循平台 v2 告警查询参数。
   * 该模块接口只返回当前用户可见 electrical_safety 设备的告警。
   */
  getAlarms(
    productCategory: string,
    pageSize: number,
    page: number
  ): Observable<PageData<AlarmInfo>> {
    const params = new HttpParams()
      .set('productCategory', productCategory)
      .set('pageSize', String(pageSize))
      .set('page', String(page))
      .set('statusList', 'ACTIVE')
      .set('sortProperty', 'startTs')
      .set('sortOrder', 'DESC');
    return this.http.get<PageData<AlarmInfo>>('/api/electrical-safety/alarms', {params});
  }

  /**
   * 通过现有电气安全告警查询接口顺序读取全部活动告警，供看板按真实 alarm.type 聚合。
   * 按 hasNext 逐页读取，避免只统计第一页；不新增后端路由，也不推断告警分类。
   */
  getAllActiveAlarms(productCategory: string): Observable<AlarmInfo[]> {
    const pageSize = ELECTRICAL_SAFETY_ALARM_SUMMARY_PAGE_SIZE;
    return this.getAlarms(productCategory, pageSize, 0).pipe(
      expand((page, pageIndex) => page.hasNext
        ? this.getAlarms(productCategory, pageSize, pageIndex + 1)
        : EMPTY, 1),
      reduce((alarms, page) => {
        for (const alarm of page.data || []) {
          alarms.push(alarm);
        }
        return alarms;
      }, [] as AlarmInfo[])
    );
  }
}
