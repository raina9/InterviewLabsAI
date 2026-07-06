package com.interviewlab.sessionstore;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySessionStoreTest {

    private final InMemorySessionStore store = new InMemorySessionStore();

    // -------------------------------------------------------------------------
    // Scenario 1: put then get — value round-trips
    // -------------------------------------------------------------------------

    @Test
    void putThenGet_returnsStoredValue() {
        store.put("k1", "hello", 1);

        assertThat(store.get("k1", String.class)).isEqualTo("hello");
    }

    // -------------------------------------------------------------------------
    // Scenario 2: get on missing key — returns null
    // -------------------------------------------------------------------------

    @Test
    void get_missingKey_returnsNull() {
        assertThat(store.get("missing", String.class)).isNull();
    }

    // -------------------------------------------------------------------------
    // Scenario 3: delete removes the key
    // -------------------------------------------------------------------------

    @Test
    void delete_removesKey() {
        store.put("k2", "value", 1);

        store.delete("k2");

        assertThat(store.get("k2", String.class)).isNull();
    }

    // -------------------------------------------------------------------------
    // Scenario 4: negative TTL (already-expired entry) — get returns null
    // -------------------------------------------------------------------------

    @Test
    void get_expiredEntry_returnsNull() {
        store.put("k3", "value", -1); // expiresAt is in the past on arrival

        assertThat(store.get("k3", String.class)).isNull();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: increment — starts at 1, increments atomically
    // -------------------------------------------------------------------------

    @Test
    void increment_newKey_startsAtOne() {
        long count = store.increment("counter", 1);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void increment_existingKey_incrementsSequentially() {
        store.increment("counter", 1);
        store.increment("counter", 1);
        long third = store.increment("counter", 1);

        assertThat(third).isEqualTo(3L);
    }

    // -------------------------------------------------------------------------
    // Scenario 6: increment on an already-expired window — resets to 1
    // -------------------------------------------------------------------------

    @Test
    void increment_expiredWindow_resetsToOne() {
        store.increment("counter", -1); // window already expired on arrival

        long afterExpiry = store.increment("counter", 1);

        assertThat(afterExpiry).isEqualTo(1L);
    }
}
