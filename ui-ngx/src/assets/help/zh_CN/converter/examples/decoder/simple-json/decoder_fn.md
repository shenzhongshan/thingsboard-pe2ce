#### 解码器 JavaScript 函数示例

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

/** 辅助函数：将原始 payload 字节解码为字符串 **/
function decodeToString(payload) {
    return String.fromCharCode.apply(String, payload);
}
/** 辅助函数：将原始 payload 字节解码为 JSON 对象 **/
function decodeToJson(payload) {
    return JSON.parse(decodeToString(payload));
}

return result;
{:copy-code}
```

<br>
<br>
