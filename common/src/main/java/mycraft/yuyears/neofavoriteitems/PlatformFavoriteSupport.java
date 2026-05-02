package mycraft.yuyears.neofavoriteitems;

import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.GameRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class PlatformFavoriteSupport {
    private static UUID activeClientPlayerId;
    private static boolean clientWorldActive;
    private static boolean clientServerAuthoritative;
    private static Path activeClientWorldDirectory;
    private static String activeClientStorageNamespace = NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY;

    private PlatformFavoriteSupport() {}

    public static void initializeClient(Path gameDirectory) {
        ConfigManager.getInstance().initialize(gameDirectory.resolve("config"));
        DataPersistenceManager.getInstance().initialize(gameDirectory, null, false);
    }

    public static void initializeServer(Path serverDirectory) {
        initializeServer(serverDirectory, serverDirectory);
    }

    public static void initializeServer(Path gameDirectory, Path worldDirectory) {
        DebugLogger.debug("Platform server persistence init: gameDirectory={} worldDirectory={}", gameDirectory, worldDirectory);
        DataPersistenceManager.getInstance().initialize(gameDirectory, worldDirectory, true);
    }

    public static void onServerStopping(Iterable<? extends Player> players) {
        for (Player player : players) {
            FavoritesManager.getStateService().setPlayer(player.getUUID());
            DataPersistenceManager.getInstance().saveData(player.getUUID());
        }
        DataPersistenceManager.getInstance().saveAllData();
    }

    public static void onPlayerLoggedIn(Player player, Consumer<ServerPlayer> fullSyncSender) {
        if (player.level().isClientSide()) {
            return;
        }

        DebugLogger.debug("Platform player login: name={} uuid={}", player.getName().getString(), player.getUUID());
        FavoritesManager.getStateService().setPlayer(player.getUUID());
        DataPersistenceManager.getInstance().loadData(player.getUUID());

        if (player instanceof ServerPlayer serverPlayer) {
            ServerFavoriteService.resetRevision(serverPlayer);
            DebugLogger.debug(
                "Platform player login full sync: name={} uuid={} slots={}",
                player.getName().getString(),
                player.getUUID(),
                FavoritesManager.getStateService().getFavoriteSlots()
            );
            fullSyncSender.accept(serverPlayer);
        }
    }

    public static void onPlayerLoggedOut(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        FavoritesManager.getStateService().setPlayer(player.getUUID());
        DebugLogger.debug(
            "Platform player logout save: name={} uuid={} slots={}",
            player.getName().getString(),
            player.getUUID(),
            FavoritesManager.getStateService().getFavoriteSlots()
        );
        DataPersistenceManager.getInstance().saveData(player.getUUID());
        ServerFavoriteService.clearPlayerState(player);
        FavoritesManager.getStateService().removePlayer(player.getUUID());
        FavoritesManager.getStateService().clearPlayer();
    }

    public static void onPlayerCloned(Player originalPlayer, Player newPlayer, boolean wasDeath, Consumer<ServerPlayer> fullSyncSender) {
        if (!wasDeath || newPlayer == null || newPlayer.level().isClientSide()) {
            return;
        }

        UUID playerUUID = newPlayer.getUUID();
        FavoritesManager.getStateService().setPlayer(playerUUID);
        boolean keepInventory = newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        if (keepInventory && originalPlayer != null) {
            ServerFavoriteService.runWithInventoryGuardsBypassed(newPlayer, () ->
                newPlayer.getInventory().replaceWith(originalPlayer.getInventory())
            );
            DataPersistenceManager.getInstance().cacheData(playerUUID);
        } else {
            FavoritesManager.getStateService().clearFavorites();
            DataPersistenceManager.getInstance().cacheData(playerUUID);
        }

        if (newPlayer instanceof ServerPlayer serverPlayer) {
            ServerFavoriteService.resetRevision(serverPlayer);
            if (fullSyncSender != null) {
                fullSyncSender.accept(serverPlayer);
            }
        }
    }

    public static void synchronizeClientPersistence(Minecraft minecraft, boolean serverAuthoritative) {
        boolean effectiveServerAuthoritative = isServerAuthoritative(serverAuthoritative, minecraft.getSingleplayerServer() != null);
        ClientStorageTarget storageTarget = resolveClientStorageTarget(minecraft);
        boolean storageChanged = !Objects.equals(activeClientWorldDirectory, storageTarget.worldDirectory())
            || !Objects.equals(activeClientStorageNamespace, storageTarget.namespace());

        if (minecraft.player != null && minecraft.level != null) {
            UUID playerUUID = minecraft.player.getUUID();
            boolean worldChanged = !clientWorldActive || !playerUUID.equals(activeClientPlayerId);
            boolean authorityChanged = clientServerAuthoritative != effectiveServerAuthoritative;

            if (worldChanged || authorityChanged || storageChanged) {
                DebugLogger.debug(
                    "Client persistence context change: uuid={} serverChannel={} singleplayerServer={} effectiveServerAuthoritative={} storageWorld={} storageNamespace={} worldChanged={} authorityChanged={} storageChanged={}",
                    playerUUID,
                    serverAuthoritative,
                    minecraft.getSingleplayerServer() != null,
                    effectiveServerAuthoritative,
                    storageTarget.worldDirectory(),
                    storageTarget.namespace(),
                    worldChanged,
                    authorityChanged,
                    storageChanged
                );
                if (clientWorldActive && activeClientPlayerId != null && !clientServerAuthoritative) {
                    DataPersistenceManager.getInstance().saveData(activeClientPlayerId);
                }

                FavoritesManager.getStateService().setPlayer(playerUUID);

                if (usesClientLocalPersistence(effectiveServerAuthoritative)) {
                    ClientFavoriteSyncService.resetSession();
                    FavoritesManager.getStateService().clearFavorites();
                    applyClientStorageTarget(storageTarget);
                    DataPersistenceManager.getInstance().loadData(playerUUID);
                }

                activeClientPlayerId = playerUUID;
                clientWorldActive = true;
                clientServerAuthoritative = effectiveServerAuthoritative;
                activeClientWorldDirectory = storageTarget.worldDirectory();
                activeClientStorageNamespace = storageTarget.namespace();
            }
            return;
        }

        if (!clientWorldActive) {
            return;
        }

        if (activeClientPlayerId != null && !clientServerAuthoritative) {
            FavoritesManager.getStateService().setPlayer(activeClientPlayerId);
            DataPersistenceManager.getInstance().saveData(activeClientPlayerId);
        }

        FavoritesManager.getStateService().clearPlayer();
        ClientFavoriteSyncService.resetSession();
        if (usesClientLocalPersistence(clientServerAuthoritative)) {
            applyClientStorageTarget(storageTarget);
        }
        activeClientPlayerId = null;
        clientWorldActive = false;
        clientServerAuthoritative = false;
        activeClientWorldDirectory = storageTarget.worldDirectory();
        activeClientStorageNamespace = storageTarget.namespace();
    }

    public static void showSlotToggleMessage(LogicalSlotIndex slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        boolean isFavorite = FavoritesManager.getStateService().isSlotFavorite(slot);
        String translationKey = isFavorite
            ? "text.neo_favorite_items.slot_marked"
            : "text.neo_favorite_items.slot_unmarked";
        minecraft.player.displayClientMessage(
            Component.translatable(translationKey).withStyle(isFavorite ? ChatFormatting.GOLD : ChatFormatting.GRAY),
            true
        );
    }

    private static ClientStorageTarget resolveClientStorageTarget(Minecraft minecraft) {
        if (minecraft.level != null && minecraft.getSingleplayerServer() != null) {
            return new ClientStorageTarget(
                minecraft.getSingleplayerServer().getServerDirectory(),
                NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY
            );
        }

        ServerData currentServer = minecraft.getCurrentServer();
        String remoteAddress = null;
        if (minecraft.getConnection() != null && minecraft.getConnection().getConnection() != null) {
            SocketAddress socketAddress = minecraft.getConnection().getConnection().getRemoteAddress();
            remoteAddress = socketAddress == null ? null : socketAddress.toString();
        }

        return new ClientStorageTarget(null, selectClientStorageNamespace(currentServer == null ? null : currentServer.ip, remoteAddress));
    }

    static String selectClientStorageNamespace(String serverDataIp, String remoteAddress) {
        if (serverDataIp != null && !serverDataIp.isBlank()) {
            return serverDataIp;
        }

        if (remoteAddress != null && !remoteAddress.isBlank()) {
            String normalized = remoteAddress.trim();
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            return normalized;
        }

        return NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY;
    }

    static boolean usesClientLocalPersistence(boolean serverAuthoritative) {
        return !serverAuthoritative;
    }

    static boolean isServerAuthoritative(boolean serverChannelPresent, boolean singleplayerServerPresent) {
        return serverChannelPresent || singleplayerServerPresent;
    }

    private static void applyClientStorageTarget(ClientStorageTarget storageTarget) {
        DataPersistenceManager.getInstance().setWorldSaveDirectory(storageTarget.worldDirectory());
        DataPersistenceManager.getInstance().setClientStorageNamespace(storageTarget.namespace());
    }

    private record ClientStorageTarget(Path worldDirectory, String namespace) {}
}
