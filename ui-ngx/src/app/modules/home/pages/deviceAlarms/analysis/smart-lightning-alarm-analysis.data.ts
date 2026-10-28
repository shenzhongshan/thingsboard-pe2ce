/** 文件职责：告警分析模块的 smart-lightning-alarm-analysis.data，定义数据模型、接口结果映射与纯函数。 */
/** 告警分析页的统计周期：日 / 月 / 年。 */
export type AlarmAnalysisPeriod = 'day' | 'month' | 'year';

/** 顶部周期统计卡的数据结构：标签文案与接口统计值。 */
export interface AlarmAnalysisPeriodCard {
  /** 周期键，与后端返回字段对应。 */
  key: AlarmAnalysisPeriod;
  /** 卡片角标文案（日/月/年）。 */
  label: string;
  /** 当前周期数值的标题（如“今日告警数”）。 */
  currentLabel: string;
  /** 对比周期数值的标题（如“昨日告警数”）。 */
  previousLabel: string;
  /** 环比/同比标题（如“日环比”）。 */
  changeLabel: string;
  /** 当前周期告警次数；接口加载前使用 0 占位。 */
  total: number;
  /** 对比周期告警次数。 */
  previous: number;
  /** 变化百分比的展示文本（带正负号）。 */
  change: string;
  /** 卡片配色主题。 */
  tone: 'blue' | 'orange' | 'teal';
}

/** 设备直属父资产告警排名行。 */
export interface AlarmAnalysisSiteRow {
  /** 资产名称。 */
  name: string;
  /** 从根资产到设备直属父资产的完整路径。 */
  path: string;
  /** 周期内告警数量。 */
  count: number;
  /** 已处理率百分比。 */
  handledRate: number;
}

/** 后端设备直属父资产告警统计的最小结构，避免数据转换器依赖 HTTP 模型。 */
export interface AlarmAnalysisLeafAssetCount {
  assetName: string;
  /** 后端返回的资产绝对路径；旧接口未返回时回退为资产名称。 */
  assetPath?: string;
  alarmCount: number;
  handledRate: number;
}

/** 后端按告警类型返回的周期统计项。 */
export interface AlarmAnalysisTypeTotal {
  type: string;
  count: number;
}

/** 后端按 ThingsBoard 严重程度返回的周期统计项。 */
export interface AlarmAnalysisSeverityTotal {
  severity: string;
  count: number;
}

/** 严重程度分布行，补充中文名称、占比和配色。 */
export interface AlarmAnalysisSeverityRow {
  code: string;
  name: string;
  count: number;
  percentage: number;
  tone: 'critical' | 'major' | 'minor' | 'warning' | 'indeterminate';
}

/** 告警确认/清除状态排行行。 */
export interface AlarmAnalysisStatusRow {
  code: string;
  name: string;
  count: number;
  percentage: number;
  tone: 'active' | 'cleared';
}

/** 条形图输入：名称 + 数量。 */
export interface AlarmAnalysisBarInput {
  name: string;
  count: number;
  /** 图表悬浮提示使用的完整路径，类型排行没有时回退为名称。 */
  path?: string;
}

/** 条形图渲染数据：在输入基础上补充排名与占比。 */
export interface AlarmAnalysisBar extends AlarmAnalysisBarInput {
  /** 排名，从 1 开始。 */
  rank: number;
  /** 相对最大值的百分比宽度。 */
  percentage: number;
}

/** 三张周期统计卡的元数据；数值由告警统计接口填充，初始不再使用演示数值。 */
export const ALERT_ANALYSIS_PERIODS: readonly AlarmAnalysisPeriodCard[] = [
  {key: 'day', label: '日', currentLabel: '今日告警数', previousLabel: '昨日告警数', changeLabel: '日环比', total: 0, previous: 0, change: '--', tone: 'blue'},
  {key: 'month', label: '月', currentLabel: '本月告警数', previousLabel: '上月告警数', changeLabel: '月环比', total: 0, previous: 0, change: '--', tone: 'orange'},
  {key: 'year', label: '年', currentLabel: '今年告警数', previousLabel: '去年告警数', changeLabel: '年同比', total: 0, previous: 0, change: '--', tone: 'teal'}
];

