/** 文件职责：告警分析模块的 smart-lightning-alarm-analysis.data.spec，验证数据模型、映射与纯函数行为。 */
/** 告警分析纯函数单测：条形数据构建与排名色调（node assert 轻量运行）。 */
import {
  ALERT_ANALYSIS_PERIODS,
  buildAlarmAnalysisBars,
  getAlarmAnalysisPeriodLabel,
  getAlarmAnalysisRankTone,
  calculateAlarmCoveragePercent,
  mapAlarmAnalysisStatusRows,
  mapAlarmAnalysisSiteRows,
  mapAlarmAnalysisTypeRows
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
      {assetName: '处理率较低但告警较多', alarmCount: 10, handledRate: 20},
      {assetName: '处理率较高', alarmCount: 3, handledRate: 80},
      {assetName: '无告警站点', alarmCount: 0, handledRate: 0}
    ]);

    expect(rows.map((row) => row.name)).toEqual(['处理率较高', '处理率较低但告警较多', '无告警站点']);
    expect(rows[2].count).toBe(0);
  });

  it('maps the real alarm type ranking and keeps every record count', () => {
    const rows = mapAlarmAnalysisTypeRows([
      {type: '温度异常', count: 1},
      {type: '现场报警', count: 4},
      {type: '', count: 2}
    ]);

    expect(rows.map((row) => row.name)).toEqual(['现场报警', '未分类', '温度异常']);
    expect(rows.map((row) => row.count)).toEqual([4, 2, 1]);
  });

  it('maps the four alarm states and calculates device coverage safely', () => {
    const rows = mapAlarmAnalysisStatusRows([
      {status: 'ACTIVE_UNACK', count: 4},
      {status: 'CLEARED_ACK', count: 2},
      {status: 'ACTIVE_ACK', count: 3},
      {status: 'CLEARED_UNACK', count: 1}
    ]);

    expect(rows.map((row) => row.name)).toEqual(['未确认未清除', '已确认未清除', '已确认已清除', '未确认已清除']);
    expect(rows[0].percentage).toBe(100);
    expect(calculateAlarmCoveragePercent(3, 12)).toBe(25);
    expect(calculateAlarmCoveragePercent(3, 0)).toBe(0);
  });
});
