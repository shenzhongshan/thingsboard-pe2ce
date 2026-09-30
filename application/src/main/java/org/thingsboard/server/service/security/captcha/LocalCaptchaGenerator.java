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
package org.thingsboard.server.service.security.captcha;

/**
 * Renders one self-hosted image captcha challenge. The concrete library backing this (Kaptcha,
 * Hutool, ...) is a system-wide deployment choice (see {@code security.captcha.local.provider} in
 * thingsboard.yml) selected via a pair of {@code @ConditionalOnProperty}-gated beans sharing this
 * bean name - mirroring how {@code cache.type}/{@code queue.type} select their implementations
 * elsewhere in this codebase. Callers (LocalCaptchaService, and transitively the self-registration
 * controllers) depend only on this interface, never on the concrete library.
 */
public interface LocalCaptchaGenerator {

    LocalCaptchaImage generate();

}
