
package mycraft.yuyears.neofavoriteitems.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;
import mycraft.yuyears.neofavoriteitems.neoforge.render.NeoForgeOverlayRenderer;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;



@Mod(NeoFavoriteItemsMod.MOD_ID)
public class NeoFavoriteItemsNeoForge {

    private static net.minecraft.client.KeyMapping lockOperationKey;
    private static net.minecraft.client.KeyMapping bypassLockKey;
    private static boolean lastLoggedLockOperationKeyState;
    private static boolean lastLoggedBypassLockKeyState;
    private NeoForgeOverlayRenderer overlayRenderer;

    public NeoFavoriteItemsNeoForge(IEventBus modBus) {
        NeoFavoriteItemsMod.getInstance().initialize();

        modBus.addListener(this::setup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::registerKeyBindings);
        modBus.addListener(this::registerGuiLayers);
        modBus.addListener(this::registerPayloadHandlers);
        
        NeoForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        NeoFavoriteItemsMod.getInstance().onClientInitialize();
        
        var gameDirectory = FMLLoader.getGamePath();
        
        ConfigManager.getInstance().initialize(
            gameDirectory.resolve("config")
        );
        
        // 初始化数据持久化管理器
        DataPersistenceManager.getInstance().initialize(
            gameDirectory,
            null, // 世界目录将在世界加载时设置
            false
        );
        
        // 初始化Overlay渲染器
        overlayRenderer = new NeoForgeOverlayRenderer();
    }

    private void registerKeyBindings(final RegisterKeyMappingsEvent event) {
        lockOperationKey = new net.minecraft.client.KeyMapping(
            "key.neo_favorite_items.lock_operation",
            GLFW.GLFW_KEY_LEFT_ALT,
            "category.neo_favorite_items"
        );
        bypassLockKey = new net.minecraft.client.KeyMapping(
            "key.neo_favorite_items.bypass_lock",
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "category.neo_favorite_items"
        );
        
        event.register(lockOperationKey);
        event.register(bypassLockKey);
        DebugLogger.debug("Registered NeoForge keybindings: lockOperation default=LEFT_ALT, bypass default=LEFT_CONTROL");
    }

    private void registerGuiLayers(final RegisterGuiLayersEvent event) {
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

    private void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        NeoForgeFavoriteNetworking.registerPackets(event.registrar(NeoFavoriteItemsMod.MOD_ID).versioned("1"));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        NeoFavoriteItemsMod.getInstance().onServerInitialize();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 加载玩家数据
        FavoritesManager.getInstance().setPlayer(event.getEntity().getUUID());
        DataPersistenceManager.getInstance().loadData(event.getEntity().getUUID());
        if (event.getEntity().level().isClientSide()) {
            ClientFavoriteSyncService.resetSession();
        } else if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ServerFavoriteService.resetRevision(serverPlayer);
            NeoForgeFavoriteNetworking.sendFullSync(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 保存玩家数据
        DataPersistenceManager.getInstance().saveData(event.getEntity().getUUID());
        ServerFavoriteService.clearPlayerState(event.getEntity());
        FavoritesManager.getInstance().clearPlayer();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        logKeyStatesIfChanged();
            
        // 当世界加载时，设置世界保存目录
        if (minecraft.level != null && minecraft.getSingleplayerServer() != null) {
            DataPersistenceManager.getInstance().setWorldSaveDirectory(
                minecraft.getSingleplayerServer().getServerDirectory()
            );
        }
            
    }

    public static void showSlotToggleMessage(LogicalSlotIndex slot) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        boolean isFavorite = FavoritesManager.getInstance().isSlotFavorite(slot);
        String key = isFavorite ? "text.neo_favorite_items.slot_marked" : "text.neo_favorite_items.slot_unmarked";
        minecraft.player.displayClientMessage(Component.translatable(key).withStyle(isFavorite ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
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

    public static boolean isBypassKeyHeld() {
        return isKeyHeld(bypassLockKey);
    }

    public static boolean isLockOperationKeyHeld() {
        return isKeyHeld(lockOperationKey);
    }

    private static boolean isKeyHeld(net.minecraft.client.KeyMapping keyMapping) {
        if (keyMapping == null) {
            return false;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        InputConstants.Key key = getBoundKey(keyMapping);
        if (key == null || key == InputConstants.UNKNOWN) {
            return false;
        }
        return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
    }

    private static InputConstants.Key getBoundKey(net.minecraft.client.KeyMapping keyMapping) {
        // 优先尝试 getKey() 方法（1.20.5+）
        InputConstants.Key key = ReflectionHelper.invokeMethod(
            keyMapping, 
            "getKey", 
            InputConstants.Key.class
        );
        if (key != null) {
            return key;
        }

        // 回退到 key 字段（旧版本）
        return ReflectionHelper.readField(keyMapping, "key", InputConstants.Key.class);
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
}
