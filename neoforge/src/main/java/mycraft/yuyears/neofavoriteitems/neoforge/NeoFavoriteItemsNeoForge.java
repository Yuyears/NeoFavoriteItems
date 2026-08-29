
package mycraft.yuyears.neofavoriteitems.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.client.NeoFavoriteItemsConfigScreen;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;
import mycraft.yuyears.neofavoriteitems.neoforge.render.NeoForgeOverlayRenderer;
import mycraft.yuyears.neofavoriteitems.application.InstantSwapCompatService;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;



@Mod(NeoFavoriteItemsMod.MOD_ID)
public class NeoFavoriteItemsNeoForge {
    private static final boolean IS_CLIENT = FMLLoader.getDist().isClient();
    
    private NeoForgeOverlayRenderer overlayRenderer;

    public NeoFavoriteItemsNeoForge(IEventBus modBus, ModContainer modContainer) {
        NeoFavoriteItemsMod.getInstance().initialize();
        ConfigManager.getInstance().initialize(FMLLoader.getGamePath().resolve("config"));

        modBus.addListener(this::setup);
        modBus.addListener(this::registerPayloadHandlers);

        // 仅客户端注册客户端相关监听器
        if (IS_CLIENT) {
            // 创建客户端事件处理器实例并注册到模组总线和 Forge 总线
            ClientEventHandler clientHandler = new ClientEventHandler();
            modBus.addListener(clientHandler::onRegisterKeyMappings);
            modBus.addListener(clientHandler::onClientSetup);
            modBus.addListener(clientHandler::onRegisterGuiLayers);
            NeoForge.EVENT_BUS.register(clientHandler);
        }
        
        // 服务端事件单独注册到新的处理器类
        NeoForge.EVENT_BUS.register(new ServerEventHandler());
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        NeoForgeFavoriteNetworking.registerPackets(event.registrar(NeoFavoriteItemsMod.MOD_ID).versioned(NeoFavoriteItemsConstants.NETWORK_PROTOCOL_VERSION_STRING));
    }

    // ========== 以下方法已迁移到内部类，保留用于向后兼容 ==========

    /**
     * 服务端事件处理器（不包含任何客户端类引用）
     */
    public static class ServerEventHandler {
        @SubscribeEvent
        public void onServerStarting(ServerStartingEvent event) {
            PlatformFavoriteSupport.initializeServer(
                event.getServer().getServerDirectory(),
                event.getServer().getWorldPath(LevelResource.ROOT),
                false
            );
            NeoFavoriteItemsMod.getInstance().onServerInitialize();
        }

        @SubscribeEvent
        public void onServerStopping(ServerStoppingEvent event) {
            PlatformFavoriteSupport.onServerStopping(event.getServer().getPlayerList().getPlayers());
        }

        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            PlatformFavoriteSupport.onPlayerLoggedIn(event.getEntity(), NeoForgeFavoriteNetworking::sendFullSync);
        }

