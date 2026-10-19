#### 解码器函数示例

```javascript
{:code-style="max-height: 500px;"}
// 使用前 4 个字节作为设备名称
var deviceName = "SN-" + parseBytesToInt(payload, 0, 4);

var result = {
  deviceName: deviceName,
  deviceType: "Thermometer",
  telemetry: {
    // 使用第 5 个字节作为电量
    battery: parseBytesToInt(payload, 4, 1),
    // 使用第 6、7 字节作为温度
    temperature: parseBytesToInt(payload, 5, 2) / 100.0,
    // 使用第 8 个字节作为饱和度
    saturation: parseBytesToInt(payload, 7, 1)
  }
};

return result;
{:copy-code}
```

<br>
<br>
