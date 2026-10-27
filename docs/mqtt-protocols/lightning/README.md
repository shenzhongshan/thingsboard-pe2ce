# 防雷设备 MQTT 通讯协议

按《MQTT通讯协议-型号文档标准模板》七节格式逐型号编制，范围为源表开头至 ESM3，共 12 个型号。当前为 V1.1 对接评审稿，各文档第 6 节保留公共配置交互说明，可读写配置参数按各型号 Modbus 文档逐项列出，MQTT 配置键留空。

| 型号 | 产品名称 | 测点数 | 文档 |
| --- | --- | --- | --- |
| DES4 | FS智能防雷监测器（四要素） | 4 | [DES4-MQTT通讯协议.md](DES4-MQTT通讯协议.md) |
| DES9 | FS智能防雷监测器（九要素） | 24 | [DES9-MQTT通讯协议.md](DES9-MQTT通讯协议.md) |
| DESA | FS智能防雷监测器（全要素） | 27 | [DESA-MQTT通讯协议.md](DESA-MQTT通讯协议.md) |
| G1A3 | FR接地电阻监测仪 | 5 | [G1A3-MQTT通讯协议.md](G1A3-MQTT通讯协议.md) |
| M1AI | FL雷电流监测仪 | 8 | [M1AI-MQTT通讯协议.md](M1AI-MQTT通讯协议.md) |
| L2AI | FL瞬态电流监测仪 | 8 | [L2AI-MQTT通讯协议.md](L2AI-MQTT通讯协议.md) |
| DEM2 | FSS智能型电涌保护器（单相无漏流） | 13 | [DEM2-MQTT通讯协议.md](DEM2-MQTT通讯协议.md) |
| DEM4 | FSS智能型电涌保护器（三相无漏流） | 22 | [DEM4-MQTT通讯协议.md](DEM4-MQTT通讯协议.md) |
| DEA2 | FSS智能型电涌保护器（单相全电流） | 16 | [DEA2-MQTT通讯协议.md](DEA2-MQTT通讯协议.md) |
| DEA4 | FSS智能型电涌保护器（三相全电流） | 31 | [DEA4-MQTT通讯协议.md](DEA4-MQTT通讯协议.md) |
| ESM0 | ESM智能防雷监测终端（基础版） | 5 | [ESM0-MQTT通讯协议.md](ESM0-MQTT通讯协议.md) |
| ESM3 | ESM全要素SPD监测（旗舰版） | 27 | [ESM3-MQTT通讯协议.md](ESM3-MQTT通讯协议.md) |

## 编制依据

- 测点来源：`新版协议命名规则(更新到ESM3).xlsx`，工作表“微物联产品MQTT点表”，第 2—208 行。
- 只读字段沿用此前确认的平台键名和保留范围；表内未列出的字段已删除，不再以平台扩展字段补回。
- 遥测外壳按《设备MQTT接入协议指南》V1.0 调整为直连 `ts/values` 或网关子设备名称分组结构；具体固件适配版本待确认。
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
- DESA：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- DESA：源表 `SPD_I1hLB` 改用平台键 `SPD_I1hB`。
- DESA：源表 `SPD_I1hLC` 改用平台键 `SPD_I1hC`。
- DESA：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- DESA：源表 `SPD_I24hLB` 改用平台键 `SPD_I24hB`。
- DESA：源表 `SPD_I24hLC` 改用平台键 `SPD_I24hC`。
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
- DEA2：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- DEA2：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- DEA4：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- DEA4：源表 `SPD_I1hLB` 改用平台键 `SPD_I1hB`。
- DEA4：源表 `SPD_I1hLC` 改用平台键 `SPD_I1hC`。
- DEA4：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- DEA4：源表 `SPD_I24hLB` 改用平台键 `SPD_I24hB`。
- DEA4：源表 `SPD_I24hLC` 改用平台键 `SPD_I24hC`。
- ESM0：源表 `Temp1` 改用平台键 `Temp`。
- ESM3：源表 `SPD_I1hLA` 改用平台键 `SPD_I1hA`。
- ESM3：源表 `SPD_I1hLB` 改用平台键 `SPD_I1hB`。
- ESM3：源表 `SPD_I1hLC` 改用平台键 `SPD_I1hC`。
- ESM3：源表 `SPD_I24hLA` 改用平台键 `SPD_I24hA`。
- ESM3：源表 `SPD_I24hLB` 改用平台键 `SPD_I24hB`。
- ESM3：源表 `SPD_I24hLC` 改用平台键 `SPD_I24hC`。

- SPD_Alarm：源表bit0“遥信脱扣”与已确认的编码0“正常”冲突，未将“遥信脱扣”另行猜配为其他整数码。
- G1A3：ResDecimal原表单位为Ω，字段名称为阻值小数位；保留原表信息，不自行定义缩放规则，需产品方复核单位。

## 2026-10-27 只读字段范围调整

按用户提供的《新版协议命名规则(更新到ESM3).xlsx》仅核对只读字段是否保留。删除 DES9、DESA、DEM2、DEM4、DEA2、DEA4、ESM3 中表内未列出的 SPD_RULDay，同步更新报文示例及数量；保留现有字段别名、单位、编码和日期字段的既有处理，不按 Modbus 文档补回表内未列出的只读字段。

## 2026-10-27 新模板适配

全部型号采用七节结构：产品说明、数据上报报文、测点定义、状态及告警码、设备属性、可读写配置参数、修订记录。
