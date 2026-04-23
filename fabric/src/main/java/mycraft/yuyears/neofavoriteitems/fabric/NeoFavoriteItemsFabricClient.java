package mycraft.yuyears.neofavoriteitems.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.fabric.mixin.KeyMappingAccessor;
import mycraft.yuyears.neofavoriteitems.fabric.render.FabricOverlayRenderer;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class NeoFavoriteItemsFabricClient implements ClientModInitializer {
    private static KeyMapping lockOperationKey;
    private static KeyMapping bypassLockKey;
    private static boolean lastLoggedLockOperationKeyState;
    private static boolean lastLoggedBypassLockKeyState;

    public static void showSlotToggleMessage(LogicalSlotIndex slot) {
        var client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        boolean isFavorite = FavoritesManager.getInstance().isSlotFavorite(slot);
        String key = isFavorite ? "text.neo_favorite_items.slot_marked" : "text.neo_favorite_items.slot_unmarked";
        client.player.displayClientMessage(Component.translatable(key).withStyle(isFavorite ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
    }

    public static boolean isBypassKeyHeld() {
        return isKeyHeld(bypassLockKey);
    }

    public static boolean isLockOperationKeyHeld() {
        return isKeyHeld(lockOperationKey);
    }

    private static boolean isKeyHeld(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return false;
        }

        var client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }

        InputConstants.Key key = ((KeyMappingAccessor) keyMapping).neoFavoriteItems$getKey();
        if (key == null || key == InputConstants.UNKNOWN) {
            return false;
        }
        return InputConstants.isKeyDown(client.getWindow().getWindow(), key.getValue());
    }

    public static void logKeyStatesIfChanged() {
        boolean lockHeld = isLockOperationKeyHeld();
        boolean bypassHeld = isBypassKeyHeld();
        if (lockHeld != lastLoggedLockOperationKeyState || bypassHeld != lastLoggedBypassLockKeyState) {
            DebugLogger.debug("Fabric key state changed: lockOperation={} bypass={}", lockHeld, bypassHeld);
            lastLoggedLockOperationKeyState = lockHeld;
            lastLoggedBypassLockKeyState = bypassHeld;
            FabricFavoriteNetworking.sendBypassKeyState(bypassHeld);
        }
    }

    @Override
    public void onInitializeClient() {
        NeoFavoriteItemsMod.getInstance().onClientInitialize();

        var client = Minecraft.getInstance();

        ConfigManager.getInstance().initialize(
            client.gameDirectory.toPath().resolve("config")
        );
        DebugLogger.debug("Fabric client initialized; config debug enabled");

        DataPersistenceManager.getInstance().initialize(
            client.gameDirectory.toPath(),
            null,
            false
        );

        new FabricOverlayRenderer();

        registerKeybindings();
        FabricFavoriteNetworking.registerClientReceivers();
        ClientTickEvents.END_CLIENT_TICK.register(client1 -> {
            if (client1.player != null) {
                logKeyStatesIfChanged();
            }
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client1, world) -> {
            if (world != null && client1.player != null) {
                ClientFavoriteSyncService.resetSession();
                FavoritesManager.getInstance().setPlayer(client1.player.getUUID());
                DataPersistenceManager.getInstance().loadData(client1.player.getUUID());
            } else if (world == null && client1.player != null) {
                DataPersistenceManager.getInstance().saveData(client1.player.getUUID());
                FavoritesManager.getInstance().clearPlayer();
            }
        });
    }

    private void registerKeybindings() {
        lockOperationKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.neo_favorite_items.lock_operation",
            GLFW.GLFW_KEY_LEFT_ALT,
            "category.neo_favorite_items"
        ));

        bypassLockKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.neo_favorite_items.bypass_lock",
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "category.neo_favorite_items"
        ));
        DebugLogger.debug("Registered Fabric keybindings: lockOperation default=LEFT_ALT, bypass default=LEFT_CONTROL");
    }
}
