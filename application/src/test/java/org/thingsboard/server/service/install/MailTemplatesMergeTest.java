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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.install.InstallScripts.MailTemplatesMerge;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The three merge rules for the system MAIL_TEMPLATES refresh that runs on every upgrade:
 * stored bodies win, keys new in the shipped file are seeded, keys dropped from it are pruned.
 */
class MailTemplatesMergeTest {

    private static JsonNode json(String raw) {
        return JacksonUtil.toJsonNode(raw);
    }

    @Test
    void givenKeyInBothSides_whenMerging_thenTheStoredBodyIsKept() {
        // the operator's customisation must survive the upgrade
        JsonNode stored = json("{\"activation\":{\"subject\":\"MINE\"}}");
        JsonNode shipped = json("{\"activation\":{\"subject\":\"SHIPPED\"}}");

        MailTemplatesMerge merge = InstallScripts.mergeMailTemplates(stored, shipped);

        assertThat(merge.merged().get("activation").get("subject").asText()).isEqualTo("MINE");
        assertThat(merge.kept()).isEqualTo(1);
        assertThat(merge.seeded()).isZero();
        assertThat(merge.pruned()).isZero();
    }

    @Test
    void givenKeyOnlyInShippedFile_whenMerging_thenItIsSeeded() {
        // this is the whole point of the refresh: a template added in a release reaches existing DBs
        JsonNode stored = json("{\"activation\":{\"subject\":\"MINE\"}}");
        JsonNode shipped = json("{\"activation\":{\"subject\":\"SHIPPED\"},\"twoFaVerification\":{\"subject\":\"NEW\"}}");

        MailTemplatesMerge merge = InstallScripts.mergeMailTemplates(stored, shipped);

        assertThat(merge.merged().get("twoFaVerification").get("subject").asText()).isEqualTo("NEW");
        assertThat(merge.merged().get("activation").get("subject").asText()).isEqualTo("MINE");
        assertThat(merge.kept()).isEqualTo(1);
        assertThat(merge.seeded()).isEqualTo(1);
        assertThat(merge.pruned()).isZero();
    }

    @Test
    void givenKeyOnlyInStoredTemplates_whenMerging_thenItIsPruned() {
        JsonNode stored = json("{\"activation\":{\"subject\":\"MINE\"},\"retired\":{\"subject\":\"OLD\"}}");
        JsonNode shipped = json("{\"activation\":{\"subject\":\"SHIPPED\"}}");

        MailTemplatesMerge merge = InstallScripts.mergeMailTemplates(stored, shipped);

        assertThat(merge.merged().has("retired")).isFalse();
        assertThat(merge.merged().fieldNames()).toIterable().containsExactly("activation");
        assertThat(merge.kept()).isEqualTo(1);
        assertThat(merge.seeded()).isZero();
        assertThat(merge.pruned()).isEqualTo(1);
    }

    @Test
    void givenAllThreeCasesAtOnce_whenMerging_thenEachKeyTakesItsOwnBranch() {
        // one fixture per rule cannot catch "applied the right rule to the wrong key"
        JsonNode stored = json("""
                {"activation":{"subject":"MINE"},
                 "resetPassword":{"subject":"ALSO MINE"},
                 "retired":{"subject":"OLD"}}""");
        JsonNode shipped = json("""
                {"activation":{"subject":"SHIPPED"},
                 "resetPassword":{"subject":"SHIPPED TOO"},
                 "test":{"subject":"BRAND NEW"}}""");

        MailTemplatesMerge merge = InstallScripts.mergeMailTemplates(stored, shipped);

        assertThat(merge.merged().get("activation").get("subject").asText()).isEqualTo("MINE");
        assertThat(merge.merged().get("resetPassword").get("subject").asText()).isEqualTo("ALSO MINE");
        assertThat(merge.merged().get("test").get("subject").asText()).isEqualTo("BRAND NEW");
        assertThat(merge.merged().has("retired")).isFalse();
        assertThat(merge.kept()).isEqualTo(2);
        assertThat(merge.seeded()).isEqualTo(1);
        assertThat(merge.pruned()).isEqualTo(1);
    }

    @Test
    void givenIdenticalSides_whenMerging_thenNothingIsReportedAsChanged() {
        // negative control: a no-op upgrade must not claim it seeded or pruned anything
        JsonNode stored = json("{\"activation\":{\"subject\":\"A\"},\"test\":{\"subject\":\"B\"}}");

        MailTemplatesMerge merge = InstallScripts.mergeMailTemplates(stored, stored.deepCopy());

        assertThat(merge.kept()).isEqualTo(2);
        assertThat(merge.seeded()).isZero();
        assertThat(merge.pruned()).isZero();
        assertThat(merge.merged()).isEqualTo(stored);
    }

    @Test
    void givenShippedFileDroppedEverything_whenMerging_thenAllStoredKeysArePruned() {
        JsonNode stored = json("{\"activation\":{\"subject\":\"A\"},\"test\":{\"subject\":\"B\"}}");

        MailTemplatesMerge merge = InstallScripts.mergeMailTemplates(stored, JacksonUtil.newObjectNode());

        assertThat(merge.merged()).isEmpty();
        assertThat(merge.kept()).isZero();
        assertThat(merge.pruned()).isEqualTo(2);
    }
}
