/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Executes converter decoder/encoder scripts (TBEL or JS) via CE's shared script engines.
 * The decoder contract matches PE: function decoder(payload, metadata) where payload is an
 * array of unsigned byte values and metadata is a key/value object; the result is the uplink
 * data JSON ({deviceName, deviceType, attributes, telemetry, ...}).
 * CE simplification: scripts are compiled per invocation (no per-converter compiled-script
 * cache; acceptable for the HTTP/MQTT uplink rates this port targets).
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class ConverterExecutorService {

    private static final long SCRIPT_TIMEOUT_SEC = 10;

    private final JsInvokeService jsInvokeService;
    private final TbelInvokeService tbelInvokeService;

    public JsonNode executeUplink(TenantId tenantId, ScriptLanguage scriptLang, String decoder,
                                  byte[] payload, Map<String, String> metadata) throws Exception {
        List<Integer> payloadArr = new ArrayList<>(payload.length);
        for (byte b : payload) {
            payloadArr.add(b & 0xFF);
        }
        // The JS engine's rule-node wrapper honors argNames only when there are exactly three.
        return invoke(tenantId, scriptLang, decoder, new String[]{"payload", "metadata", "msgType"},
                new Object[]{payloadArr, metadata, ""});
    }

    public JsonNode executeDownlink(TenantId tenantId, ScriptLanguage scriptLang, String encoder,
                                    JsonNode msg, String msgType, Map<String, String> metadata,
                                    Map<String, String> integrationMetadata) throws Exception {
        Object msgArg = JacksonUtil.convertValue(msg, Object.class);
        // Only three script arguments fit the JS rule-node wrapper — integrationMetadata is
        // inlined into the script as a literal instead (values are short strings).
        String script = "var integrationMetadata = " + JacksonUtil.toString(integrationMetadata) + ";\n" + encoder;
        return invoke(tenantId, scriptLang, script,
                new String[]{"msg", "metadata", "msgType"},
                new Object[]{msgArg, metadata, msgType});
    }

    private JsonNode invoke(TenantId tenantId, ScriptLanguage scriptLang, String script,
                            String[] argNames, Object[] args) throws Exception {
        boolean js = ScriptLanguage.JS.equals(scriptLang);
        ScriptInvokeService service = js ? jsInvokeService : tbelInvokeService;
        if (js) {
            // The JS rule-node wrapper expects the first two invoke arguments as JSON strings
            // (it JSON.parse-s them before calling the inner function); the third stays plain.
            args = new Object[]{JacksonUtil.toString(args[0]), JacksonUtil.toString(args[1]), args[2]};
        }
        UUID scriptId = service.eval(tenantId, ScriptType.RULE_NODE_SCRIPT, script, argNames)
                .get(SCRIPT_TIMEOUT_SEC, TimeUnit.SECONDS);
        try {
            Object result = service.invokeScript(tenantId, null, scriptId, args)
                    .get(SCRIPT_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (result instanceof String str) {
                return JacksonUtil.toJsonNode(str);
            }
            return JacksonUtil.valueToTree(result);
        } finally {
            service.release(scriptId);
        }
    }

}
