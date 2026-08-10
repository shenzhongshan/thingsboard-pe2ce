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
package org.thingsboard.server.service.solutions.data.emulator;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Resolves the {@code clazz} a template names to an emulator instance.
 * <p>
 * This is an explicit registry rather than {@code Class.forName}: the class name comes from a data
 * file, and a lookup table keeps that from being a way to instantiate arbitrary classes. Templates
 * still spell the name out in full, so the keys are the fully qualified names PE used.
 */
public class EmulatorFactory {

    private static final Map<String, Supplier<Emulator>> CUSTOM_EMULATORS = Map.of(
            FieldIrrigationStateEmulator.class.getName(), FieldIrrigationStateEmulator::new
    );

    private EmulatorFactory() {
    }

    public static Emulator create(EmulatorDefinition definition) {
        String clazz = definition.getClazz();
        Emulator emulator;
        if (StringUtils.isEmpty(clazz)) {
            emulator = new BasicEmulator();
        } else {
            Supplier<Emulator> supplier = CUSTOM_EMULATORS.get(clazz);
            if (supplier == null) {
                throw new IllegalArgumentException("Unknown solution emulator class: " + clazz);
            }
            emulator = supplier.get();
        }
        emulator.init(definition);
        return emulator;
    }

}
