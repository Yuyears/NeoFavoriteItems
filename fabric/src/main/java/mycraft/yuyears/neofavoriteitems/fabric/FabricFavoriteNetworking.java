package mycraft.yuyears.neofavoriteitems.fabric;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.ServerConfigAccess;
import mycraft.yuyears.neofavoriteitems.ServerRulesSnapshot;
import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

public final class FabricFavoriteNetworking {
    private FabricFavoriteNetworking() {}

    public static void registerCommonPayloads() {
        PayloadTypeRegistry.playC2S().register(ToggleFavoritePayload.TYPE, ToggleFavoritePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestFavoriteSyncPayload.TYPE, RequestFavoriteSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(BypassKeyStatePayload.TYPE, BypassKeyStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(HotbarRowSwapPayload.TYPE, HotbarRowSwapPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestServerConfigPayload.TYPE, RequestServerConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateServerConfigPayload.TYPE, UpdateServerConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncFavoritesPayload.TYPE, SyncFavoritesPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncFavoriteChangesPayload.TYPE, SyncFavoriteChangesPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ServerConfigStatusPayload.TYPE, ServerConfigStatusPayload.STREAM_CODEC);
    }

    public static void registerServerReceivers() {
        ServerFavoriteService.setCorrectionSyncSender(FabricFavoriteNetworking::sendFullSync);
        ServerPlayNetworking.registerGlobalReceiver(ToggleFavoritePayload.TYPE, (payload, context) ->
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                var result = ServerFavoriteService.toggleFavorite(player, payload.inventoryIndex());
                if (result.accepted()) {
                    Set<Integer> addedSlots = result.nowFavorite() ? Set.of(result.changedSlot()) : Set.of();
                    Set<Integer> removedSlots = result.nowFavorite() ? Set.of() : Set.of(result.changedSlot());
                    sendIncrementalSync(player, new SyncFavoriteChangesPayload(result.revision(), addedSlots, removedSlots));
                } else {
                    sendFullSync(player);
                }
            })
        );

        ServerPlayNetworking.registerGlobalReceiver(RequestFavoriteSyncPayload.TYPE, (payload, context) ->
            context.server().execute(() -> sendFullSync(context.player()))
        );

        ServerPlayNetworking.registerGlobalReceiver(BypassKeyStatePayload.TYPE, (payload, context) ->
            context.server().execute(() -> ServerFavoriteService.updateBypassState(context.player(), payload.held()))
        );
        ServerPlayNetworking.registerGlobalReceiver(HotbarRowSwapPayload.TYPE, (payload, context) ->
            context.server().execute(() -> {
                ServerFavoriteService.executeHotbarRowSwap(context.player(), payload.rowIndex(), payload.columnMask());
                sendFullSync(context.player());
            })
        );
        ServerPlayNetworking.registerGlobalReceiver(RequestServerConfigPayload.TYPE, (payload, context) ->
            context.server().execute(() -> sendServerConfigStatus(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(UpdateServerConfigPayload.TYPE, (payload, context) ->
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.hasPermissions(2)) {
                    payload.rules().applyTo(ConfigManager.getInstance().getConfig());
                    ConfigManager.getInstance().saveServerConfig();
                }
                sendServerConfigStatus(player);
            })
        );
    }

    public static void registerClientReceivers() {
        ServerConfigAccess.configureClient(
            () -> ClientPlayNetworking.canSend(RequestServerConfigPayload.TYPE),
            () -> ClientPlayNetworking.send(RequestServerConfigPayload.INSTANCE),
            rules -> ClientPlayNetworking.send(new UpdateServerConfigPayload(rules))
        );
        ClientPlayNetworking.registerGlobalReceiver(SyncFavoritesPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ClientFavoriteSyncService.applyFullSync(payload.revision(), payload.favoriteSlots()))
        );

