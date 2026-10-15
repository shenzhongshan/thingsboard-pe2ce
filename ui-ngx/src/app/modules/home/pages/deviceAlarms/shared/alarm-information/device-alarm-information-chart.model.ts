/** 文件职责：定义共享报警信息图表接收的展示数据，不包含接口或模块筛选逻辑。 */

/** 单个告警名称及其统计数量；label 使用调用方提供的告警名称，null 表示数量尚不可用。 */
export interface DeviceAlarmInformationChartItem {
  /** 图表横轴显示的原始告警名称，不由共享组件推断或改写。 */
  label: string;
  /** 当前统计周期内的报警数量。 */
  count: number | null;
}
