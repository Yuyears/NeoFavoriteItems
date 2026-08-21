package mycraft.yuyears.neofavoriteitems.application;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerFavoriteServiceTest {
    @Test
    void inventoryGuardBypassIsScopedAndNestable() {
        UUID playerId = UUID.randomUUID();

        assertFalse(ServerFavoriteService.isInventoryGuardBypassed(playerId));

        ServerFavoriteService.beginInventoryGuardBypass(playerId);
        ServerFavoriteService.beginInventoryGuardBypass(playerId);

        assertTrue(ServerFavoriteService.isInventoryGuardBypassed(playerId));

        ServerFavoriteService.endInventoryGuardBypass(playerId);
        assertTrue(ServerFavoriteService.isInventoryGuardBypassed(playerId));

        ServerFavoriteService.endInventoryGuardBypass(playerId);
        assertFalse(ServerFavoriteService.isInventoryGuardBypassed(playerId));
    }

    @Test
    void unmatchedBypassEndDoesNotEnableBypass() {
        UUID playerId = UUID.randomUUID();

        ServerFavoriteService.endInventoryGuardBypass(playerId);

        assertFalse(ServerFavoriteService.isInventoryGuardBypassed(playerId));
    }

    @Test
    void correctionSyncIsLimitedToOncePerPlayerPerTick() {
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();

        assertTrue(ServerFavoriteService.shouldSendCorrectionSync(firstPlayer, 10L));
        assertFalse(ServerFavoriteService.shouldSendCorrectionSync(firstPlayer, 10L));
        assertTrue(ServerFavoriteService.shouldSendCorrectionSync(firstPlayer, 11L));
        assertTrue(ServerFavoriteService.shouldSendCorrectionSync(secondPlayer, 10L));
    }

    @Test
    void deathDropPreservationIsScopedAndNestable() {
        UUID playerId = UUID.randomUUID();

        assertFalse(ServerFavoriteService.isDeathDropPreservationActive(playerId));

        ServerFavoriteService.beginDeathDropPreservation(playerId);
        ServerFavoriteService.beginDeathDropPreservation(playerId);

        assertTrue(ServerFavoriteService.isDeathDropPreservationActive(playerId));

        ServerFavoriteService.endDeathDropPreservation(playerId);
        assertTrue(ServerFavoriteService.isDeathDropPreservationActive(playerId));

        ServerFavoriteService.endDeathDropPreservation(playerId);
        assertFalse(ServerFavoriteService.isDeathDropPreservationActive(playerId));
    }

    @Test
    void respawnRestoreBypassUsesKeepInventory() {
        assertTrue(ServerFavoriteService.shouldBypassRespawnInventoryRestore(
            false,
            false,
            true,
            false
        ));
    }

    @Test
    void respawnRestoreBypassUsesKeepEverythingWhenPresent() {
        assertTrue(ServerFavoriteService.shouldBypassRespawnInventoryRestore(
            false,
            true,
            false,
            false
        ));
    }

    @Test
    void respawnRestoreBypassUsesDeathPreservationConfig() {
        assertTrue(ServerFavoriteService.shouldBypassRespawnInventoryRestore(
            false,
            false,
            false,
            true
        ));
    }

    @Test
    void respawnRestoreBypassStaysOffForNormalDeathDrops() {
        assertFalse(ServerFavoriteService.shouldBypassRespawnInventoryRestore(
            false,
            false,
            false,
            false
        ));
    }

    @Test
    void respawnRestoreBypassStaysOffOnClientSide() {
        assertFalse(ServerFavoriteService.shouldBypassRespawnInventoryRestore(
            true,
            true,
            true,
            true
        ));
    }

    @Test
    void incomingFreeSlotSkipsLockedEmptySlotAndUsesNextAvailableSlot() {
        assertEquals(3, ServerFavoriteService.resolveFreeSlotForIncomingItem(
            1,
            5,
            slot -> slot == 1 || slot == 3,
            slot -> slot == 1
        ));
    }

    @Test
    void incomingFreeSlotReturnsMinusOneWhenOnlyLockedEmptySlotsRemain() {
        assertEquals(-1, ServerFavoriteService.resolveFreeSlotForIncomingItem(
            1,
            4,
            slot -> slot == 1 || slot == 2,
            slot -> true
        ));
    }

    @Test
    void incomingFreeSlotKeepsOriginalWhenItIsAllowed() {
        assertEquals(2, ServerFavoriteService.resolveFreeSlotForIncomingItem(
            2,
            5,
            slot -> slot == 2 || slot == 4,
            slot -> false
        ));
    }

    @Test
    void remainingSpaceSkipsProtectedSelectedThenUsesOffhand() {
        assertEquals(40, ServerFavoriteService.resolveSlotWithRemainingSpace(
            2,
            2,
            slot -> slot == 2 || slot == 40 || slot == 5,
            slot -> slot == 2
        ));
    }

    @Test
    void remainingSpaceSkipsProtectedOffhandAndEarlierMainSlot() {
        assertEquals(6, ServerFavoriteService.resolveSlotWithRemainingSpace(
            40,
            2,
            slot -> slot == 40 || slot == 4 || slot == 6,
            slot -> slot == 40 || slot == 4
        ));
    }

    @Test
    void remainingSpaceKeepsVanillaResultWhenAllowed() {
        assertEquals(4, ServerFavoriteService.resolveSlotWithRemainingSpace(
            4,
            2,
            slot -> slot == 4 || slot == 6,
            slot -> false
        ));
    }
}
