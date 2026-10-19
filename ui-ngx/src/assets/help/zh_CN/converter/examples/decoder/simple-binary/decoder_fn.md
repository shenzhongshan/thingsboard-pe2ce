#### 解码器 JavaScript 函数示例

```javascript
{:code-style="max-height: 500px;"}
// 使用前 4 个字节作为设备名称
var deviceName = "SN-" + getInt(payload, 0, 4);

var result = {
    deviceName: deviceName,
    deviceType: "Thermometer",
    telemetry: {
        // 使用第 5 个字节作为电量
        battery: getInt(payload, 4, 5),
        // 使用第 6、7 字节作为温度
        temperature: getFloat(payload, 5, 7, 100),
        // 使用第 8 个字节作为饱和度
        saturation: getInt(payload, 7, 8)
    }
};

/* 根据 'byteIdxFrom' 和 'byteIdxTo' 从 'byteArray' 中提取整数值 */
function getInt(byteArray, byteIdxFrom, byteIdxTo) {
    var val = 0;
    for (var j = byteIdxFrom; j < byteIdxTo; j++) {
        val += byteArray[j];
        if (j < byteIdxTo - 1) {
            val = val << 8;
        }
    }
    return val;
}

/* 根据 'byteIdxFrom' 和 'byteIdxTo' 从 'byteArray' 中提取浮点数值，并用 'divided' 参数做除法 */
function getFloat(byteArray, byteIdxFrom, byteIdxTo, divided) {
    return getInt(byteArray, byteIdxFrom, byteIdxTo) / divided;
}


return result;
{:copy-code}
```

<br>
<br>
