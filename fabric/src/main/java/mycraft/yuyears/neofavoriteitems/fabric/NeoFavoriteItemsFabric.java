package mycraft.yuyears.neofavoriteitems.fabric;

import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class NeoFavoriteItemsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeoFavoriteItemsMod.getInstance().initialize();

        onInitializeServer();
    }

    private void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> NeoFavoriteItemsMod.getInstance().onServerInitialize());

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

}
