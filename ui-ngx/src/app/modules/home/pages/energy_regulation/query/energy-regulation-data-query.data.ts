/** 能源查询纯数据适配：以模块真实上下文为输入，负责状态、资产路径、筛选与CSV映射。 */
import type {EnergyRegulationDeviceContext} from '../energy-regulation-api.service';
import type {EnergyMonitoringAssetTree} from '../monitoring/energy-regulation-monitoring.data';

/** 与防雷、电气安全一致：未清除告警优先，其次离线，最后正常；不把在线与告警混为同一概念。 */
export type EnergyQueryStatus = '报警' | '离线' | '正常';
export interface EnergyQueryDevice {
  id: string; name: string; serialNo: string; deviceType: string; protocol: string;
  status: EnergyQueryStatus; productName: string; functionCategory: string;
  assetId: string; assetPath: string; lastActivityTime: number | null; updatedAt: string;
  /** 当前设备最高级别未清除告警的中文等级，没有告警时为空。 */
  alarmSeverity?: string;
}
/** 查询行只承载表格所需字段，不直接依赖其他业务模块DTO。 */
export interface EnergyQueryDeviceRow {
  id: string; deviceName: string; serialNo: string; deviceType: string; protocol: string;
  status: EnergyQueryStatus; productName: string; assetPath: string; alarmSeverity?: string;
}
export interface EnergyQueryFilters {
  status: '全部' | EnergyQueryStatus;
  productName: string; functionCategory: string; keyword: string; assetIds: ReadonlySet<string>;
}

/** 协议显示与防雷查询保持一致，默认协议使用中文说明，其他协议保留实际名称。 */
export function formatEnergyQueryProtocol(value: unknown): string {
  const protocol = typeof value === 'string' ? value.trim() : '';
  return protocol === 'DEFAULT' ? '默认（支持基本 MQTT、HTTP 和 CoAP 传输）' : protocol || '--';
}

/** 一次建立父节点索引，避免逐设备重新扫描整棵树。 */
export function buildEnergyQueryDevices(tree: EnergyMonitoringAssetTree, contexts: readonly EnergyRegulationDeviceContext[] = [],
  latest: (id: string) => ReadonlyMap<string, {ts: number; value: string}> = () => new Map(),
  alarmSeverity: ReadonlyMap<string, string> = new Map()): EnergyQueryDevice[] {
  const parents = new Map<string, string>();
  tree.nodes.forEach((node) => node.childIds.forEach((id) => parents.set(id, node.id)));
  return contexts.map((context): EnergyQueryDevice => {
    const values = latest(context.deviceId);
    const active = values.get('active')?.value;
    const online = active === undefined ? context.active === true : ['true', '1'].includes(String(active).toLowerCase());
    const lastActivityTime = Math.max(context.lastActivityTime || 0, ...Array.from(values.values(), (sample) => sample.ts)) || null;
    const names: string[] = [];
    const visited = new Set<string>();
    let id: string | undefined = context.deviceId;
    while (id && !visited.has(id)) {
      visited.add(id);
      const node = tree.nodes.get(id);
      if (node) { names.unshift(node.name); }
      id = parents.get(id);
    }
    return {id: context.deviceId, name: context.deviceName, serialNo: context.deviceName,
      deviceType: context.deviceProfileName || '__', protocol: formatEnergyQueryProtocol(context.deviceProtocol),
      status: alarmSeverity.has(context.deviceId) ? '报警' : online ? '正常' : '离线',
      productName: context.productName || '__',
      // 功能分类仍取服务器属性，只把已确认的能源分类编码翻译为中文显示。
      functionCategory: context.functionCategory === 'energy_meter' ? '电能计量' : context.functionCategory || '__',
      assetId: context.deviceId, assetPath: names.join('/') || '__', lastActivityTime,
      updatedAt: lastActivityTime ? new Date(lastActivityTime).toLocaleString() : '__', alarmSeverity: alarmSeverity.get(context.deviceId)};
  });
}

/** 设备与行使用真实设备ID一一对应，路径用于悬浮定位。 */
export function buildEnergyQueryDeviceRows(devices: readonly EnergyQueryDevice[]): EnergyQueryDeviceRow[] {
  return devices.map((device) => ({id: device.id, deviceName: device.name, serialNo: device.serialNo,
    deviceType: device.deviceType, protocol: device.protocol, status: device.status,
    productName: device.productName, assetPath: device.assetPath, alarmSeverity: device.alarmSeverity}));
}

/** 筛选即时生效，必须先命中资产范围，关键词支持名称、编号、型号与产品名称。 */
export function filterEnergyQueryDevices(devices: readonly EnergyQueryDevice[], filters: EnergyQueryFilters): EnergyQueryDevice[] {
  const keyword = filters.keyword.trim().toLocaleLowerCase();
  return devices.filter((device) => filters.assetIds.has(device.assetId)
    && (filters.status === '全部' || device.status === filters.status)
    && (!filters.productName || device.productName === filters.productName)
    && (!filters.functionCategory || device.functionCategory === filters.functionCategory)
    && (!keyword || [device.name, device.serialNo, device.deviceType, device.productName]
      .some((value) => value.toLocaleLowerCase().includes(keyword))));
}

/** CSV字段完整转义，Excel通过BOM识别中文，不修改原始数值字符串。 */
export function buildEnergyQueryCsv(lines: string[][]): Blob {
  const escape = (value: string): string => `"${value.replace(/"/g, '""')}"`;
  return new Blob([`\uFEFF${lines.map((line) => line.map(escape).join(',')).join('\r\n')}`], {type: 'text/csv;charset=utf-8'});
}

/** 与表格相同设备范围导出，并附上设备所属资产路径。 */
export function buildEnergyQueryDeviceCsv(devices: readonly EnergyQueryDevice[]): string[][] {
  return [['序号', '设备名称', '设备类型', '设备协议', '状态', '产品名称', '所属资产', '最近上报时间'],
    ...devices.map((device, index) => [String(index + 1), device.name, device.deviceType,
      device.protocol, device.status, device.productName, device.assetPath, device.updatedAt])];
}
