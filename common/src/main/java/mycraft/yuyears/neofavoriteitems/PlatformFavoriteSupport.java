package mycraft.yuyears.neofavoriteitems;

import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import mycraft.yuyears.neofavoriteitems.render.CustomAssetRegistry;
import mycraft.yuyears.neofavoriteitems.render.CustomTextureManager;
import mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog;
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
import net.minecraft.Util;

public final class PlatformFavoriteSupport {
    private static UUID activeClientPlayerId;
    private static boolean clientWorldActive;
    private static boolean clientServerAuthoritative;
    private static Path activeClientWorldDirectory;
    private static String activeClientStorageNamespace = NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY;
    private static CustomAssetRegistry customAssetRegistry;
    private static CustomTextureManager customTextureManager;
    private static Path gameDirectory;

    private PlatformFavoriteSupport() {}

    public static boolean isSyntheticPlayer(Player player) {
        if (player == null) {
            return false;
        }
        for (Class<?> type = player.getClass(); type != null; type = type.getSuperclass()) {
            if (isSyntheticPlayerClassName(type.getName())) {
                return true;
            }
        }
        return false;
    }

    static boolean isSyntheticPlayerClassName(String className) {
        return "net.neoforged.neoforge.common.util.FakePlayer".equals(className)
            || "net.minecraftforge.common.util.FakePlayer".equals(className);
    }

    public static void initializeClient(Path gameDirectory) {
        initializeClient(gameDirectory, true);
    }

    public static void initializeClient(Path gameDirectory, boolean initializeLocalConfig) {
        PlatformFavoriteSupport.gameDirectory = gameDirectory;
        if (initializeLocalConfig) {
            ConfigManager.getInstance().initialize(gameDirectory.resolve("config"));
        }
        DataPersistenceManager.getInstance().initialize(gameDirectory, null, false);
        customAssetRegistry = new CustomAssetRegistry(gameDirectory);
        if (customTextureManager != null) customTextureManager.close();
        customTextureManager = new CustomTextureManager(customAssetRegistry);
        refreshCustomTextures();
    }

    public static void openCustomAssetsDirectory() {
        Path root = gameDirectory;
        if (Minecraft.getInstance().gameDirectory != null) root = Minecraft.getInstance().gameDirectory.toPath();
        if (root == null) return;
        Path directory = root.resolve(NeoFavoriteItemsConstants.CUSTOM_ASSETS_DIRECTORY);
        try { java.nio.file.Files.createDirectories(directory); Util.getPlatform().openPath(directory); }
        catch (java.io.IOException | RuntimeException exception) { DebugLogger.warn("Failed to open custom assets directory: {}", exception.getMessage()); }
    }

    public static void reloadClientConfigIfChanged() {
        ConfigManager.getInstance().reloadIfChanged();
    }

    public static CustomAssetRegistry getCustomAssetRegistry() {
        return customAssetRegistry;
    }

    public static CustomTextureManager getCustomTextureManager() {
        return customTextureManager;
    }

    public static CustomTextureManager.RefreshResult refreshCustomTextures() {
        if (customTextureManager == null) return null;
        CustomTextureManager.RefreshResult result = customTextureManager.refresh();
        boolean changed = normalizeMissingCustomMaterial(config -> customTextureManager.contains(config.materialId));
        if (changed) ConfigManager.getInstance().saveConfig();
        return result;
    }

    private static boolean normalizeMissingCustomMaterial(java.util.function.Predicate<OverlayProfileConfig> exists) {
        boolean changed = false;
        var overlay = ConfigManager.getInstance().getConfig().overlay;
        for (OverlayProfileConfig profile : java.util.List.of(
            overlay.locked, overlay.bypass, overlay.lockable, overlay.unlockable
        )) {
            if (profile.materialId != null && profile.materialId.startsWith("custom:") && !exists.test(profile)) {
                profile.materialId = OverlayTextureCatalog.presetId(profile.style);
                changed = true;
            }
        }
        return changed;
    }

    public static void initializeServer(Path serverDirectory) {
        initializeServer(serverDirectory, serverDirectory);
    }

    public static void initializeServer(Path gameDirectory, Path worldDirectory) {
        initializeServer(gameDirectory, worldDirectory, true);
    }

    public static void initializeServer(Path gameDirectory, Path worldDirectory, boolean initializeLocalConfig) {
        DebugLogger.debug("Platform server persistence init: gameDirectory={} worldDirectory={}", gameDirectory, worldDirectory);
        if (initializeLocalConfig) {
            ConfigManager.getInstance().initialize(gameDirectory.resolve("config"));
        }
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
        } else if (originalPlayer != null && ServerFavoriteService.shouldPreserveLockedSlotContentsAfterDeath(newPlayer)) {
            ServerFavoriteService.restorePreservedLockedSlotsAfterDeath(originalPlayer, newPlayer);
            DataPersistenceManager.getInstance().cacheData(playerUUID);
        } else if (!keepInventory) {
            FavoritesManager.getStateService().clearFavorites();
            DataPersistenceManager.getInstance().cacheData(playerUUID);
        } else {
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
        FavoritesManager.getStateService().useClientState();
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
            DataPersistenceManager.getInstance().saveData(activeClientPlayerId);
        }

        FavoritesManager.getStateService().clearPlayer();
        ClientFavoriteSyncService.resetSession();
        ServerConfigAccess.reset();
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
