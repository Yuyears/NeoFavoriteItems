
package mycraft.yuyears.newitemfavorites.forge;

import mycraft.yuyears.newitemfavorites.ConfigManager;
import mycraft.yuyears.newitemfavorites.FavoritesManager;
import mycraft.yuyears.newitemfavorites.NewItemFavoritesMod;
import mycraft.yuyears.newitemfavorites.forge.render.ForgeOverlayRenderer;
import mycraft.yuyears.newitemfavorites.persistence.DataPersistenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.glfw.GLFW;

@Mod(NewItemFavoritesMod.MOD_ID)
public class NewItemFavoritesForge {
    public static final DeferredRegister&lt;SoundEvent&gt; SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NewItemFavoritesMod.MOD_ID);
    public static final RegistryObject&lt;SoundEvent&gt; FEEDBACK_SOUND = SOUNDS.register("feedback",
        () -&gt; SoundEvent.createVariableRangeEvent(new ResourceLocation(NewItemFavoritesMod.MOD_ID, "feedback")));

    private static net.minecraft.client.KeyMapping toggleFavoriteKey;
    private static net.minecraft.client.KeyMapping bypassLockKey;
    private static boolean bypassKeyHeld = false;

    public NewItemFavoritesForge() {
        NewItemFavoritesMod.getInstance().initialize();
        
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::setup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::registerKeyBindings);
        
        SOUNDS.register(modBus);
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        NewItemFavoritesMod.getInstance().onClientInitialize();
        
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
        new ForgeOverlayRenderer();
    }

    private void registerKeyBindings(final RegisterKeyMappingsEvent event) {
        toggleFavoriteKey = new net.minecraft.client.KeyMapping(
            "key.new_item_favorites.toggle_favorite",
            GLFW.GLFW_KEY_F,
            "category.new_item_favorites"
        );
        bypassLockKey = new net.minecraft.client.KeyMapping(
            "key.new_item_favorites.bypass_lock",
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "category.new_item_favorites"
        );
        
        event.register(toggleFavoriteKey);
        event.register(bypassLockKey);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        NewItemFavoritesMod.getInstance().onServerInitialize();
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
            if (minecraft.player == null) return;
            
            bypassKeyHeld = bypassLockKey.isDown();
            
            // 当世界加载时，设置世界保存目录
            if (minecraft.level != null && minecraft.getSingleplayerServer() != null) {
                DataPersistenceManager.getInstance().setWorldSaveDirectory(
                    minecraft.getSingleplayerServer().getWorldPath(minecraft.level.dimension())
                );
            }
            
            while (toggleFavoriteKey.consumeClick()) {
                if (minecraft.screen != null && minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
                    int hoveredSlot = getHoveredSlot(minecraft);
                    if (hoveredSlot >= 0) {
                        FavoritesManager.getInstance().toggleSlotFavorite(hoveredSlot);
                        boolean isFavorite = FavoritesManager.getInstance().isSlotFavorite(hoveredSlot);
                        String message = isFavorite ? "Slot marked as favorite!" : "Slot unmarked as favorite!";
                        minecraft.player.displayClientMessage(Component.literal(message), true);
                    }
                }
            }
        }
    }



    private int getHoveredSlot(Minecraft minecraft) {
        var screen = minecraft.screen;
        if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inventoryScreen) {
            var slot = inventoryScreen.hoveredSlot;
            return slot != null ? slot.index : -1;
        }
        return -1;
    }

    public static boolean isBypassKeyHeld() {
        return bypassKeyHeld;
    }
}
