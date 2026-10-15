import {resolveSmartLightningDeviceModel} from './smart-lightning-device-detail.data';

/**
 * 文件职责：数据监测设备详情页的静态产品参数说明。
 *
 * 参数以“说明书”中的技术参数表为主；“通讯协议”仅用于核对终端型号，不替代技术参数。
 * 不读取或修改设备实时遥测。实时数据定义仍由 smart-lightning-device-detail.data.ts 负责。
 */

export interface SmartLightningProductSpecificationItem {
  label: string;
  value: string;
}

export interface SmartLightningProductSpecification {
  /** 页面显示的正式产品名称。 */
  productName: string;
  /** 说明书或通讯协议中的型号。 */
  model: string;
  /** 详情卡片中显示的技术参数。 */
  items: readonly SmartLightningProductSpecificationItem[];
}

/**
 * 当前系统设备配置名称到产品资料的映射。
 * 每个型号只保留供电、保护能力、核心监测范围或通道等关键参数；
 * 尺寸、重量、材料和环境条件等辅助信息不在详情页展开。
 * 资料暂缺的型号不虚构技术参数。
 */
export const SMART_LIGHTNING_PRODUCT_SPECIFICATIONS: Readonly<Record<string, SmartLightningProductSpecification>> = {
  DES4: {
    productName: 'FS防雷器监测模块（四要素）',
    model: 'FS-00011',
    items: [
      {label: '工作电压', value: 'DC 12 V'},
      {label: '监测功能', value: '遥信状态、空开状态、接地状态、雷击计数'},
      {label: '雷击计数', value: '0～9999 次，最小触发 0.1 kA'}
    ]
  },
  DES9: {
    productName: 'FS防雷器监测模块（九要素）',
    model: 'FS-03211',
    items: [
      {label: '工作电压', value: 'DC 12 V'},
      {label: '监测功能', value: '遥信、空开、接地、雷击计数、3路漏流、2路温度'},
      {label: '雷击计数', value: '0～9999 次，最小触发 0.1 kA'},
      {label: '温度采集', value: '2 路，-20～105 ℃，精度 ±0.5 ℃'},
      {label: '漏流采集', value: '3 路，50～1500 μA，精度 ±10 μA'}
    ]
  },
  DESA: {
    productName: 'FS防雷器监测模块（全要素）',
    model: 'FS-33211',
    items: [
      {label: '工作电压', value: 'DC 12 V'},
      {label: '雷击计数', value: '0～9999 次，最小触发 0.1 kA'},
      {label: '温度采集', value: '2 路，-20～105 ℃，精度 ±0.5 ℃'},
      {label: '漏流采集', value: '3 路，50～1500 μA，精度 ±10 μA'},
      {label: '电压采集', value: '3 路，0～600.0 V，精度 ±1 V'},
      {label: '寿命预估', value: '1 路，0～100 %'}
    ]
  },
  DEM2: {
    productName: 'FSS智能型电涌保护器（单相无漏流）',
    model: 'FSS-1XX00，2P系列',
    items: [
      {label: '工作电压', value: 'AC 220 V ±20%'},
      {label: '最大持续运行电压 Uc', value: '385 V'},
      {label: '放电能力 In / Imax', value: 'In 10/20/30/40 kA；Imax 20/40/60/80 kA（按具体规格）'},
      {label: '电压保护水平 Up', value: '1.5/1.8/2.0/2.2 kV（按具体规格）'},
      {label: '级数', value: '2P'}
    ]
  },
  DEM4: {
    productName: 'FSS智能型电涌保护器（三相无漏流）',
    model: 'FSS-2XX00，4P系列',
    items: [
      {label: '工作电压', value: 'AC 220 V ±20%'},
      {label: '最大持续运行电压 Uc', value: '385 V'},
      {label: '放电能力 In / Imax', value: 'In 10/20/30/40 kA；Imax 20/40/60/80 kA（按具体规格）'},
      {label: '电压保护水平 Up', value: '1.5/1.8/2.0/2.2 kV（按具体规格）'},
      {label: '级数', value: '4P'}
    ]
  },
  DEA2: {
    productName: 'FSS智能型电涌保护器（单相全电流）',
    model: 'FSS全电流版本，2P',
    items: [
      {label: '工作电压', value: 'AC 220 V ±20%'},
      {label: '最大持续运行电压 Uc', value: '385 V'},
      {label: '放电能力 In / Imax', value: 'In 10/20/30/40 kA；Imax 20/40/60/80 kA（按具体规格）'},
      {label: '电压保护水平 Up', value: '1.5/1.8/2.0/2.2 kV（按具体规格）'},
      {label: '级数', value: '2P'},
      {label: '漏流监测', value: '1 路全电流监测'}
    ]
  },
  DEA4: {
    productName: 'FSS智能型电涌保护器（三相全电流）',
    model: 'FSS全电流版本，4P',
    items: [
      {label: '工作电压', value: 'AC 220 V ±20%'},
      {label: '最大持续运行电压 Uc', value: '385 V'},
      {label: '放电能力 In / Imax', value: 'In 10/20/30/40 kA；Imax 20/40/60/80 kA（按具体规格）'},
      {label: '电压保护水平 Up', value: '1.5/1.8/2.0/2.2 kV（按具体规格）'},
      {label: '级数', value: '4P'},
      {label: '漏流监测', value: '3 路全电流监测'}
    ]
  },
  ESM0: {
    productName: 'ESM防雷监测模组（基础版）',
    model: 'ESM-21001',
    items: [
      {label: '供电电压', value: 'AC 160～280 V（额定 220 V）'},
      {label: '开关量输入', value: '2 路干接点输入，内置电源'},
      {label: '接地状态', value: '接地回路监测'},
      {label: '雷击计数', value: '0～9999 次，最小触发 0.1 kA'},
      {label: '温度监测', value: '1 路 NTC，-30～125 ℃，精度 ±1 ℃'}
    ]
  },
  ESM3: {
    productName: 'ESM防雷监测模组（旗舰版）',
    model: 'ESM-22X12（协议型号）',
    items: [
      {label: '供电电压', value: 'AC 160～280 V（额定 220 V）'},
      {label: '雷击计数', value: '0～9999 次，最小触发 0.1 kA'},
      {label: '温度监测', value: '2 路 NTC，-30～125 ℃，精度 ±1 ℃'},
      {label: '漏流监测', value: '3 路，50～2400 μA，精度 ±20 μA'},
      {label: '电压监测', value: '3 路，0～600.0 V，精度 ±1 V'},
      {label: '寿命预估', value: '1 路，0～100 %'}
    ]
  },
  G1A3: {
    productName: 'FR接地电阻监测模块',
    model: 'FR-01311',
    items: [
      {label: '阻值范围', value: '0.01～200 Ω'},
      {label: '分辨率', value: '0.01 Ω'},
      {label: '测量方法', value: '精密三线测量'},
      {label: '测量时间', value: '1 分钟/次'},
      {label: '测量精度', value: '±2%rdg±3dgt'}
    ]
  },
  M1AI: {
    productName: 'FL雷电流监测模块（峰值型）',
    model: 'FL-012X2',
    items: [
      {label: '雷电流类型', value: '8/20 μs、10/350 μs'},
      {label: '采集范围', value: '1～120 kA，精度 ±5%'},
      {label: '数据存储', value: '100 组，自动覆盖最早记录'},
      {label: '能量信息', value: '电荷量、单位能量'}
    ]
  },
  L2AI: {
    productName: 'FL瞬态电流监测模块',
    model: 'FL-11122',
    items: [
      {label: '雷电流类型', value: '8/20 μs、10/350 μs'},
      {label: '采集范围', value: '1～120 kA，精度 ±5%'},
      {label: '数据存储', value: '100 组，自动覆盖最早记录'},
      {label: '能量信息', value: '电荷量、单位能量'}
    ]
  }
};

/** 按设备配置名称取得详情页要显示的产品参数；无资料时返回 undefined。 */
export const getSmartLightningProductSpecification = (
  deviceProfileName?: string
): SmartLightningProductSpecification | undefined =>
  resolveSmartLightningDeviceModel(deviceProfileName)
    ? SMART_LIGHTNING_PRODUCT_SPECIFICATIONS[resolveSmartLightningDeviceModel(deviceProfileName)!]
    : undefined;
