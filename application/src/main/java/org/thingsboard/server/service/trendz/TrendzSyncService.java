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

import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzIntegrationSettings;

/** Owns the handshake that makes ThingsBoard and Trendz aware of each other. */
public interface TrendzSyncService {

    String TRENDZ_API_KEY_DESCRIPTION = "Internal API key used to authenticate with Trendz";

    /** Runs the full handshake and records the outcome in settings, whether it succeeded or not. */
    TrendzIntegrationSettings performSync();

    /** Syncs only when the previous attempt never reached Trendz. */
    void performSyncIfNeeded();

    TrendzHealthcheckResult performHealthcheck();

    /** Re-hands the key to Trendz after a rotation, passing the previous one so it can overlap. */
    void performApiKeyRotationSync(ApiKey newApiKey, ApiKey oldApiKey);

}
