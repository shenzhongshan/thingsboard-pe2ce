#### 下行数据转换器编码函数

<div class="divider"></div>
<br/>

*function Encoder(msg, metadata, msgType, integrationMetadata): {msg: object, metadata: object, msgType: string}*

用于将规则引擎的入站消息及其元数据转换为对应集成所需格式的 JavaScript 函数。

**参数：**

<ul>
  <li><b>msg：</b> <code>{[key: string]: any}</code> - 规则引擎消息的 JSON。
  </li>
  <li><b>metadata：</b> <code>{[key: string]: string}</code> - 一组关于该消息的附加数据键值对（由规则引擎生成）。
  </li>
  <li><b>msgType：</b> <code>{[key: string]: string}</code> - 规则引擎消息类型。
        更多详情参见 <a href="${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/overview/#predefined-message-types" target="_blank">预定义消息类型</a>。
  </li>
  <li><b>integrationMetadata：</b> <code>{[key: string]: string}</code> - 包含部分集成特有字段的键值对映射。<br>
        你可以在集成详情中为每个集成配置额外的元数据。
  </li>
</ul>


**返回值：**

应返回一个符合以下结构的合法 JSON 文档：

<br>

<div style="padding-left: 64px;"
     tb-help-popup="converter/examples/encoder/json_output"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="JSON 输出示例">
</div>

<div class="divider"></div>

##### 示例

###### 编码 payload 以便进一步发送到外部 MQTT broker

假设有这样一个场景：温度和湿度的上报频率属性通过平台 REST API 被更新，<br>
你希望将这次更新推送到外部 MQTT broker（TTN、Mosquitto、AWS IoT 等）。<br>
你可能还希望附带很久之前配置的 "firmwareVersion" 属性值，而该值并未出现<br>
在这次具体请求中。推送更新所用的主题（topic）中应包含设备名称。

<table style="max-width: 500px;">
<thead>
<tr>
<th style="max-width: 300px; padding-left: 22px;">
<b>输入参数</b>
</th>
<th style="max-width: 200px; padding-left: 22px;">
<b>编码函数</b>
</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<span tb-help-popup="converter/examples/encoder/example1/message" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="msg"></span><br><br>
<span tb-help-popup="converter/examples/encoder/example1/metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="metadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span><br><br>
<span tb-help-popup-content="**ATTRIBUTES_UPDATED**" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="msgType"></span><br><br>
<span tb-help-popup="converter/examples/encoder/example1/integration_metadata" tb-help-popup-placement="top" trigger-style="font-size: 16px;" trigger-text="integrationMetadata" [tb-help-popup-style]="{maxWidth: '600px'}"></span>
</td>
<td>
<span tb-help-popup="converter/examples/encoder/example1/encoder_fn" tb-help-popup-placement="top" trigger-style="font-size: 16px; line-height: 120px;" trigger-text="编码函数"></span>
</td>
</tr>
</tbody>
</table>

<br>
<br>