        @SubscribeEvent
        public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            PlatformFavoriteSupport.onPlayerLoggedOut(event.getEntity());
        }

        @SubscribeEvent
        public void onPlayerClone(PlayerEvent.Clone event) {
            PlatformFavoriteSupport.onPlayerCloned(
                event.getOriginal(),
                event.getEntity(),
                event.isWasDeath(),
                NeoForgeFavoriteNetworking::sendFullSync
            );
        }
    }

    /**
     * 客户端事件处理器（可以安全使用客户端类）
     */
    public static class ClientEventHandler {
        private static net.minecraft.client.KeyMapping lockOperationKey;
        private static net.minecraft.client.KeyMapping bypassLockKey;
        private static net.minecraft.client.KeyMapping configUiKey;
        private static boolean lastLoggedLockOperationKeyState;
        private static boolean lastLoggedBypassLockKeyState;
        private static boolean previousLockOperationKeyState;
        private static boolean previousBypassLockKeyState;
        private static InstantSwapCompatService.ModifierMode lastPressedModifier = InstantSwapCompatService.ModifierMode.BYPASS;
        private static final long INSTANT_SWAP_KEY_DEBOUNCE_NANOS = 20_000_000L;
        private static long lockOperationReleasedAtNanos;
        private static long bypassReleasedAtNanos;
        private NeoForgeOverlayRenderer overlayRenderer;

        public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            lockOperationKey = new net.minecraft.client.KeyMapping(
                NeoFavoriteItemsConstants.LOCK_OPERATION_KEY_ID,
                NeoFavoriteItemsConstants.DEFAULT_LOCK_OPERATION_KEY_CODE,
                NeoFavoriteItemsConstants.KEY_CATEGORY
            );
            bypassLockKey = new net.minecraft.client.KeyMapping(
                NeoFavoriteItemsConstants.BYPASS_LOCK_KEY_ID,
                NeoFavoriteItemsConstants.DEFAULT_BYPASS_LOCK_KEY_CODE,
                NeoFavoriteItemsConstants.KEY_CATEGORY
            );
            configUiKey = new net.minecraft.client.KeyMapping(
                NeoFavoriteItemsConstants.CONFIG_UI_KEY_ID,
                NeoFavoriteItemsConstants.DEFAULT_CONFIG_UI_KEY_CODE,
                NeoFavoriteItemsConstants.KEY_CATEGORY
            );
            
            event.register(lockOperationKey);
            event.register(bypassLockKey);
            event.register(configUiKey);
            DebugLogger.debug("Registered NeoForge keybindings: lockOperation default=LEFT_ALT, bypass default=LEFT_CONTROL");
        }

        public void onClientSetup(FMLClientSetupEvent event) {
            NeoFavoriteItemsMod.getInstance().onClientInitialize();
            NeoForgeFavoriteNetworking.configureClient();
            
            var gameDirectory = FMLLoader.getGamePath();
            PlatformFavoriteSupport.initializeClient(gameDirectory, false);
            
            // 初始化Overlay渲染器
            overlayRenderer = new NeoForgeOverlayRenderer();
        }

        public void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "hotbar_favorites"),
                (guiGraphics, deltaTracker) -> {
                    if (overlayRenderer != null) {
                        overlayRenderer.renderHotbarOverlays(guiGraphics);
                    }
                }
            );
        }

        @SubscribeEvent
        public void onClientTick(ClientTickEvent.Post event) {
            var minecraft = Minecraft.getInstance();
            PlatformFavoriteSupport.reloadClientConfigIfChanged();
            while (configUiKey != null && configUiKey.consumeClick()) {
                NeoFavoriteItemsConfigScreen.open(minecraft);
            }
            if (minecraft.player != null) {
                updateModifierMode();
                logKeyStatesIfChanged();
            }

            NeoForgeLockOperationStateMachine.INSTANCE.tick();
            PlatformFavoriteSupport.synchronizeClientPersistence(minecraft, NeoForgeFavoriteNetworking.isServerPresent());
        }

        public static void showSlotToggleMessage(LogicalSlotIndex slot) {
            PlatformFavoriteSupport.showSlotToggleMessage(slot);
        }

        public static boolean isBypassKeyHeld() {
            return isKeyHeld(bypassLockKey);
        }

        public static boolean isLockOperationKeyHeld() {
            return isKeyHeld(lockOperationKey);
        }

        @SubscribeEvent
        public void onKeyInput(InputEvent.Key event) {
            if (matchesKey(lockOperationKey, event.getKey())) {
                if (event.getAction() == InputConstants.PRESS) {
                    lastPressedModifier = InstantSwapCompatService.ModifierMode.MOVE_LOCKS;
                } else if (event.getAction() == InputConstants.RELEASE) {
                    lockOperationReleasedAtNanos = System.nanoTime();
                }
            } else if (matchesKey(bypassLockKey, event.getKey())) {
                if (event.getAction() == InputConstants.PRESS) {
                    lastPressedModifier = InstantSwapCompatService.ModifierMode.BYPASS;
                } else if (event.getAction() == InputConstants.RELEASE) {
                    bypassReleasedAtNanos = System.nanoTime();
                }
            }
        }

        private static boolean matchesKey(net.minecraft.client.KeyMapping mapping, int key) {
            return mapping != null
                && mapping.getKey() != null
                && mapping.getKey().getType() == InputConstants.Type.KEYSYM
                && mapping.getKey().getValue() == key;
        }

        public static InstantSwapCompatService.ModifierMode instantSwapModifierMode() {
            long now = System.nanoTime();
            return InstantSwapCompatService.resolveModifierMode(
                InstantSwapCompatService.isModifierActive(
                    isBypassKeyHeld(), now, bypassReleasedAtNanos, INSTANT_SWAP_KEY_DEBOUNCE_NANOS
                ),
                InstantSwapCompatService.isModifierActive(
                    isLockOperationKeyHeld(), now, lockOperationReleasedAtNanos, INSTANT_SWAP_KEY_DEBOUNCE_NANOS
                ),
                lastPressedModifier
            );
        }

        private static void updateModifierMode() {
            boolean lockHeld = isLockOperationKeyHeld();
            boolean bypassHeld = isBypassKeyHeld();
            if (lockHeld && !previousLockOperationKeyState) {
                lastPressedModifier = InstantSwapCompatService.ModifierMode.MOVE_LOCKS;
            }
            if (bypassHeld && !previousBypassLockKeyState) {
                lastPressedModifier = InstantSwapCompatService.ModifierMode.BYPASS;
            }
            previousLockOperationKeyState = lockHeld;
            previousBypassLockKeyState = bypassHeld;
        }

        private static boolean isKeyHeld(net.minecraft.client.KeyMapping keyMapping) {
            if (keyMapping == null) {
                return false;
            }

            InputConstants.Key key = keyMapping.getKey();
            if (key == null || key == InputConstants.UNKNOWN) {
                return false;
            }
            if (key.getType() != InputConstants.Type.KEYSYM) {
                return keyMapping.isDown();
            }

            var minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getWindow() == null) {
                return false;
            }
            return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
        }

        private static void logKeyStatesIfChanged() {
            boolean lockHeld = isLockOperationKeyHeld();
            boolean bypassHeld = isBypassKeyHeld();
            if (lockHeld != lastLoggedLockOperationKeyState || bypassHeld != lastLoggedBypassLockKeyState) {
                DebugLogger.debug("NeoForge key state changed: lockOperation={} bypass={}", lockHeld, bypassHeld);
                lastLoggedLockOperationKeyState = lockHeld;
                lastLoggedBypassLockKeyState = bypassHeld;
                NeoForgeFavoriteNetworking.sendBypassKeyState(bypassHeld);
            }
        }

        private LogicalSlotIndex getHoveredSlot(Minecraft minecraft) {
            var screen = minecraft.screen;
            if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inventoryScreen) {
                var slot = getHoveredSlotReflectively(inventoryScreen);
                if (slot != null && slot.container == minecraft.player.getInventory()) {
                    return SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot)).orElse(null);
                }
            }
            return null;
        }

        private net.minecraft.world.inventory.Slot getHoveredSlotReflectively(Object screen) {
            return ReflectionHelper.readField(screen, "hoveredSlot", net.minecraft.world.inventory.Slot.class);
        }

        private int getContainerSlotIndex(net.minecraft.world.inventory.Slot slot) {
            Integer methodResult = ReflectionHelper.invokeIntMethod(slot, "getContainerSlot");
            if (methodResult != null) {
                return methodResult;
            }

            Integer fieldResult = ReflectionHelper.readIntField(slot, "slot");
            if (fieldResult != null) {
                return fieldResult;
            }

            fieldResult = ReflectionHelper.readIntField(slot, "index");
            return fieldResult == null ? -1 : fieldResult;
        }
    }

    // ========== 以下方法已迁移到内部类，保留用于向后兼容 ==========
    
    public static void showSlotToggleMessage(LogicalSlotIndex slot) {
        if (IS_CLIENT) {
            ClientEventHandler.showSlotToggleMessage(slot);
        }
    }

    public static boolean isBypassKeyHeld() {
        return IS_CLIENT && ClientEventHandler.isBypassKeyHeld();
    }

    public static boolean isLockOperationKeyHeld() {
        return IS_CLIENT && ClientEventHandler.isLockOperationKeyHeld();
    }

    public static InstantSwapCompatService.ModifierMode instantSwapModifierMode() {
        return IS_CLIENT
            ? ClientEventHandler.instantSwapModifierMode()
            : InstantSwapCompatService.ModifierMode.NONE;
    }
}
