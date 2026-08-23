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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CUSTOM integration type: PE dynamically loads and reflectively instantiates ANY class name a
 * tenant supplies in the integration configuration (executed only in PE's separate remote
 * integration-executor process). Running arbitrary tenant-supplied class names in-process here
 * would be a remote-code-execution hole, so this port restricts CUSTOM to a fixed, server-operator
 * controlled allow-list (thingsboard.yml: integration.custom.allowed-classes, comma-separated FQCNs)
 * — a tenant can only select from classes the deployment operator has explicitly enabled, never an
 * arbitrary string. Each allow-listed class must implement IntegrationRuntime and expose a
 * (Integration, IntegrationUplinkSink) constructor.
 */
@Slf4j
public class CustomIntegrationRuntimeFactory implements IntegrationRuntimeFactory {

    private final Set<String> allowedClasses;

    /**
     * @param allowedClassesProp comma-separated FQCNs; the host reads it from its own configuration
     *                           (platform: thingsboard.yml integration.custom.allowed-classes).
     */
    public CustomIntegrationRuntimeFactory(String allowedClassesProp) {
        this.allowedClasses = Arrays.stream(allowedClassesProp.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    @Override
    public Set<IntegrationType> getSupportedTypes() {
        return Set.of(IntegrationType.CUSTOM);
    }

    @Override
    public IntegrationRuntime create(Integration integration, IntegrationUplinkSink uplinkSink) {
        JsonNode cfg = integration.getConfiguration();
        String clazz = cfg != null ? cfg.path("clazz").asText(null) : null;
        if (clazz == null || clazz.isEmpty()) {
            throw new IllegalArgumentException("Custom integration requires a 'clazz' configuration field!");
        }
        if (!allowedClasses.contains(clazz)) {
            throw new SecurityException("Class [" + clazz + "] is not in the server's custom-integration allow-list "
                    + "(integration.custom.allowed-classes)!");
        }
        try {
            Class<?> runtimeClass = Class.forName(clazz);
            if (!IntegrationRuntime.class.isAssignableFrom(runtimeClass)) {
                throw new IllegalArgumentException("Class [" + clazz + "] does not implement IntegrationRuntime!");
            }
            return (IntegrationRuntime) runtimeClass
                    .getDeclaredConstructor(Integration.class, IntegrationUplinkSink.class)
                    .newInstance(integration, uplinkSink);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate custom integration class [" + clazz + "]: " + e.getMessage(), e);
        }
    }

}
