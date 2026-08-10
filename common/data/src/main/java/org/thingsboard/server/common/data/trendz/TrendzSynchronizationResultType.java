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

package org.thingsboard.server.common.data.trendz;

/** Outcome of a Trendz handshake, shown verbatim to the administrator. */
public enum TrendzSynchronizationResultType {

    SYNC_NOT_INITIALIZED("Trendz synchronization is not initialized."),
    SYNC_COMPLETED("Synchronization completed successfully."),
    SYNC_DISABLED("Synchronization is disabled by Trendz configuration."),
    TRENDZ_UNSUPPORTED_VERSION("Trendz version is not supported."),
    TRENDZ_AUTH_INVALID("Trendz authentication failed. Invalid or missing Trendz API key."),
    TRENDZ_URL_UNREACHABLE("Provided Trendz URL is not reachable."),
    TB_URL_MISMATCH("Provided ThingsBoard URL does not match the one stored in ThingsBoard."),
    TB_URL_UNREACHABLE("ThingsBoard URL is not reachable."),
    TB_AUTH_INVALID("ThingsBoard authentication failed. Invalid API key."),
    SYNC_INTERNAL_ERROR("Unexpected internal synchronization error.");

    private final String message;

    TrendzSynchronizationResultType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
