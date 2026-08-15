package org.rugdar.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class IdServiceTest {

    private final IdService ids = new IdService(7);

    @Test
    void generatesUniqueIds() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertThat(seen.add(ids.next())).isTrue();
        }
        assertThat(seen).hasSize(10_000);
    }

    @Test
    void encodesVersionVariantAndNode() {
        UUID id = ids.next();
        long version = (id.getMostSignificantBits() >>> 12) & 0xF;
        long variant = (id.getLeastSignificantBits() >>> 62) & 0b11;
        assertThat(version).isEqualTo(8);
        assertThat(variant).isEqualTo(0b10);
        assertThat(ids.nodeOf(id)).isEqualTo(7);
    }

    @Test
    void decodesTimestamp() {
        long before = Instant.now().toEpochMilli();
        UUID id = ids.next();
        long after = Instant.now().toEpochMilli();
        assertThat(ids.timestampOf(id)).isBetween(before, after);
    }

    @Test
    void sequenceIncrementsWithinSameMillisecond() {
        UUID prev = ids.next();
        for (int i = 0; i < 1000; i++) {
            UUID current = ids.next();
            if (ids.timestampOf(current) == ids.timestampOf(prev)) {
                assertThat(ids.sequenceOf(current)).isGreaterThan(ids.sequenceOf(prev));
            }
            prev = current;
        }
    }
}
