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

import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Set;

/**
 * Builds an IntegrationRuntime for one or more IntegrationType constants. Implementations are
 * Spring beans, collected by DefaultPlatformIntegrationService into a type-&gt;factory registry.
 * One factory may serve several related types (e.g. all MQTT-family variants share one factory
 * backed by MqttIntegrationClient, since the differences are config-shape only).
 */
public interface IntegrationRuntimeFactory {

    Set<IntegrationType> getSupportedTypes();

    IntegrationRuntime create(Integration integration, PlatformIntegrationService platformIntegrationService);

}
