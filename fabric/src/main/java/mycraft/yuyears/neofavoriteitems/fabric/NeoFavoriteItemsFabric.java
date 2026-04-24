package mycraft.yuyears.neofavoriteitems.fabric;

import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.persistence.DataPersistenceManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport;

public class NeoFavoriteItemsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeoFavoriteItemsMod.getInstance().initialize();
        FabricFavoriteNetworking.registerCommonPayloads();

        onInitializeServer();
    }

    private void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            PlatformFavoriteSupport.initializeServer(server.getServerDirectory());
            NeoFavoriteItemsMod.getInstance().onServerInitialize();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
            PlatformFavoriteSupport.onServerStopping(server.getPlayerList().getPlayers())
        );
        FabricFavoriteNetworking.registerServerReceivers();

        // 处理玩家加入和离开事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            PlatformFavoriteSupport.onPlayerLoggedIn(player, joinedPlayer -> sender.sendPacket(FabricFavoriteNetworking.createFullSyncPayload(joinedPlayer)));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var player = handler.getPlayer();
            PlatformFavoriteSupport.onPlayerLoggedOut(player);
        });
    }

}
