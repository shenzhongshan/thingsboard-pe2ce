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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Gate checks in {@link DefaultDatabaseSchemaSettingsService#validateSchemaSettings()}. The
 * same-family entry ("4.4.0") in the supported-versions map makes an offline upgrade between two
 * patch releases possible, which in turn is what makes the downgrade check load-bearing.
 */
class DatabaseSchemaSettingsValidationTest {

    private static DefaultDatabaseSchemaSettingsService serviceWith(String dbVersion, String packageVersion, String dbProduct) {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(pack(dbVersion)));
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of(dbProduct));

        ProjectInfo projectInfo = mock(ProjectInfo.class);
        when(projectInfo.getProjectVersion()).thenReturn(packageVersion);
        when(projectInfo.getProductType()).thenReturn("CE");

        return new DefaultDatabaseSchemaSettingsService(projectInfo, jdbcTemplate);
    }

    private static long pack(String version) {
        String[] p = version.split("\\.");
        return Long.parseLong(p[0]) * 1_000_000_000L + Long.parseLong(p[1]) * 1_000_000L
                + Long.parseLong(p[2]) * 1000L + Long.parseLong(p[3]);
    }

    @Test
    void givenNewerPatchInSameFamily_whenValidating_thenUpgradeIsAllowed() {
        // the reason "4.4.0" is whitelisted: a release touching @RuleNode(version) or needing a Java
        // data migration must be installable offline, and SystemPatchApplier can do neither
        assertThatCode(() -> serviceWith("4.4.0.101", "4.4.0.102", "CE").validateSchemaSettings())
                .doesNotThrowAnyException();
    }

    @Test
    void givenPreviousFamily_whenValidating_thenUpgradeIsAllowed() {
        assertThatCode(() -> serviceWith("4.3.1.0", "4.4.0.0", "CE").validateSchemaSettings())
                .doesNotThrowAnyException();
    }

    @Test
    void givenDbNewerThanPackageInSameFamily_whenValidating_thenDowngradeIsRejected() {
        // without this check the whitelist entry above would let a downgrade run the migrations
        // backwards: product matches, versions differ, and "4.4.0" is an accepted source prefix
        assertThatThrownBy(() -> serviceWith("4.4.0.105", "4.4.0.102", "CE").validateSchemaSettings())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Downgrade is not supported");
    }

    @Test
    void givenDbNewerThanPackageAcrossFamilies_whenValidating_thenDowngradeIsRejectedBeforeTheWhitelist() {
        // 4.5.0 is not a whitelisted source either, but the downgrade message is the accurate one
        assertThatThrownBy(() -> serviceWith("4.5.0.0", "4.4.0.0", "CE").validateSchemaSettings())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Downgrade is not supported");
    }

    @Test
    void givenSameVersion_whenValidating_thenAlreadyUpgradedIsRejected() {
        assertThatThrownBy(() -> serviceWith("4.4.0.102", "4.4.0.102", "CE").validateSchemaSettings())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already upgraded");
    }

    @Test
    void givenUnsupportedOlderVersion_whenValidating_thenUpgradeIsRejected() {
        assertThatThrownBy(() -> serviceWith("4.2.2.0", "4.4.0.0", "CE").validateSchemaSettings())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("is not supported for upgrade");
    }

    @Test
    void givenForeignProduct_whenValidating_thenUpgradeIsRejected() {
        assertThatThrownBy(() -> serviceWith("4.3.1.0", "4.4.0.0", "PE").validateSchemaSettings())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("can't upgrade ThingsBoard PE database");
    }

    @Test
    void givenLegacyThreePartDbVersion_whenReadingIt_thenItNormalisesToFourParts() {
        // 4002001 is the pre-4.2.1.1 packing; the downgrade comparison has to agree with it
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(4_002_001L));
        ProjectInfo projectInfo = mock(ProjectInfo.class);
        when(projectInfo.getProjectVersion()).thenReturn("4.4.0.0");

        assertThat(new DefaultDatabaseSchemaSettingsService(projectInfo, jdbcTemplate).getDbSchemaVersion())
                .isEqualTo("4.2.1.0");
    }
}
