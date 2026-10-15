#### 解码器函数示例

```javascript
{:code-style="max-height: 500px;"}

// 将 payload 解码为 JSON，参见下方辅助函数
var json = decodeToJson(payload);
// 将日期字符串转换为毫秒级时间戳
var timestamp = Date.parse(json.ts);
// 构造包含时间序列数据的结果对象
var result = {
    deviceName: json.serialNumber,
    deviceType: "Thermostat",
    deviceLabel: "Kitchen Thermostat",
    telemetry: {
        ts: timestamp,
        values: {
            temperature: json.t,
            humidity: json.h,
        }
    }
};

return result;
{:copy-code}
```

<br>
<br>
