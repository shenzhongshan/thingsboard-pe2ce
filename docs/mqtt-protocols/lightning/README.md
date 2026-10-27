# 防雷设备 MQTT 通讯协议

按已确认的五节格式逐型号编制，范围为源表开头至 ESM3，共 12 个型号。

| 型号 | 产品名称 | 测点数 | 文档 |
| --- | --- | --- | --- |
| DES4 | FS智能防雷监测器（四要素） | 4 | [DES4-MQTT通讯协议.md](DES4-MQTT通讯协议.md) |
| DES9 | FS智能防雷监测器（九要素） | 25 | [DES9-MQTT通讯协议.md](DES9-MQTT通讯协议.md) |
| DESA | FS智能防雷监测器（全要素） | 28 | [DESA-MQTT通讯协议.md](DESA-MQTT通讯协议.md) |
| G1A3 | FR接地电阻监测仪 | 5 | [G1A3-MQTT通讯协议.md](G1A3-MQTT通讯协议.md) |
| M1AI | FL雷电流监测仪 | 8 | [M1AI-MQTT通讯协议.md](M1AI-MQTT通讯协议.md) |
| L2AI | FL瞬态电流监测仪 | 8 | [L2AI-MQTT通讯协议.md](L2AI-MQTT通讯协议.md) |
| DEM2 | FSS智能型电涌保护器（单相无漏流） | 14 | [DEM2-MQTT通讯协议.md](DEM2-MQTT通讯协议.md) |
| DEM4 | FSS智能型电涌保护器（三相无漏流） | 23 | [DEM4-MQTT通讯协议.md](DEM4-MQTT通讯协议.md) |
| DEA2 | FSS智能型电涌保护器（单相全电流） | 17 | [DEA2-MQTT通讯协议.md](DEA2-MQTT通讯协议.md) |
| DEA4 | FSS智能型电涌保护器（三相全电流） | 32 | [DEA4-MQTT通讯协议.md](DEA4-MQTT通讯协议.md) |
| ESM0 | ESM智能防雷监测终端（基础版） | 5 | [ESM0-MQTT通讯协议.md](ESM0-MQTT通讯协议.md) |
| ESM3 | ESM全要素SPD监测（旗舰版） | 28 | [ESM3-MQTT通讯协议.md](ESM3-MQTT通讯协议.md) |

## 编制依据

- 测点来源：`新版协议命名规则(更新到ESM3).xlsx`，工作表“微物联产品MQTT点表”，第 2—208 行。
- 按用户确认采用平台当前键名及此前实际报文；完整键集合与 smartLightningRealtimeMetricDefinitions 逐型号核对。
- 中文名称与单位原则上来自产品测点表；寄存器系数不用于 MQTT 数值的二次换算。
- 告警码采用已确认的整数枚举，6为温升异常，0为正常；未沿用源表bit位码写法。
- 本目录未改动原Excel、平台源代码或转换器。

## 与源表及平台的核对说明（内部审核）

- DES9：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- DES9：源表 `SPD_I1hLB` 改用平台键 `SPD_I1hB`。
- DES9：源表 `SPD_I1hLC` 改用平台键 `SPD_I1hC`。
- DES9：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- DES9：源表 `SPD_I24hLB` 改用平台键 `SPD_I24hB`。
- DES9：源表 `SPD_I24hLC` 改用平台键 `SPD_I24hC`。
- DES9：补入平台及此前实际报文已有的 `SPD_RULDay`，源表本型号未列出。
- DESA：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- DESA：源表 `SPD_I1hLB` 改用平台键 `SPD_I1hB`。
- DESA：源表 `SPD_I1hLC` 改用平台键 `SPD_I1hC`。
- DESA：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- DESA：源表 `SPD_I24hLB` 改用平台键 `SPD_I24hB`。
- DESA：源表 `SPD_I24hLC` 改用平台键 `SPD_I24hC`。
- DESA：补入平台及此前实际报文已有的 `SPD_RULDay`，源表本型号未列出。
- M1AI：源表第 67 行 `Year` 未列入文档，平台当前无此测点。
- M1AI：源表第 68 行 `Month` 未列入文档，平台当前无此测点。
- M1AI：源表第 69 行 `Day` 未列入文档，平台当前无此测点。
- M1AI：源表 `PeakValue` 改用平台键 `PeakCur`。
- L2AI：源表第 79 行 `Year` 未列入文档，平台当前无此测点。
- L2AI：源表第 80 行 `Month` 未列入文档，平台当前无此测点。
- L2AI：源表第 81 行 `Day` 未列入文档，平台当前无此测点。
- L2AI：源表 `PeakValue` 改用平台键 `PeakCur`。
- L2AI：单位能量按源表记为 J/Ω；平台公共显示单位为 kJ/Ω，单位冲突待核实，未自动换算数值。
- L2AI：极性按源表0正1负；平台当前映射0负1正，与源表冲突，需核实。
- DEM2：补入平台及此前实际报文已有的 `SPD_RULDay`，源表本型号未列出。
- DEM4：补入平台及此前实际报文已有的 `SPD_RULDay`，源表本型号未列出。
- DEA2：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- DEA2：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- DEA2：补入平台及此前实际报文已有的 `SPD_RULDay`，源表本型号未列出。
- DEA4：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- DEA4：源表 `SPD_I1hLB` 改用平台键 `SPD_I1hB`。
- DEA4：源表 `SPD_I1hLC` 改用平台键 `SPD_I1hC`。
- DEA4：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- DEA4：源表 `SPD_I24hLB` 改用平台键 `SPD_I24hB`。
- DEA4：源表 `SPD_I24hLC` 改用平台键 `SPD_I24hC`。
- DEA4：补入平台及此前实际报文已有的 `SPD_RULDay`，源表本型号未列出。
- ESM0：源表 `Temp1` 改用平台键 `Temp`。
- ESM3：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- ESM3：源表 `SPD_I1hLB` 改用平台键 `SPD_I1hB`。
- ESM3：源表 `SPD_I1hLC` 改用平台键 `SPD_I1hC`。
- ESM3：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- ESM3：源表 `SPD_I24hLB` 改用平台键 `SPD_I24hB`。
- ESM3：源表 `SPD_I24hLC` 改用平台键 `SPD_I24hC`。
- ESM3：补入平台及此前实际报文已有的 `SPD_RULDay`，源表本型号未列出。

- SPD_Alarm：源表bit0“遥信脱扣”与已确认的编码0“正常”冲突，未将“遥信脱扣”另行猜配为其他整数码。
- G1A3：ResDecimal原表单位为Ω，字段名称为阻值小数位；保留原表信息，不自行定义缩放规则，需产品方复核单位。

## 验证结果

12 份文档共 197 个型号测点定义，已逐份校验章节数量、JSON语法、报文键与测点表键、平台键集合一致性及 slaveid 拼写。未进行设备固件联调。
