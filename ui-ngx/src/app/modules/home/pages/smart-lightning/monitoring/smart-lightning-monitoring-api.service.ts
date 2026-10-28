/** 文件职责：数据监测模块的 smart-lightning-monitoring-api.service，封装后端 HTTP 接口与请求参数。 */
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AlarmInfo, AlarmQueryV2 } from '@shared/models/alarm.models';
import { DeviceInfo } from '@shared/models/device.models';
import { PageData } from '@shared/models/page/page-data';
import { SmartLightningMonitoringTreeResponseNode } from './smart-lightning-monitoring.data';
import { SmartLightningPeriod } from '../dashboard/smart-lightning-dashboard.data';

/** 资产树接口响应结构。 */
export interface SmartLightningMonitoringTreeResponse {
  /** 根节点列表（资产），设备只作为叶子返回。 */
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
  /** 地图纬度，来自设备 SERVER_SCOPE 的 latitude 服务器属性。 */
  latitude?: number | null;
  /** 地图经度，来自设备 SERVER_SCOPE 的 longitude 服务器属性。 */
  longitude?: number | null;
}

/** 全局顶部栏使用的设备状态汇总，不限定产品分类。 */
export interface SmartLightningDeviceStatusSummary {
  normal: number;
  abnormal: number;
}

/** 数据监测页所选资产范围内的整体实时状态。 */
export interface SmartLightningRealtimeOverall {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  alarmDeviceCount: number;
  status: string;
}

/** SPD 设备最新状态参数与关键漏流指标。 */
export interface SmartLightningRealtimeSpd {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  alarmDeviceCount: number;
  breakerStatusNormalCount: number;
  breakerStatusDeviceCount: number;
  groundStatusNormalCount: number;
  groundStatusDeviceCount: number;
  temperatureNormalCount: number;
  temperatureDeviceCount: number;
  lifeNormalCount: number;
  lifeDeviceCount: number;
  maximumLeakage: number | null;
  maximumLeakageDeviceName?: string | null;
  maximumLeakageParameter?: string | null;
  maximumLeakageTime?: number | null;
  status: string;
}

/** 接地电阻设备最新阻值范围。 */
export interface SmartLightningRealtimeGround {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  alarmDeviceCount: number;
  reportingDeviceCount: number;
  maximumResistance: number | null;
  maximumResistanceDeviceName?: string | null;
  maximumResistanceTime?: number | null;
  minimumResistance: number | null;
  minimumResistanceDeviceName?: string | null;
  minimumResistanceTime?: number | null;
  status: string;
}

/** 雷电流设备最新累计次数与最近峰值。 */
export interface SmartLightningRealtimeCurrent {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  alarmDeviceCount: number;
  totalSurgeCount: number;
  latestPeakCurrent: number | null;
  latestPeakDeviceName?: string | null;
  latestPeakPolarity?: string | null;
  latestPeakTime?: number | null;
  peakCurrentUnit?: string | null;
  status: string;
}

/** 故障电弧设备最新状态与累计次数。 */
export interface SmartLightningRealtimeArcFault {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  alarmDeviceCount: number;
  normalStatusCount: number;
  statusDeviceCount: number;
  totalArcFaultCount: number;
  latestReportTime?: number | null;
  status: string;
}

/** 数据监测页实时汇总响应；arcFault 字段保留兼容旧接口，前端卡片已迁至电气安全模块。 */
export interface SmartLightningRealtimeAnalysis {
  overall: SmartLightningRealtimeOverall;
  spd: SmartLightningRealtimeSpd;
  ground: SmartLightningRealtimeGround;
  lightningCurrent: SmartLightningRealtimeCurrent;
  arcFault: SmartLightningRealtimeArcFault;
}

/** 地网监测模块的当前快照及所选周期内的阻值范围。 */
export interface SmartLightningGroundMonitoringSummary {
  total: number;
  online: number;
  offline: number;
  alarmCount: number;
  /** 所选周期内按告警类型统计的数量，接口已按数量降序返回。 */
  alarmTypeTotals: SmartLightningAlarmTypeTotal[];
  alarmRate: number;
  maxResistance: number | null;
  minResistance: number | null;
  /** 阻值最值及其设备、参数和时间来源。 */
  resistanceExtrema: SmartLightningGroundResistanceExtrema;
}

/** 地网阻值最值及其来源信息。 */
export interface SmartLightningGroundResistanceExtrema {
  maximum: number | null;
  maximumDeviceName?: string | null;
  maximumParameter?: string | null;
  maximumTime?: number | null;
  minimum: number | null;
  minimumDeviceName?: string | null;
  minimumParameter?: string | null;
  minimumTime?: number | null;
}

/** SPD 周期最值及其来源信息；无有效遥测时数值和来源字段为空。 */
export interface SmartLightningSpdMetricExtrema {
  maximum: number | null;
  maximumDeviceName?: string | null;
  maximumParameter?: string | null;
  maximumTime?: number | null;
  minimum: number | null;
  minimumDeviceName?: string | null;
  minimumParameter?: string | null;
  minimumTime?: number | null;
}

