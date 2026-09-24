/**
 * 文件职责：把原生遥测删除策略转换为平台遥测删除接口参数。
 */
export interface LogTelemetryDeleteResult {
  strategy: string;
  startDateTime?: Date;
  endDateTime?: Date;
  rewriteLatest: boolean;
}

/** 删除接口使用的参数，字段语义与 AttributeService.deleteEntityTimeseries 保持一致。 */
export interface LogTelemetryDeleteOptions {
  deleteAllDataForKeys: boolean;
  startTs?: number;
  endTs?: number;
  rewriteLatestIfDeleted: boolean;
  deleteLatest: boolean;
}

/**
 * 按 ThingsBoard 原生页面的策略语义生成单个遥测键的删除参数。
 *
 * <p>最新值策略按最新时间戳构造 [ts, ts + 1) 的时间范围；缺少有效时间戳、
 * 自定义时间范围无效或策略未知时返回 null，防止错误范围导致误删。</p>
 */
export function buildLogTelemetryDeleteOptions(result: LogTelemetryDeleteResult,
                                               latestTimestamp?: number): LogTelemetryDeleteOptions | null {
  switch (result?.strategy) {
    case 'DELETE_ALL_DATA':
      return {
        deleteAllDataForKeys: true,
        rewriteLatestIfDeleted: false,
        deleteLatest: true
      };
    case 'DELETE_ALL_DATA_EXCEPT_LATEST_VALUE':
      return {
        deleteAllDataForKeys: true,
        rewriteLatestIfDeleted: false,
        deleteLatest: false
      };
    case 'DELETE_LATEST_VALUE':
      if (!Number.isFinite(latestTimestamp) || latestTimestamp < 0) {
        return null;
      }
      return {
        deleteAllDataForKeys: false,
        startTs: latestTimestamp,
        endTs: latestTimestamp + 1,
        rewriteLatestIfDeleted: result.rewriteLatest,
        deleteLatest: true
      };
    case 'DELETE_ALL_DATA_FOR_TIME_PERIOD': {
      const startTs = result.startDateTime?.getTime();
      const endTs = result.endDateTime?.getTime();
      if (!Number.isFinite(startTs) || !Number.isFinite(endTs) || endTs <= startTs) {
        return null;
      }
      return {
        deleteAllDataForKeys: false,
        startTs,
        endTs,
        rewriteLatestIfDeleted: result.rewriteLatest,
        deleteLatest: true
      };
    }
    default:
      return null;
  }
}
