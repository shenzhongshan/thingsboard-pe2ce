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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 纯转发，不解析也不缓存响应 —— 许可判定完全由门户和 edge 两端决定，云只做通道。
 * 端点可配置（PE 里是写死的常量），留空即关闭转发。
 */
@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeLicenseService implements EdgeLicenseService {

    @Value("${edges.enabled:false}")
    private boolean edgesEnabled;

    @Value("${edges.license.server_endpoint:}")
    private String licenseServerEndpoint;

    @Value("${edges.license.timeout_ms:30000}")
    private int timeoutMs;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        // 沿用 PE 的懒初始化：edges 关掉时连 RestTemplate 都不建。
        if (edgesEnabled && StringUtils.isNotEmpty(licenseServerEndpoint)) {
            restTemplate = initRestTemplate();
        }
    }

    @Override
    public ResponseEntity<JsonNode> checkInstance(JsonNode request) {
        RestTemplate template = requireTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = template.postForEntity(
                licenseServerEndpoint + "/api/license/checkInstance", new HttpEntity<>(request, headers), JsonNode.class);
        return new ResponseEntity<>(response.getBody(), response.getStatusCode());
    }

    @Override
    public ResponseEntity<JsonNode> activateInstance(String licenseSecret, String releaseDate) {
        RestTemplate template = requireTemplate();
        Map<String, String> params = new HashMap<>();
        params.put("licenseSecret", licenseSecret);
        params.put("releaseDate", releaseDate);
        ResponseEntity<JsonNode> response = template.postForEntity(
                licenseServerEndpoint + "/api/license/activateInstance?licenseSecret={licenseSecret}&releaseDate={releaseDate}",
                null, JsonNode.class, params);
        return new ResponseEntity<>(response.getBody(), response.getStatusCode());
    }

    private RestTemplate requireTemplate() {
        if (restTemplate == null) {
            // 明确报错好过让调用方对着一个 NPE 猜配置。
            throw new IllegalStateException("Edge license forwarding is disabled: set edges.enabled=true and "
                    + "edges.license.server_endpoint to the license portal URL");
        }
        return restTemplate;
    }

    private RestTemplate initRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        String proxyHost = System.getProperty("tb.proxy.host");
        String proxyPort = System.getProperty("tb.proxy.port");
        if (StringUtils.isNotEmpty(proxyHost) && StringUtils.isNotEmpty(proxyPort)) {
            log.info("Edge license requests will go through proxy [{}:{}]", proxyHost, proxyPort);
            factory.setProxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost, Integer.parseInt(proxyPort))));
        }
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return new RestTemplate(factory);
    }

}
