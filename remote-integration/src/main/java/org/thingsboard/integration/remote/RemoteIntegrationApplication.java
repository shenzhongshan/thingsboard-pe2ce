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
package org.thingsboard.integration.remote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the remote integration process.
 * <p>
 * Runs one ThingsBoard integration outside the platform, connecting back over gRPC with the
 * integration's routing key and secret. See {@code README.md} in this module for how to start it.
 */
@SpringBootApplication
public class RemoteIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemoteIntegrationApplication.class, args);
    }

}
