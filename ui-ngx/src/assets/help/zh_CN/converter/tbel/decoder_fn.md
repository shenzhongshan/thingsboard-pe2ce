#### 上行数据转换器解码函数

<div class="divider"></div>
<br/>

*function Decoder(payload, metadata): object | object[]*

用于将集成的上行消息解析并转换为平台通用格式的 [TBEL{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/tbel/) 函数。

**参数：**

<ul>
  <li><b>payload：</b> <code>any</code> - 是一个 <b>字节数组</b>，包含对应集成产生的原始消息。<br>
    集成产生的 payload 内容类型可能是 JSON、TEXT 或 Binary（Base64）三种之一。
    内容类型本质上只是用于保存调试事件的一个提示，不会影响解码函数的实际逻辑。
    解码函数可以使用 <i>decodeToString</i> 和 <i>decodeToJson</i> 函数将字节数组转换为字符串或 JSON 对象。<br/>
    大多数集成（如 SigFox、LORIOT、ChirpStack、The Things Stack）总是产生 JSON 格式的 payload，
    其中包裹了设备原始二进制 payload 以及一些有用的元数据，例如 RSSI、SNR 等。<br/>
    基于 HTTP 的集成和 CoAP 集成会根据请求头判断 payload 的内容类型。
    同一个集成可能根据请求内容产生 JSON、TEXT 或 BINARY 格式的 payload。<br/>
    MQTT 集成的 payload 始终为 BINARY 类型，因为 MQTT 3.x 之前的版本在 MQTT publish 消息中没有 content-type 字段。
  </li>
  <li><b>metadata：</b> <code>{[key: string]: string}</code> - 是一个元数据键值对映射，包含部分集成特有的字段。<br>
     你可以在集成详情中为每个集成配置额外的元数据。
  </li>
</ul>

**返回值：**

必须返回一个符合以下要求的合法 JSON 文档：

 * **必须** 包含一对 **deviceName** 和 **deviceType**，或一对 **assetName** 和 **assetType** 属性。
   这些属性用于标识设备或资产，因为设备和资产名称在租户范围内是唯一的。
   平台会使用这些参数查找已存在的设备/资产。
   如果找不到对应实体，且该集成启用了"允许创建设备或资产"设置，平台会创建新实体。
   DevEUI、MAC 地址或其他唯一标识符通常被用作设备名称。
   参见 **deviceLabel**，用于添加一个非唯一、用户友好的标签，可在仪表板上代替设备名称使用。
 * 可以包含 **attributes** 对象，表示要赋给该设备/资产的一组服务端属性。
 * 可以包含 **telemetry** 对象/数组，表示该设备/资产的时间序列数据。
 * 可以包含 **customerName** 属性。平台会用它自动将设备分配给对应客户。
   如果该名称的客户不存在，将创建一个新客户。
   该分配只会在当前集成创建设备或资产的过程中发生（见第一条要求）。
   换句话说，如果设备或资产已存在，平台会忽略此参数。
 * 可以包含 **groupName** 属性。平台会用它自动将设备分配到某个实体分组。
   如果该名称的分组不存在，将创建一个新分组。
   该分组会在租户范围内创建（默认），或在客户范围内创建（如果存在 **customerName** 属性）。
   该分配只会在当前集成创建设备或资产的过程中发生（见第一条要求）。
   换句话说，如果设备或资产已存在，平台会忽略此参数。
 * 可以包含 **deviceLabel** 或 **assetLabel** 属性。
   可用于为设备/资产创建非唯一的、用户友好的标签，可在仪表板上使用。
   该分配只会在当前集成创建设备或资产的过程中发生（见第一条要求）。
   换句话说，如果设备或资产已存在，平台会忽略此参数。


下面来看几个解码输出示例。

包含设备名、类型、"serialNumber" 属性以及若干遥测值的简单 JSON。

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/tbel/examples/decoder/simple_json_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="简单 JSON 输出">
</div>

<br>

类似的 JSON，同时还包含设备标签、客户名和分组名。

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/tbel/examples/decoder/label_json_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="带设备标签、客户名和分组名的输出">
</div>

<br>

同样的 JSON，同时包含事件的时间戳。
平台期望时间戳为毫秒级 Unix 纪元时间。
否则将使用服务器时间戳。

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/tbel/examples/decoder/simple_json_output_with_ts"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="带自定义时间戳的输出">
</div>

<br>

最后，数据转换的输出也可以是包含多个设备/资产的对象数组。
每个对象也可以包含多个带不同时间戳的时间序列数据点：

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/tbel/examples/decoder/json_array_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="JSON 数组输出示例">
</div>

<div class="divider"></div>

##### 示例

下表列出了一些示例，包含输入参数、解码函数和预期输出。

<table style="max-width: 1200px;">
<thead>
<tr>
<th style="max-width: 150px; padding-left: 22px;">
<b>名称</b>
</th>
<th style="max-width: 150px; padding-left: 22px;">
<b>内容类型</b>
</th>
<th style="max-width: 300px; padding-left: 22px;">
<b>说明</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>输入</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>解码器</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>输出</b>
</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<b>带日期的简单 JSON</b>
</td>
<td>
<b>JSON</b>
</td>
<td>
<b>解析特定 JSON 格式中字符串形式的时间戳</b>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-json/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="payload"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-json/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码函数"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-json/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码输出"></span>
</td>
</tr>
<tr>
<td>
<b>简单 CSV</b>
</td>
<td>
<b>TEXT</b>
</td>
<td>
<b>解析 CSV 数据</b>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-csv/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="payload"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-csv/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码函数"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-csv/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码输出"></span>
</td>
</tr>
<tr>
<td>
<b>简单二进制数据</b>
</td>
<td>
<b>BINARY</b>
</td>
<td>
<b>解析包含设备序列号、电量、温度和饱和度的二进制 payload</b>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-binary/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="payload"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-binary/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码函数"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-binary/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码输出"></span>
</td>
</tr>
<tr>
<td>
<b>包含多个十六进制编码值的 JSON</b>
</td>
<td>
<b>JSON</b>
</td>
<td>
<b>转换多个带十六进制"value"字段和时间戳的 JSON 对象</b>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/complex-json-hex/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="payload"></span><br><br>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/complex-json-hex/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码函数"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/complex-json-hex/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码输出"></span>
</td>
</tr>
<tr>
<td>
<b>使用元数据字段</b>
</td>
<td>
<b>JSON</b>
</td>
<td>
<b>使用元数据字段确定设备类型、型号和客户名称</b>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-metadata/payload" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="payload"></span><br><br>
<span tb-help-popup="converter/tbel/examples/decoder/simple-metadata/metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="metadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-metadata/decoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码函数"></span>
</td>
<td>
<span tb-help-popup="converter/tbel/examples/decoder/simple-metadata/output" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 75px;" trigger-text="解码输出"></span>
</td>
</tr>
</tbody>
</table>