/** SPD 监测模块的聚合快照；漏电流没有有效遥测时为 null。 */
export interface SmartLightningSpdMonitoringSummary {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  alarmCount: number;
  /** 所选周期内按告警类型统计的数量，接口已按数量降序返回。 */
  alarmTypeTotals: SmartLightningAlarmTypeTotal[];
  maxLeakage: number | null;
  maxLeakageDeviceName?: string | null;
  maxLeakagePhase?: string | null;
  /** 周期最大漏流对应的规范遥测键，例如 LeakCurrB。 */
  maxLeakageKey?: string | null;
  maxLeakageTime?: number | null;
  leakageReportingDeviceCount: number;
  /** 接地状态正常设备数，以及有 GndStatus 参数的设备总数。 */
  groundStatusNormalCount: number;
  groundStatusDeviceCount: number;
  /** 空开状态正常设备数，以及有 McbStatus 参数的设备总数。 */
  breakerStatusNormalCount: number;
  breakerStatusDeviceCount: number;
  /** 温度正常设备数，以及有任意 Temp 键的设备总数；同一设备任一温度异常即不计为正常。 */
  temperatureNormalCount: number;
  temperatureDeviceCount: number;
  /** SPD_RUL 不低于 20% 的设备数，以及有 SPD_RUL 参数的设备总数。 */
  degradationNormalCount: number;
  degradationDeviceCount: number;
  /** 所选周期内 StrikeCnt 的累计增量。 */
  /** 无基线且仅有首条累计值时为null，不将历史累计误算为本周期次数。 */
  lightningCount: number | null;
  /** 所选周期内温度的最大值和最小值。 */
  temperatureExtrema: SmartLightningSpdMetricExtrema;
  /** 所选周期内漏电流的最大值和最小值。 */
  leakageExtrema: SmartLightningSpdMetricExtrema;
  /** 所选周期内电压的最大值和最小值。 */
  voltageExtrema: SmartLightningSpdMetricExtrema;
}

/** 雷电流监测模块的聚合快照；峰值没有有效遥测时为 null。 */
export interface SmartLightningCurrentMonitoringSummary {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  alarmCount: number;
  /** 所选周期内按告警类型统计的数量，接口已按数量降序返回。 */
  alarmTypeTotals: SmartLightningAlarmTypeTotal[];
  /** 任一设备增量尚不可确定时为null。 */
  eventCount: number | null;
  maxPeakCurrent: number | null;
  /** 后端返回的雷击脉冲峰值电流单位；产品说明书规定为 kA。 */
  peakCurrentUnit?: string | null;
  maxPeakDeviceName?: string | null;
  maxPeakTime?: number | null;
  maxPeakPolarity?: string | null;
  minPeakCurrent: number | null;
  minPeakDeviceName?: string | null;
  minPeakTime?: number | null;
  minPeakPolarity?: string | null;
  reportingDeviceCount: number;
}

/** 按告警类型聚合的数量项。 */
export interface SmartLightningAlarmTypeTotal {
  type: string;
  count: number;
}

/** 告警分析页按 ThingsBoard 严重程度返回的统计项。 */
export interface SmartLightningAlarmSeverityTotal {
  severity: string;
  count: number;
}

/** 告警分析页当前周期的四种确认/清除状态统计。 */
export interface SmartLightningAlarmStatusTotal {
  status: string;
  count: number;
}

/** 告警设备覆盖率统计；设备数量均按设备 ID 去重。 */
export interface SmartLightningAlarmDeviceCoverage {
  totalDevices: number;
  alarmedDevices: number;
  activeAlarmDevices: number;
}

/** 告警分析页右侧状态分布与设备覆盖率聚合结果。 */
export interface SmartLightningAlarmAnalysisOverview {
  statusDistribution: SmartLightningAlarmStatusTotal[];
  deviceCoverage: SmartLightningAlarmDeviceCoverage;
}

/** 看板当前未清除告警统计。 */
export interface SmartLightningAlarmSummary {
  activeCount: number;
  typeTotals: SmartLightningAlarmTypeTotal[];
}

/** 单个统计周期的告警次数与环比。 */
export interface SmartLightningAlarmPeriodSummary {
  currentCount: number;
  previousCount: number;
  changePercent: number;
}

/** 告警趋势时间桶，label 为小时、日期或月份标签。 */
export interface SmartLightningAlarmTrendPoint {
  label: string;
  count: number;
}

/** 告警分析页重点设备：严重等级且仍未清除的设备。 */
export interface SmartLightningFocusAlarmDevice {
  deviceName: string;
  parentNodeName: string;
  alarmType: string;
  durationMs: number;
}

/** 告警分析的日/月/年周期统计。 */
export interface SmartLightningAlarmAnalysisSummary {
  day: SmartLightningAlarmPeriodSummary;
  month: SmartLightningAlarmPeriodSummary;
  year: SmartLightningAlarmPeriodSummary;
  dayTrend: SmartLightningAlarmTrendPoint[];
  monthTrend: SmartLightningAlarmTrendPoint[];
  yearTrend: SmartLightningAlarmTrendPoint[];
  focusDevices: SmartLightningFocusAlarmDevice[];
}

