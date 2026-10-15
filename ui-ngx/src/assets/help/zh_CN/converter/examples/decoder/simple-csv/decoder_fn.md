#### 解码器 JavaScript 函数示例

```javascript
{:code-style="max-height: 500px;"}

// 将 payload 解码为字符串，参见下方辅助函数
var str = decodeToString(payload);
// 使用 ',' 分隔符将字符串拆分为字符串数组
var csv = str.split(',');
// 构造包含时间序列数据的结果对象
var result = {
    deviceName: csv[0],
    deviceType: "Thermostat",
    deviceLabel: "Kitchen Thermostat",
    telemetry: {
        temperature: parseFloat(csv[1]),
        humidity: parseFloat(csv[2]),
    }
};

/** 辅助函数：将原始 payload 字节解码为字符串 **/
function decodeToString(payload) {
    return String.fromCharCode.apply(String, payload);
}

return result;
{:copy-code}
```

<br>
<br>
