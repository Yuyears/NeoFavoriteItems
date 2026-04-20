
package mycraft.yuyears.newitemfavorites.fabric;

import mycraft.yuyears.newitemfavorites.ConfigManager;
import mycraft.yuyears.newitemfavorites.FavoritesManager;
import mycraft.yuyears.newitemfavorites.NewItemFavoritesMod;
import mycraft.yuyears.newitemfavorites.domain.LogicalSlotIndex;
import mycraft.yuyears.newitemfavorites.fabric.render.FabricOverlayRenderer;
import mycraft.yuyears.newitemfavorites.integration.SlotMappingService;
import mycraft.yuyears.newitemfavorites.persistence.DataPersistenceManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NewItemFavoritesFabric implements ModInitializer {
    private static KeyMapping toggleFavoriteKey;
    private static KeyMapping bypassLockKey;
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
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            // 加载玩家数据
            DataPersistenceManager.getInstance().loadData(player.getUUID());
            FavoritesManager.getInstance().setPlayer(player.getUUID());
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var player = handler.getPlayer();
            // 保存玩家数据
            DataPersistenceManager.getInstance().saveData(player.getUUID());
            FavoritesManager.getInstance().clearPlayer();
        });
    }

    public void onInitializeClient() {
        NewItemFavoritesMod.getInstance().onClientInitialize();
        
        var client = Minecraft.getInstance();
        
        ConfigManager.getInstance().initialize(
            client.gameDirectory.toPath().resolve("config")
        );
        
        // 初始化数据持久化管理器
        DataPersistenceManager.getInstance().initialize(
            client.gameDirectory.toPath(),
            null, // 世界目录将在世界加载时设置
            false
        );
        
        // 初始化Overlay渲染器
        new FabricOverlayRenderer();
        
        registerKeybindings();
        
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client1, world) -> {
            if (world != null && client1.player != null) {
                DataPersistenceManager.getInstance().loadData(client1.player.getUUID());
                FavoritesManager.getInstance().setPlayer(client1.player.getUUID());
            } else if (world == null && client1.player != null) {
                DataPersistenceManager.getInstance().saveData(client1.player.getUUID());
                FavoritesManager.getInstance().clearPlayer();
            }
        });
        
        ClientTickEvents.END_CLIENT_TICK.register(tickClient -> handleKeyInput(tickClient));
    }

    private void registerKeybindings() {
        var config = ConfigManager.getInstance().getConfig();
        
        toggleFavoriteKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.new_item_favorites.toggle_favorite",
            GLFW.GLFW_KEY_F,
            "category.new_item_favorites"
        ));
        
        bypassLockKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.new_item_favorites.bypass_lock",
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "category.new_item_favorites"
        ));
    }

    private void handleKeyInput(Minecraft client) {
        bypassKeyHeld = bypassLockKey.isDown();
        
        while (toggleFavoriteKey.consumeClick()) {
            if (client.player != null && client.screen != null) {
                var screen = client.screen;
                if (screen instanceof AbstractContainerScreen<?>) {
                    LogicalSlotIndex hoveredSlot = getHoveredSlot(client);
                    if (hoveredSlot != null) {
                        FavoritesManager.getInstance().toggleSlotFavorite(hoveredSlot);
                        boolean isFavorite = FavoritesManager.getInstance().isSlotFavorite(hoveredSlot);
                        String message = isFavorite ? "Slot marked as favorite!" : "Slot unmarked as favorite!";
                        client.player.displayClientMessage(Component.literal(message).withStyle(isFavorite ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
                    }
                }
            }
        }
    }

    private LogicalSlotIndex getHoveredSlot(Minecraft client) {
        var screen = client.screen;
        if (screen instanceof AbstractContainerScreen<?>) {
            Slot slot = getHoveredSlotReflectively(screen);
            if (slot != null && slot.container == client.player.getInventory()) {
                return SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot)).orElse(null);
            }
        }
        return null;
    }

    private Slot getHoveredSlotReflectively(Object screen) {
        try {
            var field = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
            field.setAccessible(true);
            return (Slot) field.get(screen);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private int getContainerSlotIndex(Slot slot) {
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

    private Integer invokeIntMethod(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return (Integer) method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

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
}
