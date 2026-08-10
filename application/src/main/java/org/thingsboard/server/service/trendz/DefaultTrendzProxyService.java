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
package org.thingsboard.server.service.trendz;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Map;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTrendzProxyService implements TrendzProxyService {

    private final TrendzClient trendzClient;

    @Override
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, byte[] body) throws ThingsboardException {
        Map<String, String[]> parameterMap = request.getParameterMap();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders headers = new HttpHeaders();
        request.getHeaderNames().asIterator()
                .forEachRemaining(name -> request.getHeaders(name).asIterator()
                        .forEachRemaining(value -> headers.add(name, value)));
        return trendzClient.sendTrendzProxyRequest(request.getRequestURI(), parameterMap, method, body, headers);
    }

}
