/** 文件职责：为智能防雷看板周期最值提供纯增量计算与滚动窗口判断。 */

/** 一个落在当前滚动窗口内的遥测样本及其悬浮信息来源。 */
export interface SmartLightningPeriodicExtremaSample {
  readonly value: number;
  readonly deviceName: string;
  readonly parameter?: string;
  readonly timestamp: number;
}

/** 地网与 SPD 周期最值共有的字段。 */
export interface SmartLightningPeriodicExtrema {
  maximum: number | null;
  maximumDeviceName?: string | null;
  maximumParameter?: string | null;
  maximumTime?: number | null;
  minimum: number | null;
  minimumDeviceName?: string | null;
  minimumParameter?: string | null;
  minimumTime?: number | null;
}

/** 雷电峰值周期范围与设备、时间、极性来源。 */
export interface SmartLightningPeakExtrema {
  maxPeakCurrent: number | null;
  maxPeakDeviceName?: string | null;
  maxPeakTime?: number | null;
  maxPeakPolarity?: string | null;
  minPeakCurrent: number | null;
  minPeakDeviceName?: string | null;
  minPeakTime?: number | null;
  minPeakPolarity?: string | null;
}

/**
 * 将 WebSocket 样本并入已有周期最值；只比较样本，不伪造或换算遥测值。
 *
 * <p>后端周期是滚动窗口：日 24 小时、周 7 天、月 30 天。越界、无效或非有限
 * 样本直接忽略。源字段与数值在同一次替换中更新，避免 tooltip 指向旧设备/时间。</p>
 */
export function updateSmartLightningPeriodicExtrema<T extends SmartLightningPeriodicExtrema>(
  current: T,
  samples: readonly SmartLightningPeriodicExtremaSample[],
  period: string,
  now: number,
  absoluteValue = false
): T {
  const duration = getSmartLightningPeriodDuration(period);
  if (duration == null || !Number.isFinite(now)) {
    return current;
  }

  let updated: T | null = null;
  for (const sample of samples) {
    if (!isSmartLightningSampleInPeriod(sample.timestamp, period, now) || !Number.isFinite(sample.value)) {
      continue;
    }
    const value = absoluteValue ? Math.abs(sample.value) : sample.value;
    const result = updated || {...current} as T;
    if (result.maximum == null || value > result.maximum) {
      result.maximum = value;
      result.maximumDeviceName = sample.deviceName;
      result.maximumParameter = sample.parameter ?? null;
      result.maximumTime = sample.timestamp;
      updated = result;
    }
    if (result.minimum == null || value < result.minimum) {
      result.minimum = value;
      result.minimumDeviceName = sample.deviceName;
      result.minimumParameter = sample.parameter ?? null;
      result.minimumTime = sample.timestamp;
      updated = result;
    }
  }
  return updated || current;
}

/**
 * 检查增量是否覆盖了当前最值来源的同一设备、测点和时间戳，但值已变化。
 * 最新值订阅无法撤销旧历史样本，因此这种覆盖必须回读现有周期聚合接口校准。
 */
export function doesSmartLightningSampleInvalidateExtrema<T extends SmartLightningPeriodicExtrema>(
  current: T,
  samples: readonly SmartLightningPeriodicExtremaSample[],
  absoluteValue = false,
  period?: string,
  now = Date.now()
): boolean {
  return samples.some((sample) => {
    if (!Number.isFinite(sample.value) ||
      (period != null && !isSmartLightningSampleInPeriod(sample.timestamp, period, now))) {
      return false;
    }
    const value = absoluteValue ? Math.abs(sample.value) : sample.value;
    const replacesMaximum = sample.deviceName === current.maximumDeviceName &&
      sample.parameter === current.maximumParameter && sample.timestamp === current.maximumTime &&
      current.maximum != null && value !== current.maximum;
    const replacesMinimum = sample.deviceName === current.minimumDeviceName &&
      sample.parameter === current.minimumParameter && sample.timestamp === current.minimumTime &&
      current.minimum != null && value !== current.minimum;
    return replacesMaximum || replacesMinimum;
  });
}

