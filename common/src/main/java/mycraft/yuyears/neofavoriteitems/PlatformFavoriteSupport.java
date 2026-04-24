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
import java.util.UUID;
import java.util.function.Consumer;

public final class PlatformFavoriteSupport {
    private static UUID activeClientPlayerId;
    private static boolean clientWorldActive;
    private static boolean clientServerAuthoritative;

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
        updateClientStorageTarget(minecraft);

        if (minecraft.player != null && minecraft.level != null) {
            UUID playerUUID = minecraft.player.getUUID();
            boolean worldChanged = !clientWorldActive || !playerUUID.equals(activeClientPlayerId);
            boolean authorityChanged = clientServerAuthoritative != serverAuthoritative;

            if (worldChanged || authorityChanged) {
                if (clientWorldActive && activeClientPlayerId != null && !clientServerAuthoritative) {
                    DataPersistenceManager.getInstance().saveData(activeClientPlayerId);
                }

                ClientFavoriteSyncService.resetSession();
                FavoritesManager.getStateService().setPlayer(playerUUID);
                FavoritesManager.getStateService().clearFavorites();

                if (!serverAuthoritative) {
                    DataPersistenceManager.getInstance().loadData(playerUUID);
                }

                activeClientPlayerId = playerUUID;
                clientWorldActive = true;
                clientServerAuthoritative = serverAuthoritative;
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
        activeClientPlayerId = null;
        clientWorldActive = false;
        clientServerAuthoritative = false;
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

    private static void updateClientStorageTarget(Minecraft minecraft) {
        if (minecraft.level != null && minecraft.getSingleplayerServer() != null) {
            DataPersistenceManager.getInstance().setWorldSaveDirectory(
                minecraft.getSingleplayerServer().getServerDirectory()
            );
            DataPersistenceManager.getInstance().setClientStorageNamespace(NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY);
            return;
        }

        DataPersistenceManager.getInstance().setWorldSaveDirectory(null);
        ServerData currentServer = minecraft.getCurrentServer();
        if (currentServer != null) {
            DataPersistenceManager.getInstance().setClientStorageNamespace(currentServer.ip);
            return;
        }

        DataPersistenceManager.getInstance().setClientStorageNamespace(NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY);
    }
}
