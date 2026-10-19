#### 解码器函数示例

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

return result;
{:copy-code}
```

<br>
<br>
