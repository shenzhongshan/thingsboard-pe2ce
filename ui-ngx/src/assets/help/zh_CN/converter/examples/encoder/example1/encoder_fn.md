#### 编码器 JavaScript 函数示例

```javascript
{:code-style="max-height: 500px;"}

// 将入站规则引擎消息编码为下行数据

// msg - 下行消息的 JSON 消息 payload
// msgType - 消息类型，例如 'ATTRIBUTES_UPDATED'、'POST_TELEMETRY_REQUEST' 等
// metadata - 关于该消息附加数据的键值对列表
// integrationMetadata - 执行此转换器的集成中定义的附加数据键值对列表

/** 编码器 **/

var data = {};

// 处理来自入站消息和元数据的数据

data.tempFreq = msg.temperatureUploadFrequency;

data.firmwareVersion = metadata['ss_firmwareVersion'];

// 包含已编码下行 payload 的结果对象
var result = {

  // 下行数据内容类型：JSON、TEXT 或 BINARY（base64 格式）
  contentType: "JSON",

  // 下行数据
  data: JSON.stringify(data),

  // 可选的、以键值对形式呈现的元数据对象
  metadata: {topic: metadata['deviceType'] + '/' + metadata['deviceName'] + '/upload'}

};

return result;
{:copy-code}
```

<br>
<br>
