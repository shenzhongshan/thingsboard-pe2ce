package org.thingsboard.server.service.monitoring.util;

import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** SPD型号测点白名单：只限定查询和聚合范围，不改变状态含义、单位、阈值或告警数量。 */
public final class SpdTelemetryKeyDefinitions {
    private SpdTelemetryKeyDefinitions() { }

    /** 与用户确认的各型号最新协议一致；不包含旧键或其他型号专属测点。 */
    private static final Map<String, Set<String>> KEYS_BY_MODEL = Map.ofEntries(
            Map.entry("DES4", Set.of(
                    "RemoteSig", "McbStatus", "GndStatus", "StrikeCnt")),
            Map.entry("DES9", Set.of(
                    "RemoteSig", "McbStatus", "GndStatus", "StrikeCnt", "LeakCurrA",
                    "LeakCurrB", "LeakCurrC", "Temp1", "Temp2", "SPD_RUL",
                    "SPD_Status", "SPD_RULDay", "SPD_Alarm", "SPD_WorstPh", "SPD_RULA",
                    "SPD_RULB", "SPD_RULC", "SPD_Reason", "SPD_I1hA", "SPD_I1hB",
                    "SPD_I1hC", "SPD_I24hA", "SPD_I24hB", "SPD_I24hC", "SPD_DeltaT")),
            Map.entry("DESA", Set.of(
                    "RemoteSig", "McbStatus", "GndStatus", "StrikeCnt", "PhAVolt",
                    "PhBVolt", "PhCVolt", "LeakCurrA", "LeakCurrB", "LeakCurrC",
                    "Temp1", "Temp2", "SPD_RUL", "SPD_Status", "SPD_RULDay",
                    "SPD_Alarm", "SPD_WorstPh", "SPD_RULA", "SPD_RULB", "SPD_RULC",
                    "SPD_Reason", "SPD_I1hA", "SPD_I1hB", "SPD_I1hC", "SPD_I24hA",
                    "SPD_I24hB", "SPD_I24hC", "SPD_DeltaT")),
            Map.entry("DEM2", Set.of(
                    "PhAStatus", "GndStatus", "TripSW1", "TripSW2", "StrikeCnt",
                    "Temp1", "Temp2", "SPD_RUL", "SPD_Status", "SPD_RULDay",
                    "SPD_Alarm", "SPD_RULA", "SPD_Reason", "SPD_DeltaT")),
            Map.entry("DEM4", Set.of(
                    "PhAStatus", "PhBStatus", "PhCStatus", "GndStatus", "TripSW1",
                    "TripSW2", "TripSW3", "TripSW4", "StrikeCnt", "Temp1",
                    "Temp2", "Temp3", "Temp4", "SPD_RUL", "SPD_Status",
                    "SPD_RULDay", "SPD_Alarm", "SPD_WorstPh", "SPD_RULA", "SPD_RULB",
                    "SPD_RULC", "SPD_Reason", "SPD_DeltaT")),
            Map.entry("DEA2", Set.of(
                    "PhAStatus", "GndStatus", "TripSW1", "TripSW2", "StrikeCnt",
                    "LeakCurrA", "Temp1", "Temp2", "SPD_RUL", "SPD_Status",
                    "SPD_RULDay", "SPD_Alarm", "SPD_RULA", "SPD_Reason", "SPD_I1hA",
                    "SPD_I24hA", "SPD_DeltaT")),
            Map.entry("DEA4", Set.of(
                    "PhAStatus", "PhBStatus", "PhCStatus", "GndStatus", "TripSW1",
                    "TripSW2", "TripSW3", "TripSW4", "StrikeCnt", "LeakCurrA",
                    "LeakCurrB", "LeakCurrC", "Temp1", "Temp2", "Temp3",
                    "Temp4", "SPD_RUL", "SPD_Status", "SPD_RULDay", "SPD_Alarm",
                    "SPD_WorstPh", "SPD_RULA", "SPD_RULB", "SPD_RULC", "SPD_Reason",
                    "SPD_I1hA", "SPD_I1hB", "SPD_I1hC", "SPD_I24hA", "SPD_I24hB",
                    "SPD_I24hC", "SPD_DeltaT")),
            Map.entry("ESM0", Set.of(
                    "RemoteSig", "McbStatus", "GndStatus", "StrikeCnt", "Temp1")),
            Map.entry("ESM3", Set.of(
                    "RemoteSig", "McbStatus", "GndStatus", "StrikeCnt", "PhAVolt",
                    "PhBVolt", "PhCVolt", "LeakCurrA", "LeakCurrB", "LeakCurrC",
                    "Temp1", "Temp2", "SPD_RUL", "SPD_Status", "SPD_RULDay",
                    "SPD_Alarm", "SPD_WorstPh", "SPD_RULA", "SPD_RULB", "SPD_RULC",
                    "SPD_Reason", "SPD_I1hA", "SPD_I1hB", "SPD_I1hC", "SPD_I24hA",
                    "SPD_I24hB", "SPD_I24hC", "SPD_DeltaT")),
            Map.entry("DEP4", Set.of(
                    "RemoteSig", "StrikeCnt", "Temp")));

