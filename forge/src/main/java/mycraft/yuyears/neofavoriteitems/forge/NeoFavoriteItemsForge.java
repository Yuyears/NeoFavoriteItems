
package mycraft.yuyears.neofavoriteitems.forge;

import com.mojang.blaze3d.platform.InputConstants;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.client.NeoFavoriteItemsConfigScreen;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.forge.render.ForgeOverlayRenderer;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
    private static net.minecraft.client.KeyMapping configUiKey;
    private static boolean lastLoggedLockOperationKeyState;
    private static boolean lastLoggedBypassLockKeyState;
    private ForgeOverlayRenderer overlayRenderer;

    public NeoFavoriteItemsForge(FMLJavaModLoadingContext loadingContext) {
        NeoFavoriteItemsMod.getInstance().initialize();
        ConfigManager.getInstance().initialize(FMLLoader.getGamePath().resolve("config"));
        var modEventBus = loadingContext.getModEventBus();
        modEventBus.register(IExtensionPoint.DisplayTest.IGNORE_SERVER_VERSION);
        
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
        ForgeFavoriteNetworking.configureClient();
        
        var gameDirectory = FMLLoader.getGamePath();
        PlatformFavoriteSupport.initializeClient(gameDirectory, false);
        
        // 初始化Overlay渲染器
        overlayRenderer = new ForgeOverlayRenderer();
    }

    private void registerKeyBindings(final RegisterKeyMappingsEvent event) {
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
        PlatformFavoriteSupport.onPlayerLoggedIn(event.getEntity(), ForgeFavoriteNetworking::sendFullSync);
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
            ForgeFavoriteNetworking::sendFullSync
        );
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            var minecraft = Minecraft.getInstance();
            PlatformFavoriteSupport.reloadClientConfigIfChanged();
            while (configUiKey != null && configUiKey.consumeClick()) {
                NeoFavoriteItemsConfigScreen.open(minecraft);
            }
            if (minecraft.player != null) {
                logKeyStatesIfChanged();
            }
            PlatformFavoriteSupport.synchronizeClientPersistence(minecraft, ForgeFavoriteNetworking.isServerPresent());
        }
    }

    public static void showSlotToggleMessage(LogicalSlotIndex slot) {
        PlatformFavoriteSupport.showSlotToggleMessage(slot);
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
            DebugLogger.debug("Forge key state changed: lockOperation={} bypass={}", lockHeld, bypassHeld);
            lastLoggedLockOperationKeyState = lockHeld;
            lastLoggedBypassLockKeyState = bypassHeld;
            ForgeFavoriteNetworking.sendBypassKeyState(bypassHeld);
        }
    }
}
