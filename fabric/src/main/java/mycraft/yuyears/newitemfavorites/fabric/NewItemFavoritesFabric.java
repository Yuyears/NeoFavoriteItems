
package mycraft.yuyears.newitemfavorites.fabric;

import mycraft.yuyears.newitemfavorites.ConfigManager;
import mycraft.yuyears.newitemfavorites.FavoritesManager;
import mycraft.yuyears.newitemfavorites.NewItemFavoritesMod;
import mycraft.yuyears.newitemfavorites.fabric.render.FabricOverlayRenderer;
import mycraft.yuyears.newitemfavorites.persistence.DataPersistenceManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerPlayerEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class NewItemFavoritesFabric implements ModInitializer {
    private static KeyBinding toggleFavoriteKey;
    private static KeyBinding bypassLockKey;
    private static boolean bypassKeyHeld = false;

    @Override
    public void onInitialize() {
        NewItemFavoritesMod.getInstance().initialize();
        
        onInitializeServer();
        onInitializeClient();
    }

    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            NewItemFavoritesMod.getInstance().onServerInitialize();
        });
        
        // 处理玩家加入和离开事件
        ServerPlayerEvents.JOIN.register((server, player) -> {
            // 加载玩家数据
            DataPersistenceManager.getInstance().loadData(player.getUuid());
            FavoritesManager.getInstance().setPlayer(player.getUuid());
        });
        
        ServerPlayerEvents.LEAVE.register((server, player) -> {
            // 保存玩家数据
            DataPersistenceManager.getInstance().saveData(player.getUuid());
            FavoritesManager.getInstance().clearPlayer();
        });
    }

    public void onInitializeClient() {
        NewItemFavoritesMod.getInstance().onClientInitialize();
        
        var client = MinecraftClient.getInstance();
        
        ConfigManager.getInstance().initialize(
            client.runDirectory.toPath().resolve("config")
        );
        
        // 初始化数据持久化管理器
        DataPersistenceManager.getInstance().initialize(
            client.runDirectory.toPath(),
            null, // 世界目录将在世界加载时设置
            false
        );
        
        // 初始化Overlay渲染器
        new FabricOverlayRenderer();
        
        registerKeybindings();
        
        // 处理世界加载事件
        ClientWorldEvents.LOAD.register((client1, world) -> {
            // 当世界加载时，设置世界保存目录
            if (client1.getServer() != null && client1.getServer().getWorldSaveDirectory() != null) {
                DataPersistenceManager.getInstance().setWorldSaveDirectory(
                    client1.getServer().getWorldSaveDirectory().toPath()
                );
                
                // 加载当前玩家数据
                if (client1.player != null) {
                    DataPersistenceManager.getInstance().loadData(client1.player.getUuid());
                    FavoritesManager.getInstance().setPlayer(client1.player.getUuid());
                }
            }
        });
        
        // 处理客户端断开连接事件
        ClientWorldEvents.UNLOAD.register((client1, world) -> {
            // 保存当前玩家数据
            if (client1.player != null) {
                DataPersistenceManager.getInstance().saveData(client1.player.getUuid());
                FavoritesManager.getInstance().clearPlayer();
            }
        });
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeyInput(client);
        });
    }

    private void registerKeybindings() {
        var config = ConfigManager.getInstance().getConfig();
        
        toggleFavoriteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.new_item_favorites.toggle_favorite",
            GLFW.GLFW_KEY_F,
            "category.new_item_favorites"
        ));
        
        bypassLockKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.new_item_favorites.bypass_lock",
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "category.new_item_favorites"
        ));
    }

    private void handleKeyInput(MinecraftClient client) {
        bypassKeyHeld = bypassLockKey.isPressed();
        
        while (toggleFavoriteKey.wasPressed()) {
            if (client.player != null &amp;&amp; client.currentScreen != null) {
                var screen = client.currentScreen;
                if (screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
                    int hoveredSlot = getHoveredSlot(client);
                    if (hoveredSlot &gt;= 0) {
                        FavoritesManager.getInstance().toggleSlotFavorite(hoveredSlot);
                        boolean isFavorite = FavoritesManager.getInstance().isSlotFavorite(hoveredSlot);
                        String message = isFavorite ? "Slot marked as favorite!" : "Slot unmarked as favorite!";
                        client.player.sendMessage(Text.literal(message).formatted(isFavorite ? Formatting.GOLD : Formatting.GRAY), true);
                    }
                }
            }
        }
    }

    private int getHoveredSlot(MinecraftClient client) {
        var screen = client.currentScreen;
        if (screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen inventoryScreen) {
            var slot = inventoryScreen.focusedSlot;
            return slot != null ? slot.id : -1;
        }
        return -1;
    }

    public static boolean isBypassKeyHeld() {
        return bypassKeyHeld;
    }
}