/** 设备直属父资产的告警数量与处理率统计。 */
export interface SmartLightningLeafAssetAlarmCount {
  assetId: string;
  assetName: string;
  alarmCount: number;
  handledCount: number;
  handledRate: number;
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
   * 查询当前资产及全部后代设备的最新分析数据。
   * 不传 assetId 时由后端统计当前用户全部可见防雷设备。
   */
  getRealtimeAnalysis(assetId?: string): Observable<SmartLightningRealtimeAnalysis> {
    let params = new HttpParams();
    if (assetId) {
      params = params.set('assetId', assetId);
    }
    return this.http.get<SmartLightningRealtimeAnalysis>(
      '/api/smart-lightning/monitoring/realtime-analysis', {params}
    );
  }

  /**
   * 查询当前用户可见防雷设备的状态汇总。
   * active 的解释由后端统一读取最新遥测，避免误用服务器属性或 DeviceInfo.active。
   */
  getDeviceStatusSummary(): Observable<SmartLightningDeviceStatusSummary> {
    return this.http.get<SmartLightningDeviceStatusSummary>('/api/smart-lightning/monitoring/device-status-summary');
  }

  /**
   * 地网模块只发起一次聚合请求。period 同时限定按触发时间统计的告警和阻值范围窗口，
   * 设备状态仍为当前快照。
   */
  getGroundMonitoringSummary(period: SmartLightningPeriod): Observable<SmartLightningGroundMonitoringSummary> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SmartLightningGroundMonitoringSummary>(
      '/api/smart-lightning/monitoring/ground-summary', {params}
    );
  }

  getSpdMonitoringSummary(period: SmartLightningPeriod): Observable<SmartLightningSpdMonitoringSummary> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SmartLightningSpdMonitoringSummary>(
      '/api/smart-lightning/monitoring/spd-summary', {params}
    );
  }

  getLightningCurrentMonitoringSummary(period: SmartLightningPeriod): Observable<SmartLightningCurrentMonitoringSummary> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SmartLightningCurrentMonitoringSummary>(
      '/api/smart-lightning/monitoring/lightning-current-summary', {params}
    );
  }

  /** 查询防雷设备未清除告警摘要；后端按 productCategory=lightning 统计。 */
  getLightningAlarmSummary(): Observable<SmartLightningAlarmSummary> {
    return this.http.get<SmartLightningAlarmSummary>('/api/smart-lightning/monitoring/alarm-summary');
  }

  /**
   * 查询防雷模块告警；筛选条件沿用 v2 参数格式，productCategory 固定为 lightning。
   * 后端会把请求范围限制到当前用户有读取权限的防雷设备。
   */
  getLightningAlarms(query: AlarmQueryV2): Observable<PageData<AlarmInfo>> {
    return this.http.get<PageData<AlarmInfo>>(
      `/api/smart-lightning/monitoring/alarms${query.toQuery()}&productCategory=lightning`
    );
  }

  /** 查询告警分析页日、月、年触发次数；后端按用户权限和防雷类别过滤。 */
  getLightningAlarmAnalysisSummary(): Observable<SmartLightningAlarmAnalysisSummary> {
    return this.http.get<SmartLightningAlarmAnalysisSummary>(
      '/api/smart-lightning/monitoring/alarm-analysis-summary'
    );
  }

  /** 查询每个末级设备父资产直接挂载设备在指定周期内的告警触发次数。 */
  getLeafAssetAlarmCounts(period: SmartLightningPeriod): Observable<SmartLightningLeafAssetAlarmCount[]> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SmartLightningLeafAssetAlarmCount[]>(
      '/api/smart-lightning/monitoring/alarm-analysis/leaf-assets', {params}
    );
  }

  /** 查询当前周期内防雷设备按告警类型聚合的记录数。 */
  getAlarmTypeRanking(period: SmartLightningPeriod): Observable<SmartLightningAlarmTypeTotal[]> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SmartLightningAlarmTypeTotal[]>(
      '/api/smart-lightning/monitoring/alarm-analysis/types', {params}
    );
  }

  /** 查询当前周期内防雷设备按告警严重程度聚合的记录数。 */
  getAlarmSeverityRanking(period: SmartLightningPeriod): Observable<SmartLightningAlarmSeverityTotal[]> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SmartLightningAlarmSeverityTotal[]>(
      '/api/smart-lightning/monitoring/alarm-analysis/severities', {params}
    );
  }

  /** 查询当前周期的告警状态分布和设备告警覆盖率。 */
  getAlarmAnalysisOverview(period: SmartLightningPeriod): Observable<SmartLightningAlarmAnalysisOverview> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SmartLightningAlarmAnalysisOverview>(
      '/api/smart-lightning/monitoring/alarm-analysis/overview', {params}
    );
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
