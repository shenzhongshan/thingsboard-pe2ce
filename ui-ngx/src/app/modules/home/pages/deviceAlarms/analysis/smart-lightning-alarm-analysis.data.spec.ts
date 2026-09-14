/** 文件职责：告警分析模块的 smart-lightning-alarm-analysis.data.spec，验证数据模型、映射与纯函数行为。 */
/** 告警分析纯函数单测：条形数据构建与排名色调（node assert 轻量运行）。 */
import {
  ALERT_ANALYSIS_PERIODS,
  buildAlarmAnalysisBars,
  getAlarmAnalysisPeriodLabel,
  getAlarmAnalysisRankTone,
  mapAlarmAnalysisSiteRows
} from './smart-lightning-alarm-analysis.data';

describe('smart lightning alarm analysis data', () => {
  it('keeps the three summary periods in day, month, year order', () => {
    expect(ALERT_ANALYSIS_PERIODS.map((period) => period.key)).toEqual(['day', 'month', 'year']);
    expect(getAlarmAnalysisPeriodLabel('month')).toBe('月');
    expect(ALERT_ANALYSIS_PERIODS.map((period) => period.currentLabel)).toEqual(['今日告警数', '本月告警数', '今年告警数']);
    expect(ALERT_ANALYSIS_PERIODS.map((period) => period.changeLabel)).toEqual(['日环比', '月环比', '年同比']);
  });

  it('builds ranking bars in descending order and assigns a stable rank tone', () => {
    const bars = buildAlarmAnalysisBars([
      {name: '设备 C', count: 3},
      {name: '设备 A', count: 12},
      {name: '设备 B', count: 7}
    ]);

    expect(bars.map((bar) => bar.name)).toEqual(['设备 A', '设备 B', '设备 C']);
    expect(bars[0].rank).toBe(1);
    expect(getAlarmAnalysisRankTone(1)).toBe('critical');
    expect(getAlarmAnalysisRankTone(4)).toBe('neutral');
  });

  it('maps every设备直属父资产返回值 without dropping zero-alarm sites', () => {
    const rows = mapAlarmAnalysisSiteRows([
      {assetName: '有告警站点', alarmCount: 3, handledRate: 50},
      {assetName: '无告警站点', alarmCount: 0, handledRate: 0}
    ]);

    expect(rows.map((row) => row.name)).toEqual(['有告警站点', '无告警站点']);
    expect(rows[1].count).toBe(0);
  });
});
