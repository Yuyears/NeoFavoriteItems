package mycraft.yuyears.neofavoriteitems;

import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
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
        DataPersistenceManager.getInstance().initialize(serverDirectory, serverDirectory, true);
        DataPersistenceManager.getInstance().loadAllData();
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

        FavoritesManager.getStateService().setPlayer(player.getUUID());
        DataPersistenceManager.getInstance().loadData(player.getUUID());

        if (player instanceof ServerPlayer serverPlayer) {
            ServerFavoriteService.resetRevision(serverPlayer);
            fullSyncSender.accept(serverPlayer);
        }
    }

    public static void onPlayerLoggedOut(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        FavoritesManager.getStateService().setPlayer(player.getUUID());
        DataPersistenceManager.getInstance().saveData(player.getUUID());
        ServerFavoriteService.clearPlayerState(player);
        FavoritesManager.getStateService().removePlayer(player.getUUID());
        FavoritesManager.getStateService().clearPlayer();
    }

    public static void synchronizeClientPersistence(Minecraft minecraft, boolean serverAuthoritative) {
        ClientStorageTarget storageTarget = resolveClientStorageTarget(minecraft);
        boolean storageChanged = !Objects.equals(activeClientWorldDirectory, storageTarget.worldDirectory())
            || !Objects.equals(activeClientStorageNamespace, storageTarget.namespace());

        if (minecraft.player != null && minecraft.level != null) {
            UUID playerUUID = minecraft.player.getUUID();
            boolean worldChanged = !clientWorldActive || !playerUUID.equals(activeClientPlayerId);
            boolean authorityChanged = clientServerAuthoritative != serverAuthoritative;

            if (worldChanged || authorityChanged || storageChanged) {
                if (clientWorldActive && activeClientPlayerId != null && !clientServerAuthoritative) {
                    DataPersistenceManager.getInstance().saveData(activeClientPlayerId);
                }

                applyClientStorageTarget(storageTarget);
                ClientFavoriteSyncService.resetSession();
                FavoritesManager.getStateService().setPlayer(playerUUID);
                FavoritesManager.getStateService().clearFavorites();

                if (!serverAuthoritative) {
                    DataPersistenceManager.getInstance().loadData(playerUUID);
                }

                activeClientPlayerId = playerUUID;
                clientWorldActive = true;
                clientServerAuthoritative = serverAuthoritative;
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
        applyClientStorageTarget(storageTarget);
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

    private static void applyClientStorageTarget(ClientStorageTarget storageTarget) {
        DataPersistenceManager.getInstance().setWorldSaveDirectory(storageTarget.worldDirectory());
        DataPersistenceManager.getInstance().setClientStorageNamespace(storageTarget.namespace());
    }

    private record ClientStorageTarget(Path worldDirectory, String namespace) {}
}
