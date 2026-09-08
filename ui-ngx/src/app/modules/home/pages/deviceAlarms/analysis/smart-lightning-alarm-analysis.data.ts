export type AlarmAnalysisPeriod = 'day' | 'month' | 'year';

export interface AlarmAnalysisPeriodCard {
  key: AlarmAnalysisPeriod;
  label: string;
  currentLabel: string;
  previousLabel: string;
  changeLabel: string;
  total: number;
  previous: number;
  change: string;
  tone: 'blue' | 'orange' | 'teal';
}

export interface AlarmAnalysisSiteRow {
  name: string;
  count: number;
  handledRate: number;
}

export interface AlarmAnalysisBarInput {
  name: string;
  count: number;
}

export interface AlarmAnalysisBar extends AlarmAnalysisBarInput {
  rank: number;
  percentage: number;
}

export const ALERT_ANALYSIS_PERIODS: readonly AlarmAnalysisPeriodCard[] = [
  {key: 'day', label: '日', currentLabel: '今日告警数', previousLabel: '昨日告警数', changeLabel: '日环比', total: 18, previous: 14, change: '+28.57%', tone: 'blue'},
  {key: 'month', label: '月', currentLabel: '本月告警数', previousLabel: '上月告警数', changeLabel: '月环比', total: 86, previous: 73, change: '+17.81%', tone: 'orange'},
  {key: 'year', label: '年', currentLabel: '今年告警数', previousLabel: '去年告警数', changeLabel: '年同比', total: 426, previous: 391, change: '+8.95%', tone: 'teal'}
];

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

export const ALARM_ANALYSIS_TYPE_ROWS: readonly AlarmAnalysisBarInput[] = [
  {name: '现场报警', count: 23},
  {name: '通讯状态', count: 13},
  {name: '电流越限', count: 9},
  {name: '电压异常', count: 6},
  {name: '温度异常', count: 4}
];

export const ALARM_ANALYSIS_TREND = {
  labels: ['01', '02', '03', '04', '05', '06', '07'],
  values: [32, 24, 41, 27, 35, 18, 29]
};

export function getAlarmAnalysisPeriodLabel(period: AlarmAnalysisPeriod): string {
  const item = ALERT_ANALYSIS_PERIODS.find((candidate) => candidate.key === period);
  return item?.label || '日';
}

export function buildAlarmAnalysisBars(items: readonly AlarmAnalysisBarInput[]): AlarmAnalysisBar[] {
  const sorted = [...items].sort((left, right) => right.count - left.count || left.name.localeCompare(right.name, 'zh-CN'));
  const max = Math.max(1, sorted[0]?.count || 0);
  return sorted.map((item, index) => ({
    ...item,
    rank: index + 1,
    percentage: Math.round(item.count / max * 100)
  }));
}

export function getAlarmAnalysisRankTone(rank: number): 'critical' | 'warning' | 'neutral' {
  if (rank === 1) {
    return 'critical';
  }
  if (rank === 2 || rank === 3) {
    return 'warning';
  }
  return 'neutral';
}