        ClientPlayNetworking.registerGlobalReceiver(SyncFavoriteChangesPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                var result = ClientFavoriteSyncService.applyIncrementalSync(payload.revision(), payload.addedSlots(), payload.removedSlots());
                if (result == ClientFavoriteSyncService.ApplyResult.GAP) {
                    requestFullSync();
                }
            })
        );
        ClientPlayNetworking.registerGlobalReceiver(ServerConfigStatusPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ServerConfigAccess.receive(true, payload.allowed(), payload.rules()))
        );
    }

    public static boolean trySendToggle(int inventoryIndex) {
        if (!isServerPresent()) {
            DebugLogger.debug("Fabric toggle packet not sent: server_receiver_unavailable inventoryIndex={}", inventoryIndex);
            return false;
        }

        ClientPlayNetworking.send(new ToggleFavoritePayload(inventoryIndex));
        DebugLogger.debug("Fabric sent toggle favorite packet: inventoryIndex={}", inventoryIndex);
        return true;
    }

    public static void requestFullSync() {
        if (!isServerPresent()) {
            DebugLogger.debug("Fabric full sync request not sent: server_receiver_unavailable");
            return;
        }
        ClientPlayNetworking.send(RequestFavoriteSyncPayload.INSTANCE);
        DebugLogger.debug("Fabric requested full favorite sync");
    }

    public static void sendBypassKeyState(boolean held) {
        if (!isServerPresent()) {
            DebugLogger.debug("Fabric bypass key state packet not sent: server_receiver_unavailable held={}", held);
            return;
        }
        ClientPlayNetworking.send(new BypassKeyStatePayload(held));
        DebugLogger.debug("Fabric sent bypass key state: held={}", held);
    }

    public static boolean isServerPresent() {
        return ClientPlayNetworking.canSend(ToggleFavoritePayload.TYPE);
    }

    public static boolean canSendHotbarRowSwap() {
        return ClientPlayNetworking.canSend(HotbarRowSwapPayload.TYPE);
    }

    public static void sendHotbarRowSwap(int rowIndex, int columnMask) {
        ClientPlayNetworking.send(new HotbarRowSwapPayload(rowIndex, columnMask));
    }

    public static SyncFavoritesPayload createFullSyncPayload(ServerPlayer player) {
        return new SyncFavoritesPayload(ServerFavoriteService.currentRevision(player), ServerFavoriteService.getFavoritesFor(player));
    }

    public static void sendFullSync(ServerPlayer player) {
        if (!canSendTo(player, SyncFavoritesPayload.TYPE)) {
            DebugLogger.debug("Fabric full sync skipped: client_receiver_unavailable player={}", player.getGameProfile().getName());
            return;
        }
        ServerPlayNetworking.send(player, createFullSyncPayload(player));
    }

    private static void sendIncrementalSync(ServerPlayer player, SyncFavoriteChangesPayload payload) {
        if (!canSendTo(player, SyncFavoriteChangesPayload.TYPE)) {
            DebugLogger.debug("Fabric incremental sync skipped: client_receiver_unavailable player={}", player.getGameProfile().getName());
            return;
        }
        ServerPlayNetworking.send(player, payload);
    }

    private static boolean canSendTo(ServerPlayer player, CustomPacketPayload.Type<?> payloadType) {
        return player != null && ServerPlayNetworking.canSend(player, payloadType);
    }

    private static void sendServerConfigStatus(ServerPlayer player) {
        if (!canSendTo(player, ServerConfigStatusPayload.TYPE)) return;
        boolean allowed = player.hasPermissions(2);
        ServerPlayNetworking.send(player, new ServerConfigStatusPayload(
            allowed, allowed ? ServerRulesSnapshot.from(ConfigManager.getInstance().getConfig()) : null
        ));
    }

    public record ToggleFavoritePayload(int inventoryIndex) implements CustomPacketPayload {
        public static final Type<ToggleFavoritePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "toggle_favorite")
        );
        public static final StreamCodec<FriendlyByteBuf, ToggleFavoritePayload> STREAM_CODEC =
            StreamCodec.of(ToggleFavoritePayload::write, ToggleFavoritePayload::read);

        private static void write(FriendlyByteBuf buffer, ToggleFavoritePayload payload) {
            buffer.writeInt(payload.inventoryIndex);
        }

        private static ToggleFavoritePayload read(FriendlyByteBuf buffer) {
            return new ToggleFavoritePayload(buffer.readInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestFavoriteSyncPayload() implements CustomPacketPayload {
        public static final RequestFavoriteSyncPayload INSTANCE = new RequestFavoriteSyncPayload();
        public static final Type<RequestFavoriteSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "request_favorite_sync")
        );
        public static final StreamCodec<FriendlyByteBuf, RequestFavoriteSyncPayload> STREAM_CODEC =
            StreamCodec.of(RequestFavoriteSyncPayload::write, RequestFavoriteSyncPayload::read);

        private static void write(FriendlyByteBuf buffer, RequestFavoriteSyncPayload payload) {
        }

        private static RequestFavoriteSyncPayload read(FriendlyByteBuf buffer) {
            return INSTANCE;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BypassKeyStatePayload(boolean held) implements CustomPacketPayload {
        public static final Type<BypassKeyStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "bypass_key_state")
        );
        public static final StreamCodec<FriendlyByteBuf, BypassKeyStatePayload> STREAM_CODEC =
            StreamCodec.of(BypassKeyStatePayload::write, BypassKeyStatePayload::read);

        private static void write(FriendlyByteBuf buffer, BypassKeyStatePayload payload) {
            buffer.writeBoolean(payload.held);
        }

        private static BypassKeyStatePayload read(FriendlyByteBuf buffer) {
            return new BypassKeyStatePayload(buffer.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record HotbarRowSwapPayload(int rowIndex, int columnMask) implements CustomPacketPayload {
        public static final Type<HotbarRowSwapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "hotbar_row_swap")
        );
        public static final StreamCodec<FriendlyByteBuf, HotbarRowSwapPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeByte(payload.rowIndex);
                buffer.writeVarInt(payload.columnMask);
            },
            buffer -> new HotbarRowSwapPayload(buffer.readByte(), buffer.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestServerConfigPayload() implements CustomPacketPayload {
        public static final RequestServerConfigPayload INSTANCE = new RequestServerConfigPayload();
        public static final Type<RequestServerConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "request_server_config")
        );
        public static final StreamCodec<FriendlyByteBuf, RequestServerConfigPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {}, buffer -> INSTANCE);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UpdateServerConfigPayload(ServerRulesSnapshot rules) implements CustomPacketPayload {
        public static final Type<UpdateServerConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "update_server_config")
        );
        public static final StreamCodec<FriendlyByteBuf, UpdateServerConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> payload.rules.write(buffer),
            buffer -> new UpdateServerConfigPayload(ServerRulesSnapshot.read(buffer))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerConfigStatusPayload(boolean allowed, ServerRulesSnapshot rules) implements CustomPacketPayload {
        public static final Type<ServerConfigStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "server_config_status")
        );
        public static final StreamCodec<FriendlyByteBuf, ServerConfigStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.allowed);
                if (payload.allowed) payload.rules.write(buffer);
            },
            buffer -> {
                boolean allowed = buffer.readBoolean();
                return new ServerConfigStatusPayload(allowed, allowed ? ServerRulesSnapshot.read(buffer) : null);
            }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncFavoritesPayload(long revision, Set<Integer> favoriteSlots) implements CustomPacketPayload {
        public static final Type<SyncFavoritesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "sync_favorites")
        );
        public static final StreamCodec<FriendlyByteBuf, SyncFavoritesPayload> STREAM_CODEC =
            StreamCodec.of(SyncFavoritesPayload::write, SyncFavoritesPayload::read);

        private static void write(FriendlyByteBuf buffer, SyncFavoritesPayload payload) {
            buffer.writeLong(payload.revision);
            buffer.writeInt(payload.favoriteSlots.size());
            for (int slot : payload.favoriteSlots) {
                buffer.writeInt(slot);
            }
        }

        private static SyncFavoritesPayload read(FriendlyByteBuf buffer) {
            long revision = buffer.readLong();
            int size = buffer.readInt();
            Set<Integer> favoriteSlots = new HashSet<>();
            for (int i = 0; i < size; i++) {
                favoriteSlots.add(buffer.readInt());
            }
            return new SyncFavoritesPayload(revision, favoriteSlots);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncFavoriteChangesPayload(long revision, Set<Integer> addedSlots, Set<Integer> removedSlots) implements CustomPacketPayload {
        public static final Type<SyncFavoriteChangesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "sync_favorite_changes")
        );
        public static final StreamCodec<FriendlyByteBuf, SyncFavoriteChangesPayload> STREAM_CODEC =
            StreamCodec.of(SyncFavoriteChangesPayload::write, SyncFavoriteChangesPayload::read);

        private static void write(FriendlyByteBuf buffer, SyncFavoriteChangesPayload payload) {
            buffer.writeLong(payload.revision);
            writeSlots(buffer, payload.addedSlots);
            writeSlots(buffer, payload.removedSlots);
        }

        private static SyncFavoriteChangesPayload read(FriendlyByteBuf buffer) {
            return new SyncFavoriteChangesPayload(buffer.readLong(), readSlots(buffer), readSlots(buffer));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void writeSlots(FriendlyByteBuf buffer, Set<Integer> slots) {
        buffer.writeInt(slots.size());
        for (int slot : slots) {
            buffer.writeInt(slot);
        }
    }

    private static Set<Integer> readSlots(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Set<Integer> slots = new HashSet<>();
        for (int i = 0; i < size; i++) {
            slots.add(buffer.readInt());
        }
        return slots;
    }
}
