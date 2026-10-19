package org.thingsboard.server.service.monitoring.util;

import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;

/** 雷电流型号协议差异：按已配置的档案／产品名称选择计数键和极性编码，不修改遥测原值。 */
public final class LightningCurrentProtocolSupport {
    private LightningCurrentProtocolSupport() { }

    /** 识别M1AI型号编码及项目已有产品档案别名；其余型号保持原有协议。 */
    public static boolean isM1ai(MonitoringDeviceContextBO device) {
        String profile = device.getDeviceProfileName();
        return "M1AI".equalsIgnoreCase(profile)
                || "FL雷电流监测模块".equals(profile)
                || "FL雷电流监测模块（峰值型）".equals(profile)
                || "FL雷电流监测模块(峰值型)".equals(profile)
                || "FL雷电流监测模块".equals(device.getProductName());
    }

    /** M1AI只读新版StrikeCnt，不回退旧计数键；L2AI仍使用自身的SurgeCnt。 */
    public static String counterKey(MonitoringDeviceContextBO device) {
        return isM1ai(device) ? "StrikeCnt" : "SurgeCnt";
    }

    /** 两种雷电流型号共用的规范峰值键。 */
    public static final String PEAK_CURRENT_KEY = "PeakCur";

    /** M1AI为0正1负，其他型号沿用0负1正；无效值原样返回，缺失仍为空。 */
    public static String polarityLabel(String value, boolean m1ai) {
        Double numeric = MonitoringValueParser.parseFiniteDouble(value);
        if (numeric != null && numeric == 0D) {
            return m1ai ? "正极性" : "负极性";
        }
        if (numeric != null && numeric == 1D) {
            return m1ai ? "负极性" : "正极性";
        }
        return value;
    }
}
