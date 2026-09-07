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
package org.thingsboard.server.coapserver;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public interface CoapServerService {

    CoapServer getCoapServer() throws UnknownHostException;

    /**
     * Walks/creates the given resource path under the server root and returns the deepest node, so a
     * caller can attach its own resource there. Used by the CoAP integration to publish itself at
     * PE's shared-server path instead of opening a port of its own.
     */
    Resource addResourceHierarchicallyAndReturnLast(List<String> resourceHierarchy) throws UnknownHostException;

    /** False when no DTLS settings are configured, in which case coaps:// endpoints cannot be served. */
    boolean isDtlsEnabled();

    ConcurrentMap<TbCoapDtlsSessionKey, TbCoapDtlsSessionInfo> getDtlsSessionsMap();
}
