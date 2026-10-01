#### 解码器 JavaScript 函数示例

```javascript
{:code-style="max-height: 500px;"}
// 将 payload 解码为 JSON
var data = decodeToJson(payload);

var result = [];

for (var i in data) {
    var report = data[i];
    var deviceName = report.serialNumber;
    var raw = report.value;
    var decoded = hexStringToByteArray(raw);
    // 包含设备属性/遥测数据的结果对象
    result.push({
        deviceName: deviceName,
        deviceType: "Thermometer",
        telemetry: {
            ts: Date.parse(report.timestamp),
            values: {
                batteryVoltage: getFloat(decoded, 0, 2, 100),
                temperature: getFloat(decoded, 2, 4, 100),
                rawData: report
            }
        }
    });
}

/** 辅助函数 **/

function decodeToString(payload) {
    return String.fromCharCode.apply(String, payload);
}

function decodeToJson(payload) {
    var str = decodeToString(payload);
    var data = JSON.parse(str);
    return data;
}

/* 将十六进制字符串转换为字节数组 */
function hexStringToByteArray(str) {
    if (!str) {
        return new Uint8Array();
    }
    var a = [];
    for (var i = 0, len = str.length; i < len; i += 2) {
        a.push(parseInt(str.substr(i, 2), 16));
    }
    return new Uint8Array(a);
}

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