/** 检查峰值来源样本被覆盖；雷电流后端以峰值绝对值参与最值比较。 */
export function doesSmartLightningPeakSampleInvalidateExtrema<T extends SmartLightningPeakExtrema>(
  current: T,
  samples: readonly SmartLightningPeriodicExtremaSample[],
  period?: string,
  now = Date.now()
): boolean {
  return samples.some((sample) => {
    if (!Number.isFinite(sample.value) ||
      (period != null && !isSmartLightningSampleInPeriod(sample.timestamp, period, now))) {
      return false;
    }
    const value = Math.abs(sample.value);
    const replacesMaximum = sample.deviceName === current.maxPeakDeviceName &&
      sample.parameter === 'PeakValue' && sample.timestamp === current.maxPeakTime &&
      current.maxPeakCurrent != null && value !== current.maxPeakCurrent;
    const replacesMinimum = sample.deviceName === current.minPeakDeviceName &&
      sample.parameter === 'PeakValue' && sample.timestamp === current.minPeakTime &&
      current.minPeakCurrent != null && value !== current.minPeakCurrent;
    return replacesMaximum || replacesMinimum;
  });
}

/** 将周期内新雷电峰值并入最大/最小峰值，同时更新对应极性来源。 */
export function updateSmartLightningPeakExtrema<T extends SmartLightningPeakExtrema>(
  current: T,
  samples: readonly (SmartLightningPeriodicExtremaSample & {readonly polarity?: string | null})[],
  period: string,
  now: number
): T {
  const duration = getSmartLightningPeriodDuration(period);
  if (duration == null || !Number.isFinite(now)) {
    return current;
  }

  let updated: T | null = null;
  for (const sample of samples) {
    if (!isSmartLightningSampleInPeriod(sample.timestamp, period, now) || !Number.isFinite(sample.value)) {
      continue;
    }
    const value = Math.abs(sample.value);
    const result = updated || {...current} as T;
    if (result.maxPeakCurrent == null || value > result.maxPeakCurrent) {
      result.maxPeakCurrent = value;
      result.maxPeakDeviceName = sample.deviceName;
      result.maxPeakTime = sample.timestamp;
      result.maxPeakPolarity = sample.polarity ?? null;
      updated = result;
    } else if (value === result.maxPeakCurrent && sample.deviceName === result.maxPeakDeviceName &&
      sample.timestamp === result.maxPeakTime && sample.polarity != null &&
      sample.polarity !== result.maxPeakPolarity) {
      // 峰值与极性可能分两个遥测事件到达；同一峰值样本到齐后只校准来源标签。
      result.maxPeakPolarity = sample.polarity;
      updated = result;
    }
    if (result.minPeakCurrent == null || value < result.minPeakCurrent) {
      result.minPeakCurrent = value;
      result.minPeakDeviceName = sample.deviceName;
      result.minPeakTime = sample.timestamp;
      result.minPeakPolarity = sample.polarity ?? null;
      updated = result;
    } else if (value === result.minPeakCurrent && sample.deviceName === result.minPeakDeviceName &&
      sample.timestamp === result.minPeakTime && sample.polarity != null &&
      sample.polarity !== result.minPeakPolarity) {
      result.minPeakPolarity = sample.polarity;
      updated = result;
    }
  }
  return updated || current;
}

/** 判断遥测时间戳是否在后端同口径的左闭右开滚动窗口中。 */
export function isSmartLightningSampleInPeriod(timestamp: number, period: string, now: number): boolean {
  const duration = getSmartLightningPeriodDuration(period);
  return duration != null && Number.isFinite(timestamp) && Number.isFinite(now) &&
    timestamp >= now - duration && timestamp < now;
}

/** 获取后端固定使用的滚动周期长度，非法周期不参与本地增量。 */
export function getSmartLightningPeriodDuration(period: string): number | null {
  const dayMilliseconds = 24 * 60 * 60 * 1000;
  if (period === 'day') {
    return dayMilliseconds;
  }
  if (period === 'week') {
    return 7 * dayMilliseconds;
  }
  if (period === 'month') {
    return 30 * dayMilliseconds;
  }
  return null;
}
