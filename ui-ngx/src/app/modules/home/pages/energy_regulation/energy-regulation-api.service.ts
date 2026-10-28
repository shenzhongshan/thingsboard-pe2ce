/** 文件职责：能源监管页面设备上下文与用电统计接口访问。 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AlarmInfo, AlarmQueryV2} from '@shared/models/alarm.models';
import {PageData} from '@shared/models/page/page-data';

/** 能源监管设备上下文只保留本模块当前需要的设备字段。 */
export interface EnergyRegulationDeviceContext {
  deviceId: string;
  deviceName: string;
  deviceProfileName?: string;
  productCategory?: string;
  productName?: string;
  active?: boolean;
  latitude?: number | null;
  longitude?: number | null;
}

/** 公司筛选使用的资产树根节点。 */
export interface EnergyRegulationRootAsset {
  assetId: string;
  assetName: string;
}

/** 能源监管接口返回的真实资产树节点，子节点遵循平台 Contains 层级。 */
export interface EnergyRegulationAssetTreeNode {
  id: string;
  name: string;
  entityType: 'ASSET' | 'DEVICE';
  children: EnergyRegulationAssetTreeNode[];
}

/** 当前用户可见资产和设备关系树的响应结构。 */
export interface EnergyRegulationAssetTree {
  roots: EnergyRegulationAssetTreeNode[];
}

/** 日、周或月的真实用电汇总；缺少有效表底边界时数值为空。 */
export interface EnergyRegulationPeriodConsumption {
  valueKwh: number | null;
  previousValueKwh: number | null;
  changePercent: number | null;
  includedMeterCount: number;
  previousIncludedMeterCount: number;
}

/** 按时间桶返回用电趋势，null 表示该桶缺少完整读数。 */
export interface EnergyRegulationConsumptionSeries {
  labels: string[];
  values: Array<number | null>;
}

/** 能源监管累计电能接口的完整响应结构。 */
export interface EnergyRegulationEnergyConsumption {
  meterCount: number;
  readFailureCount: number;
  invalidCounterCount: number;
  totalKwh: number | null;
  energyConsumptionByPeriod: Record<'day' | 'week' | 'month', EnergyRegulationPeriodConsumption>;
  energySeries: Record<'day' | 'week' | 'month', EnergyRegulationConsumptionSeries>;
}

@Injectable({providedIn: 'root'})
export class EnergyRegulationApiService {
  constructor(private readonly http: HttpClient) {}

  /** 获取当前用户可见资产树的根资产，作为公司筛选选项。 */
  getRootAssets(): Observable<EnergyRegulationRootAsset[]> {
    return this.http.get<EnergyRegulationRootAsset[]>('/api/energy-regulation/root-assets');
  }

  /** 读取当前用户有权限查看的能源监管资产层级，不按设备分类裁剪资产树。 */
  getAssetTree(): Observable<EnergyRegulationAssetTree> {
    return this.http.get<EnergyRegulationAssetTree>('/api/energy-regulation/asset-tree');
  }

  /** 获取 productCategory=energy_regulation 的当前用户可见设备。 */
  getDeviceContexts(): Observable<EnergyRegulationDeviceContext[]> {
    return this.http.get<EnergyRegulationDeviceContext[]>('/api/energy-regulation/device-contexts');
  }

  /** 读取能源监管设备的标准累计电能统计和日、周、月趋势。 */
  getEnergyConsumption(): Observable<EnergyRegulationEnergyConsumption> {
    return this.http.get<EnergyRegulationEnergyConsumption>('/api/energy-regulation/energy-consumption');
  }

  /**
   * 查询能源监管模块告警；筛选条件沿用 v2 参数格式，productCategory 固定为 energy_regulation。
   * 后端会把请求范围限制到当前用户有读取权限的能源监管设备。
   */
  getAlarms(query: AlarmQueryV2): Observable<PageData<AlarmInfo>> {
    return this.http.get<PageData<AlarmInfo>>(
      `/api/energy-regulation/alarms${query.toQuery()}&productCategory=energy_regulation`
    );
  }
}
