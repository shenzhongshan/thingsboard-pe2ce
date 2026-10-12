/** 文件职责：集中定义能源监管看板读取的标准电能指标。 */
package org.thingsboard.server.service.energy_regulation.util;

import java.util.List;

/** 汇总标准键与 ESA 原生键中的总正向有功电能字段，统一按 kWh 处理。 */
public final class EnergyRegulationEnergyMetricDefinitions {

    /** 协议命名规范中的总正向有功电能完整键。 */
    public static final String TOTAL_FORWARD_ACTIVE_ENERGY_KEY = "FwdActET";
    /** 协议命名规范中的总正向有功电能简称。 */
    public static final String TOTAL_FORWARD_ACTIVE_ENERGY_ABBREVIATION = "FPET";
    /** ESA 智能电表直接上报的总正向有功电能键，不与分相电能键重复求和。 */
    public static final String ESA_TOTAL_FORWARD_ACTIVE_ENERGY_KEY = "EPI0";
    /** 同一累计指标可能出现的协议键；查询后每台设备只选其中一个。 */
    public static final List<String> TOTAL_FORWARD_ACTIVE_ENERGY_KEYS = List.of(
            TOTAL_FORWARD_ACTIVE_ENERGY_KEY,
            TOTAL_FORWARD_ACTIVE_ENERGY_ABBREVIATION,
            ESA_TOTAL_FORWARD_ACTIVE_ENERGY_KEY);
    /** 总正向有功电能和用电量差值的统一单位。 */
    public static final String ENERGY_UNIT = "kWh";

    /** 指标定义仅通过静态常量访问。 */
    private EnergyRegulationEnergyMetricDefinitions() {
    }
}
