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
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins how the Maven project version becomes a schema version. Release numbering depends on it, and
 * {@link DatabaseSchemaSettingsValidationTest} cannot cover it: that suite mocks {@link ProjectInfo}
 * and therefore feeds already-stripped versions, bypassing the stripping entirely.
 */
class ProjectInfoVersionTest {

    private static ProjectInfo withMavenVersion(String version) {
        Properties props = new Properties();
        props.setProperty("version", version);
        return new ProjectInfo(Optional.of(new BuildProperties(props)));
    }

    @Test
    void givenSnapshotVersion_whenReadingProjectVersion_thenTheQualifierIsStripped() {
        // Consequence for releases: the qualifier carries no version information. Shipping
        // 4.4.0-SNAPSHOT and then 4.4.0 is numerically the same version, so neither upgrade path
        // triggers. The bump has to be in the digits.
        assertThat(withMavenVersion("4.4.0.102-SNAPSHOT").getProjectVersion()).isEqualTo("4.4.0.102");
        assertThat(withMavenVersion("4.4.0-SNAPSHOT").getProjectVersion()).isEqualTo("4.4.0");
        assertThat(withMavenVersion("4.4.0.102").getProjectVersion()).isEqualTo("4.4.0.102");
    }

    @Test
    void givenPeStyleQualifier_whenReadingProjectVersion_thenOnlyDigitsAndDotsSurvive() {
        // PE tags its artifacts 4.3.1.1-PE-SNAPSHOT; the same stripping applies.
        assertThat(withMavenVersion("4.3.1.1-PE-SNAPSHOT").getProjectVersion()).isEqualTo("4.3.1.1");
    }

    @Test
    void givenNoBuildProperties_whenReadingProjectVersion_thenItFailsLoudly() {
        // Running from an IDE without a Maven build produces no BuildProperties. Better to fail
        // than to silently report version 0.0.0.0 and let an upgrade gate decide on garbage.
        assertThatThrownBy(() -> new ProjectInfo(Optional.empty()).getProjectVersion())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Build properties are missing");
    }

    @Test
    void whenReadingProductType_thenItIsCe() {
        // The upgrade gate refuses a database whose product column disagrees, so this value is
        // effectively part of the release contract.
        assertThat(withMavenVersion("4.4.0").getProductType()).isEqualTo("CE");
    }
}
