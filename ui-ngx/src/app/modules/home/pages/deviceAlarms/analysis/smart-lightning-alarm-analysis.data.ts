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

/** 末级资产告警排名行（演示数据）。 */
export interface AlarmAnalysisSiteRow {
  /** 资产名称。 */
  name: string;
  /** 周期内告警数量。 */
  count: number;
  /** 已处理率百分比。 */
  handledRate: number;
}

/** 条形图输入：名称 + 数量。 */
export interface AlarmAnalysisBarInput {
  name: string;
  count: number;
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

/** 末级资产告警排名演示数据（按告警数量降序）。 */
export const ALARM_ANALYSIS_SITE_ROWS: readonly AlarmAnalysisSiteRow[] = [
  {name: '研究院储能', count: 23, handledRate: 100},
  {name: '防雷测试项目-001', count: 16, handledRate: 67},
  {name: '研发楼配电房', count: 12, handledRate: 50},
  {name: '生产车间-东区', count: 8, handledRate: 38},
  {name: '门卫照明配电箱', count: 5, handledRate: 0},
  {name: '消防主电源', count: 3, handledRate: 0},
  {name: '研发楼配电房-二层', count: 2, handledRate: 0},
  {name: '实验楼总配电柜', count: 1, handledRate: 0}
];

/** 按告警类型聚合的排名演示数据。 */
export const ALARM_ANALYSIS_TYPE_ROWS: readonly AlarmAnalysisBarInput[] = [
  {name: '现场报警', count: 23},
  {name: '通讯状态', count: 13},
  {name: '电流越限', count: 9},
  {name: '电压异常', count: 6},
  {name: '温度异常', count: 4}
];

/** 近 7 日告警趋势演示数据。 */
export const ALARM_ANALYSIS_TREND = {
  labels: ['01', '02', '03', '04', '05', '06', '07'],
  values: [32, 24, 41, 27, 35, 18, 29]
};

/** 取周期对应的中文标签，未匹配时回退为“日”。 */
export function getAlarmAnalysisPeriodLabel(period: AlarmAnalysisPeriod): string {
  const item = ALERT_ANALYSIS_PERIODS.find((candidate) => candidate.key === period);
  return item?.label || '日';
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
