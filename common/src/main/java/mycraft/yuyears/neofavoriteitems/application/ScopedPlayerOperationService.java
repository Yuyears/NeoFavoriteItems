package mycraft.yuyears.neofavoriteitems.application;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class ScopedPlayerOperationService {
    private static final ThreadLocal<Map<UUID, Integer>> inventoryGuardBypassPlayers = ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<UUID, Integer>> deathDropPreservationPlayers = ThreadLocal.withInitial(HashMap::new);

    private ScopedPlayerOperationService() {}

    static void beginInventoryGuardBypass(UUID playerId) {
        begin(inventoryGuardBypassPlayers, playerId);
    }

    static void endInventoryGuardBypass(UUID playerId) {
        end(inventoryGuardBypassPlayers, playerId);
    }

    static boolean isInventoryGuardBypassed(UUID playerId) {
        return isActive(inventoryGuardBypassPlayers, playerId);
    }

    static void beginDeathDropPreservation(UUID playerId) {
        begin(deathDropPreservationPlayers, playerId);
    }

    static void endDeathDropPreservation(UUID playerId) {
        end(deathDropPreservationPlayers, playerId);
    }

    static boolean isDeathDropPreservationActive(UUID playerId) {
        return isActive(deathDropPreservationPlayers, playerId);
    }

    private static void begin(ThreadLocal<Map<UUID, Integer>> scopedPlayers, UUID playerId) {
        if (playerId == null) {
            return;
        }
        scopedPlayers.get().merge(playerId, 1, Integer::sum);
    }

    private static void end(ThreadLocal<Map<UUID, Integer>> scopedPlayers, UUID playerId) {
        if (playerId == null) {
            return;
        }

        Map<UUID, Integer> activePlayers = scopedPlayers.get();
        Integer currentDepth = activePlayers.get(playerId);
        if (currentDepth == null) {
            return;
        }
        if (currentDepth <= 1) {
            activePlayers.remove(playerId);
        } else {
            activePlayers.put(playerId, currentDepth - 1);
        }
        if (activePlayers.isEmpty()) {
            scopedPlayers.remove();
        }
    }

    private static boolean isActive(ThreadLocal<Map<UUID, Integer>> scopedPlayers, UUID playerId) {
        return playerId != null && scopedPlayers.get().getOrDefault(playerId, 0) > 0;
    }
}
