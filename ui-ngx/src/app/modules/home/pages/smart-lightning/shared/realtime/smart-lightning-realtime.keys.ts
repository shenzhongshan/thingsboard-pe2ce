import {smartLightningRealtimeMetricDefinitions} from '../../monitoring/smart-lightning-device-detail.data';
import {SMART_LIGHTNING_WAVEFORM_KEY} from '../telemetry/smart-lightning-measurements';

/** 防雷自身的规范测点集合；波形和GPS仍保留既有实时能力，不混入其他模块测点。 */
export const SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS: readonly string[] = Array.from(new Set([
  ...Object.values(smartLightningRealtimeMetricDefinitions).flatMap((definitions) => definitions.map((item) => item.key)),
  SMART_LIGHTNING_WAVEFORM_KEY
]));
