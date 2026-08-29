package mycraft.yuyears.neofavoriteitems;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigAccessTest {
    @AfterEach
    void restoreDefaults() {
        ServerConfigAccess.configureClient(() -> false, () -> {}, ignored -> {});
        ServerConfigAccess.reset();
    }

    @Test
    void enablesEditingAndSubmissionOnlyAfterAllowedResponse() {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<ServerRulesSnapshot> update = new AtomicReference<>();
        ServerRulesSnapshot rules = ServerRulesSnapshot.from(new NeoFavoriteItemsConfig());
        ServerConfigAccess.configureClient(() -> true, requests::incrementAndGet, update::set);

        ServerConfigAccess.request();
        assertEquals(1, requests.get());
        assertEquals(ServerConfigAccess.State.UNKNOWN, ServerConfigAccess.state());
        assertFalse(ServerConfigAccess.canEdit());
        assertFalse(ServerConfigAccess.submit(rules));

        ServerConfigAccess.receive(true, false, null);
        assertEquals(ServerConfigAccess.State.DENIED, ServerConfigAccess.state());
        assertFalse(ServerConfigAccess.canEdit());
        assertFalse(ServerConfigAccess.submit(rules));

        ServerConfigAccess.receive(true, true, rules);
        assertTrue(ServerConfigAccess.canEdit());
        assertTrue(ServerConfigAccess.submit(rules));
        assertEquals(rules, update.get());
    }

    @Test
    void unavailableServerNeverEnablesEditing() {
        AtomicInteger requests = new AtomicInteger();
        ServerConfigAccess.configureClient(() -> false, requests::incrementAndGet, ignored -> {});

        ServerConfigAccess.request();

        assertEquals(0, requests.get());
        assertEquals(ServerConfigAccess.State.UNAVAILABLE, ServerConfigAccess.state());
        assertFalse(ServerConfigAccess.canEdit());
    }
}
