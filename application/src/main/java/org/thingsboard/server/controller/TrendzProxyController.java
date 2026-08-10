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
package org.thingsboard.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.trendz.TrendzProxyService;

/**
 * Serves the Trendz web UI from this origin. Everything is relayed byte-for-byte, so Trendz can sit
 * on a private network with only ThingsBoard exposed.
 */
@RestController
@TbCoreComponent
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class TrendzProxyController extends BaseController {

    private final TrendzProxyService trendzProxyService;

    @ApiOperation(value = "Forward authorized requests to Trendz",
            notes = "Relays /apiTrendz/** to Trendz. Requires Trendz to be synchronized. " +
                    "Available for any authorized user.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping("/apiTrendz/**")
    public ResponseEntity<byte[]> handleAuthorizedTrendzRequests(HttpServletRequest request,
                                                                 @RequestBody(required = false) byte[] body) throws ThingsboardException {
        return trendzProxyService.proxy(request, body);
    }

    @ApiOperation(value = "Forward unauthorized requests to Trendz",
            notes = "Relays the Trendz public API and static assets, which have to load before anyone has signed in.")
    @RequestMapping({"/apiTrendz/publicApi/**", "/trendz/**"})
    public ResponseEntity<byte[]> handleUnauthorizedTrendzRequests(HttpServletRequest request,
                                                                   @RequestBody(required = false) byte[] body) throws ThingsboardException {
        return trendzProxyService.proxy(request, body);
    }

}
