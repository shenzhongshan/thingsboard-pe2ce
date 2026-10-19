#### 解码器函数示例

```javascript
{:code-style="max-height: 500px;"}
// 将 payload 解码为 JSON
var data = decodeToJson(payload);

var result = [];

for (int i = 0; i < data.length; i++) {
  var report = data[i];
  var deviceName = report.serialNumber;
  var raw = report.value;
  var decoded = hexToBytes(raw);
  // 包含设备属性/遥测数据的结果对象
  result.push({
    deviceName: deviceName,
    deviceType: "Thermometer",
    telemetry: {
      ts: Date.parse(report.timestamp),
      values: {
        batteryVoltage: parseBytesToInt(decoded, 0, 2) / 100.0,
        temperature: parseBytesToInt(decoded, 2, 2) / 100.0,
        rawData: report
      }
    }
  });
}

return result;
{:copy-code}
```

<br>
<br>
