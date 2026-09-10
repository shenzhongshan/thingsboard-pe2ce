/** 文件职责：共享告警能力模块的 smart-lightning-alarm-display，承载该模块文件的实现细节。 */
/**
 * 告警页面共用的显示映射。
 *
 * 列表、详情弹窗和后续告警子页面都从这里取得严重程度与原生状态文案，
 * 页面组件只负责调用，不在各自文件重复维护同一套中文映射。
 */
export type SmartLightningAlarmSeverityTone = 'critical' | 'major' | 'minor' | 'warning' | 'indeterminate';

const SEVERITY_LABELS: Readonly<Record<SmartLightningAlarmSeverityTone, string>> = {
  critical: '严重',
  major: '重要',
  minor: '次要',
  warning: '警告',
  indeterminate: '不确定'
};

/** 将 ThingsBoard 严重程度编码转换为页面色调。 */
export const getSmartLightningAlarmSeverityTone = (severity?: string): SmartLightningAlarmSeverityTone => {
  switch (severity) {
    case 'CRITICAL': return 'critical';
    case 'MAJOR': return 'major';
    case 'MINOR': return 'minor';
    case 'WARNING': return 'warning';
    default: return 'indeterminate';
  }
};

/** 将严重程度色调转换为中文标签。 */
export const getSmartLightningAlarmSeverityLabel = (tone: SmartLightningAlarmSeverityTone): string =>
  SEVERITY_LABELS[tone];

/** 将 ThingsBoard 原生告警状态转换为列表和详情共用的中文文案。 */
export const getSmartLightningAlarmStatusLabel = (status?: string): string => {
  switch (status) {
    case 'ACTIVE_UNACK': return '未确认未清除';
    case 'ACTIVE_ACK': return '已确认未清除';
    case 'CLEARED_UNACK': return '未确认已清除';
    case 'CLEARED_ACK': return '已确认已清除';
    default: return '--';
  }
};
