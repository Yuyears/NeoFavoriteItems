
package mycraft.yuyears.neofavoriteitems.forge;

import com.mojang.blaze3d.platform.InputConstants;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.forge.render.ForgeOverlayRenderer;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;



@Mod(NeoFavoriteItemsMod.MOD_ID)
public class NeoFavoriteItemsForge {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NeoFavoriteItemsMod.MOD_ID);
    public static final RegistryObject<SoundEvent> FEEDBACK_SOUND = SOUNDS.register("feedback",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "feedback")));

    private static net.minecraft.client.KeyMapping lockOperationKey;
    private static net.minecraft.client.KeyMapping bypassLockKey;
    private static boolean lastLoggedLockOperationKeyState;
    private static boolean lastLoggedBypassLockKeyState;
    private ForgeOverlayRenderer overlayRenderer;

    public NeoFavoriteItemsForge(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        NeoFavoriteItemsMod.getInstance().initialize();
        
        var modBus = modEventBus;
        modBus.addListener(this::setup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::registerKeyBindings);
        modBus.addListener(this::addGuiOverlayLayers);
        ForgeFavoriteNetworking.registerPackets();
        
        SOUNDS.register(modBus);
        
        MinecraftForge.EVENT_BUS.register(this);
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
        overlayRenderer = new ForgeOverlayRenderer();
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
        DebugLogger.debug("Registered Forge keybindings: lockOperation default=LEFT_ALT, bypass default=LEFT_CONTROL");
    }

    private void addGuiOverlayLayers(final AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(
            ForgeLayeredDraw.VANILLA_ROOT,
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "hotbar_favorites"),
            ForgeLayeredDraw.HOTBAR,
            (guiGraphics, deltaTracker) -> {
                if (overlayRenderer != null) {
                    overlayRenderer.renderHotbarOverlays(guiGraphics);
                }
            }
        );
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
            ForgeFavoriteNetworking.sendFullSync(serverPlayer);
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
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            logKeyStatesIfChanged();
            
            // 当世界加载时，设置世界保存目录
            if (minecraft.level != null && minecraft.getSingleplayerServer() != null) {
                DataPersistenceManager.getInstance().setWorldSaveDirectory(
                    minecraft.getSingleplayerServer().getServerDirectory()
                );
            }
            
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
            DebugLogger.debug("Forge key state changed: lockOperation={} bypass={}", lockHeld, bypassHeld);
            lastLoggedLockOperationKeyState = lockHeld;
            lastLoggedBypassLockKeyState = bypassHeld;
            ForgeFavoriteNetworking.sendBypassKeyState(bypassHeld);
        }
    }
}
