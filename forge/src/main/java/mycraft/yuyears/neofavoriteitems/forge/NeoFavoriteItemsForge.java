
package mycraft.yuyears.neofavoriteitems.forge;

import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.forge.render.ForgeOverlayRenderer;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod(NeoFavoriteItemsMod.MOD_ID)
public class NeoFavoriteItemsForge {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NeoFavoriteItemsMod.MOD_ID);
    public static final RegistryObject<SoundEvent> FEEDBACK_SOUND = SOUNDS.register("feedback",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "feedback")));

    private static net.minecraft.client.KeyMapping lockOperationKey;
    private static net.minecraft.client.KeyMapping bypassLockKey;
    private static boolean lockOperationKeyHeld = false;
    private static boolean bypassKeyHeld = false;
    private ForgeOverlayRenderer overlayRenderer;

    public NeoFavoriteItemsForge(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        NeoFavoriteItemsMod.getInstance().initialize();
        
        var modBus = modEventBus;
        modBus.addListener(this::setup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::registerKeyBindings);
        modBus.addListener(this::addGuiOverlayLayers);
        
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
        DataPersistenceManager.getInstance().loadData(event.getEntity().getUUID());
        FavoritesManager.getInstance().setPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 保存玩家数据
        DataPersistenceManager.getInstance().saveData(event.getEntity().getUUID());
        FavoritesManager.getInstance().clearPlayer();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            
            lockOperationKeyHeld = lockOperationKey.isDown();
            bypassKeyHeld = bypassLockKey.isDown();
            
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
        Class<?> type = screen.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("hoveredSlot");
                field.setAccessible(true);
                return (net.minecraft.world.inventory.Slot) field.get(screen);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    private int getContainerSlotIndex(net.minecraft.world.inventory.Slot slot) {
        Integer methodResult = invokeIntMethod(slot, "getContainerSlot");
        if (methodResult != null) {
            return methodResult;
        }

        Integer fieldResult = readIntField(slot, "slot");
        if (fieldResult != null) {
            return fieldResult;
        }

        fieldResult = readIntField(slot, "index");
        return fieldResult == null ? -1 : fieldResult;
    }

    /**
     * 通过反射调用方法获取整数值
     */
    private Integer invokeIntMethod(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return (Integer) method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * 通过反射读取字段获取整数值
     */
    private Integer readIntField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static boolean isBypassKeyHeld() {
        return bypassKeyHeld;
    }

    public static boolean isLockOperationKeyHeld() {
        return lockOperationKeyHeld;
    }
}