/** 把后端设备直属父资产统计映射为站点排名行，并按处理率降序排列。 */
export function mapAlarmAnalysisSiteRows(items: readonly AlarmAnalysisLeafAssetCount[]): AlarmAnalysisSiteRow[] {
  return [...items]
    .map((item) => ({
      name: item.assetName || '未命名设备父资产',
      path: item.assetPath || item.assetName || '未命名设备父资产',
      count: Number.isFinite(item.alarmCount) ? item.alarmCount : 0,
      handledRate: Number.isFinite(item.handledRate) ? item.handledRate : 0
    }))
    .sort((left, right) => right.handledRate - left.handledRate
      || right.count - left.count
      || left.name.localeCompare(right.name, 'zh-CN'));
}
/** 把后端告警类型统计转换为排行榜输入；数量排序交给统一条形图构建函数。 */
/** 取周期对应的中文标签，未匹配时回退为“日”。 */
export function getAlarmAnalysisPeriodLabel(period: AlarmAnalysisPeriod): string {
  const item = ALERT_ANALYSIS_PERIODS.find((candidate) => candidate.key === period);
  return item?.label || '日';
}
export function mapAlarmAnalysisTypeRows(items: readonly AlarmAnalysisTypeTotal[]): AlarmAnalysisBarInput[] {
  return items.map((item) => ({
    name: item.type?.trim() || '未分类',
    count: Number.isFinite(item.count) ? item.count : 0
  }));
}

/** 将 ThingsBoard 严重程度代码转换为页面展示名称，并按数量降序排列。 */
export function mapAlarmAnalysisSeverityRows(items: readonly AlarmAnalysisSeverityTotal[]): AlarmAnalysisSeverityRow[] {
  const labels: Record<string, {name: string; tone: AlarmAnalysisSeverityRow['tone']}> = {
    CRITICAL: {name: '严重', tone: 'critical'},
    MAJOR: {name: '重要', tone: 'major'},
    MINOR: {name: '次要', tone: 'minor'},
    WARNING: {name: '警告', tone: 'warning'},
    INDETERMINATE: {name: '不确定', tone: 'indeterminate'}
  };
  const rows: AlarmAnalysisSeverityRow[] = [];
  for (const item of items) {
    const code = item.severity?.trim().toUpperCase() || 'INDETERMINATE';
    const display = labels[code] || {name: '不确定', tone: 'indeterminate' as const};
    rows.push({code, name: display.name, count: Number.isFinite(item.count) ? item.count : 0, percentage: 0, tone: display.tone});
  }
  rows.sort((left, right) => right.count - left.count || left.name.localeCompare(right.name, 'zh-CN'));
  const max = Math.max(1, rows[0]?.count || 0);
  for (const row of rows) {
    row.percentage = Math.round(row.count / max * 100);
  }
  return rows;
}

/** 将后端四种告警状态代码映射为中文名称，并按数量降序排列。 */
export function mapAlarmAnalysisStatusRows(items: readonly {status: string; count: number}[]): AlarmAnalysisStatusRow[] {
  const labels: Record<string, {name: string; tone: AlarmAnalysisStatusRow['tone']}> = {
    ACTIVE_UNACK: {name: '未确认未清除', tone: 'active'},
    ACTIVE_ACK: {name: '已确认未清除', tone: 'active'},
    CLEARED_UNACK: {name: '未确认已清除', tone: 'cleared'},
    CLEARED_ACK: {name: '已确认已清除', tone: 'cleared'}
  };
  const rows: AlarmAnalysisStatusRow[] = [];
  for (const item of items) {
    const code = item.status?.trim().toUpperCase() || 'ACTIVE_UNACK';
    const display = labels[code] || {name: '其他状态', tone: 'cleared' as const};
    rows.push({
      code,
      name: display.name,
      count: Number.isFinite(item.count) ? item.count : 0,
      percentage: 0,
      tone: display.tone
    });
  }
  rows.sort((left, right) => right.count - left.count || left.name.localeCompare(right.name, 'zh-CN'));
  const max = Math.max(1, rows[0]?.count || 0);
  for (const row of rows) {
    row.percentage = Math.round(row.count / max * 100);
  }
  return rows;
}

/** 计算告警设备覆盖率，设备总数为 0 时返回 0，避免出现 NaN。 */
export function calculateAlarmCoveragePercent(alarmedDevices: number, totalDevices: number): number {
  if (totalDevices <= 0 || alarmedDevices <= 0) {
    return 0;
  }
  return Math.round(Math.min(alarmedDevices, totalDevices) / totalDevices * 10000) / 100;
}

/**
 * 把排名输入转换为条形图渲染数据。
 *
 * <p>数量降序、同数量按名称排序；计算每项相对最大值的百分比，便于渲染条宽。</p>
 */
export function buildAlarmAnalysisBars(items: readonly AlarmAnalysisBarInput[]): AlarmAnalysisBar[] {
  const sorted = [...items].sort((left, right) => right.count - left.count || left.name.localeCompare(right.name, 'zh-CN'));
  const max = Math.max(1, sorted[0]?.count || 0);
  return sorted.map((item, index) => ({
    ...item,
    rank: index + 1,
    percentage: Math.round(item.count / max * 100)
  }));
}

/** 按排名返回色调：第 1 名红色突出，第 2/3 名橙色警示，其余中性。 */
export function getAlarmAnalysisRankTone(rank: number): 'critical' | 'warning' | 'neutral' {
  if (rank === 1) {
    return 'critical';
  }
  if (rank === 2 || rank === 3) {
    return 'warning';
  }
  return 'neutral';
}
