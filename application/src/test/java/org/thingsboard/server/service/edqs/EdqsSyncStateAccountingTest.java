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
package org.thingsboard.server.service.edqs;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsState.EdqsSyncStatus;
import org.thingsboard.server.common.data.edqs.EdqsSyncRequest;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the resident-type accounting that decides whether a DB whose sync already finished still
 * needs a top-up for entity types added to {@link ObjectType#edqsTenantTypes} afterwards.
 */
class EdqsSyncStateAccountingTest {

    @Test
    void givenFullSyncRequest_whenGetCoveredResidentTypes_thenCoversEveryResidentType() {
        assertThat(DefaultEdqsService.getCoveredResidentTypes(new EdqsSyncRequest()))
                .isEqualTo(ObjectType.edqsTenantTypes);
    }

    @Test
    void givenFullSyncRequest_whenGetCoveredResidentTypes_thenDoesNotLeakTheMutableStaticSet() {
        Set<ObjectType> covered = DefaultEdqsService.getCoveredResidentTypes(new EdqsSyncRequest());
        covered.clear();
        assertThat(ObjectType.edqsTenantTypes).isNotEmpty();
    }

    @Test
    void givenStructuralTypeRequested_whenGetCoveredResidentTypes_thenItIsNotRecorded() {
        // ATTRIBUTE_KV is syncable through an enumerated request but is not a resident *entity* type,
        // so it is outside the accounting basis. Recording it would pollute the synced set with a key
        // that no future diff can ever ask about.
        Set<ObjectType> covered = DefaultEdqsService.getCoveredResidentTypes(
                new EdqsSyncRequest(Set.of(ObjectType.REPORT_TEMPLATE, ObjectType.ATTRIBUTE_KV)));

        assertThat(ObjectType.edqsTenantTypes).doesNotContain(ObjectType.ATTRIBUTE_KV);
        assertThat(covered).containsExactly(ObjectType.REPORT_TEMPLATE);
    }

    @Test
    void givenEveryResidentTypeSynced_whenGetUnsyncedResidentTypes_thenNothingIsMissing() {
        EdqsSyncState state = new EdqsSyncState(EdqsSyncStatus.FINISHED, null, ObjectType.edqsTenantTypes);

        assertThat(DefaultEdqsService.getUnsyncedResidentTypes(state)).isEmpty();
    }

    @Test
    void givenOneResidentTypeMissing_whenGetUnsyncedResidentTypes_thenOnlyThatTypeIsReturned() {
        // negative control: drop a single type from the accounting and it must be the only one reported
        EnumSet<ObjectType> synced = EnumSet.copyOf(ObjectType.edqsTenantTypes);
        synced.remove(ObjectType.ROLE);
        EdqsSyncState state = new EdqsSyncState(EdqsSyncStatus.FINISHED, null, synced);

        assertThat(DefaultEdqsService.getUnsyncedResidentTypes(state)).containsExactly(ObjectType.ROLE);
    }

    @Test
    void givenTwoResidentTypesMissing_whenGetUnsyncedResidentTypes_thenBothAreReturned() {
        // second fixture so a "returns some single type" bug cannot pass
        EnumSet<ObjectType> synced = EnumSet.copyOf(ObjectType.edqsTenantTypes);
        synced.removeAll(Set.of(ObjectType.SCHEDULER_EVENT, ObjectType.ENTITY_GROUP));
        EdqsSyncState state = new EdqsSyncState(EdqsSyncStatus.FINISHED, null, synced);

        assertThat(DefaultEdqsService.getUnsyncedResidentTypes(state))
                .containsExactlyInAnyOrder(ObjectType.SCHEDULER_EVENT, ObjectType.ENTITY_GROUP);
    }

    @Test
    void givenStateWrittenBeforeAccountingExisted_whenDeserialized_thenSyncedTypesIsNullNotEmpty() {
        // the startup branch distinguishes "covered set unknown" (null -> full re-sync) from
        // "nothing covered" (empty -> enumerated sync), so this must not collapse to an empty set
        EdqsSyncState legacy = JacksonUtil.fromString("{\"status\":\"FINISHED\",\"objectTypes\":null}", EdqsSyncState.class);

        assertThat(legacy).isNotNull();
        assertThat(legacy.getStatus()).isEqualTo(EdqsSyncStatus.FINISHED);
        assertThat(legacy.getSyncedTypes()).isNull();
    }

    @Test
    void givenNullOrEmptyCoveredSet_whenIsCoveredSetUnknown_thenTrue() {
        // null predates the accounting; empty is unreachable from a correct run, so it means the same
        // thing. Both must route to a full re-sync, never to an enumerated entity-only sync.
        assertThat(DefaultEdqsService.isCoveredSetUnknown(null)).isTrue();
        assertThat(DefaultEdqsService.isCoveredSetUnknown(Set.of())).isTrue();
    }

    @Test
    void givenAnyCoveredType_whenIsCoveredSetUnknown_thenFalse() {
        assertThat(DefaultEdqsService.isCoveredSetUnknown(Set.of(ObjectType.DEVICE))).isFalse();
        assertThat(DefaultEdqsService.isCoveredSetUnknown(ObjectType.edqsTenantTypes)).isFalse();
    }

    @Test
    void givenStructuralOnlySync_whenComputingWhatToRecord_thenNothingIsLearned() {
        // Regression: a resumed ATTRIBUTE_KV request covers no resident entity type. Unioning that
        // empty result into a null accounting produced an empty set, which silently downgraded
        // "covered set unknown" to "covered set empty" and disarmed the full-re-sync fallback.
        Set<ObjectType> covered = DefaultEdqsService.getCoveredResidentTypes(
                new EdqsSyncRequest(Set.of(ObjectType.ATTRIBUTE_KV)));

        assertThat(covered).isEmpty();
        assertThat(DefaultEdqsService.isCoveredSetUnknown(covered)).isTrue();
    }

    @Test
    void givenSyncedTypes_whenRoundTrippedThroughJson_thenPreserved() {
        EdqsSyncState state = new EdqsSyncState(EdqsSyncStatus.FINISHED, null,
                Set.of(ObjectType.DEVICE, ObjectType.ROLE));

        EdqsSyncState restored = JacksonUtil.fromString(JacksonUtil.toString(state), EdqsSyncState.class);

        assertThat(restored).isNotNull();
        assertThat(restored.getSyncedTypes()).containsExactlyInAnyOrder(ObjectType.DEVICE, ObjectType.ROLE);
    }
}