    /** 复用项目已配置的型号与产品档案别名，不按设备名称猜测产品分类。 */
    private static final Map<String, String> MODEL_BY_ALIAS = Map.ofEntries(
            Map.entry("DES4", "DES4"),
            Map.entry("FS防雷监测模块（四要素）", "DES4"),
            Map.entry("FS防雷监测模块(四要素)", "DES4"),
            Map.entry("FS防雷器监测模块（四要素）", "DES4"),
            Map.entry("FS防雷器监测模块(四要素)", "DES4"),
            Map.entry("DES9", "DES9"),
            Map.entry("FS防雷监测模块（九要素）", "DES9"),
            Map.entry("FS防雷监测模块(九要素)", "DES9"),
            Map.entry("FS防雷器监测模块（九要素）", "DES9"),
            Map.entry("FS防雷器监测模块(九要素)", "DES9"),
            Map.entry("DESA", "DESA"),
            Map.entry("FS防雷监测模块（全要素）", "DESA"),
            Map.entry("FS防雷监测模块(全要素)", "DESA"),
            Map.entry("FS防雷器监测模块（全要素）", "DESA"),
            Map.entry("FS防雷器监测模块(全要素)", "DESA"),
            Map.entry("DEM2", "DEM2"),
            Map.entry("FSS智能型电涌保护器（单相无漏流）", "DEM2"),
            Map.entry("FSS智能型电涌保护器(单相无漏流)", "DEM2"),
            Map.entry("DEM4", "DEM4"),
            Map.entry("FSS智能型电涌保护器（三相无漏流）", "DEM4"),
            Map.entry("FSS智能型电涌保护器(三相无漏流)", "DEM4"),
            Map.entry("DEA2", "DEA2"),
            Map.entry("FSS智能型电涌保护器（单相全电流）", "DEA2"),
            Map.entry("FSS智能型电涌保护器(单相全电流)", "DEA2"),
            Map.entry("DEA4", "DEA4"),
            Map.entry("FSS智能型电涌保护器（三相全电流）", "DEA4"),
            Map.entry("FSS智能型电涌保护器(三相全电流)", "DEA4"),
            Map.entry("ESM0", "ESM0"),
            Map.entry("ESM防雷监测模组（基础版）", "ESM0"),
            Map.entry("ESM防雷监测模组(基础版)", "ESM0"),
            Map.entry("ESM3", "ESM3"),
            Map.entry("ESM防雷监测模组（旗舰版）", "ESM3"),
            Map.entry("ESM防雷监测模组(旗舰版)", "ESM3"),
            Map.entry("DEP4", "DEP4"),
            Map.entry("FSP防雷监测底座", "DEP4"));

    /** 优先读取设备档案，档案为自定义名称时使用明确的产品名称映射；未知型号不猜测测点。 */
    public static Set<String> supportedKeys(MonitoringDeviceContextBO device) {
        String profile = device.getDeviceProfileName();
        String model = profile == null ? null : MODEL_BY_ALIAS.get(profile.trim());
        if (model == null) {
            String productName = device.getProductName();
            model = productName == null ? null : MODEL_BY_ALIAS.get(productName.trim());
        }
        return model == null ? Set.of() : KEYS_BY_MODEL.getOrDefault(model, Set.of());
    }

    /** 从本次汇总需要的键中选出该型号支持的子集，不扩大既有接口职责。 */
    public static List<String> filterKeys(MonitoringDeviceContextBO device, List<String> candidates) {
        Set<String> supported = supportedKeys(device);
        List<String> result = new ArrayList<>();
        for (String key : candidates) {
            if (supported.contains(key)) {
                result.add(key);
            }
        }
        return result;
    }
}
