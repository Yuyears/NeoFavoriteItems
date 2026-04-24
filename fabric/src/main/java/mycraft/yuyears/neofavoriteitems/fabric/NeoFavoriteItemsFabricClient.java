package mycraft.yuyears.neofavoriteitems.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.fabric.mixin.KeyMappingAccessor;
import mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport;
import mycraft.yuyears.neofavoriteitems.fabric.render.FabricOverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
        PlatformFavoriteSupport.showSlotToggleMessage(slot);
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
        PlatformFavoriteSupport.initializeClient(client.gameDirectory.toPath());
        DebugLogger.debug("Fabric client initialized; config debug enabled");

        new FabricOverlayRenderer();

        registerKeybindings();
        FabricFavoriteNetworking.registerClientReceivers();
        ClientTickEvents.END_CLIENT_TICK.register(client1 -> {
            if (client1.player != null) {
                logKeyStatesIfChanged();
            }
            PlatformFavoriteSupport.synchronizeClientPersistence(client1, FabricFavoriteNetworking.isServerPresent());
        });
    }

    private void registerKeybindings() {
        lockOperationKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            NeoFavoriteItemsConstants.LOCK_OPERATION_KEY_ID,
            NeoFavoriteItemsConstants.DEFAULT_LOCK_OPERATION_KEY_CODE,
            NeoFavoriteItemsConstants.KEY_CATEGORY
        ));

        bypassLockKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            NeoFavoriteItemsConstants.BYPASS_LOCK_KEY_ID,
            NeoFavoriteItemsConstants.DEFAULT_BYPASS_LOCK_KEY_CODE,
            NeoFavoriteItemsConstants.KEY_CATEGORY
        ));
        DebugLogger.debug("Registered Fabric keybindings: lockOperation default=LEFT_ALT, bypass default=LEFT_CONTROL");
    }
}
