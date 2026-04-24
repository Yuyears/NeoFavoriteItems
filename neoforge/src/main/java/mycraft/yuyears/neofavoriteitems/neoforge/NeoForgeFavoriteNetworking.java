package mycraft.yuyears.neofavoriteitems.neoforge;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.client.Minecraft;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashSet;
import java.util.Set;

public final class NeoForgeFavoriteNetworking {
    private NeoForgeFavoriteNetworking() {}

    public static void registerPackets(PayloadRegistrar registrar) {
        PayloadRegistrar optionalRegistrar = registrar.optional();
        optionalRegistrar.playToServer(ToggleFavoritePayload.TYPE, ToggleFavoritePayload.STREAM_CODEC, ToggleFavoritePayload::handle);
        optionalRegistrar.playToServer(RequestFavoriteSyncPayload.TYPE, RequestFavoriteSyncPayload.STREAM_CODEC, RequestFavoriteSyncPayload::handle);
        optionalRegistrar.playToServer(BypassKeyStatePayload.TYPE, BypassKeyStatePayload.STREAM_CODEC, BypassKeyStatePayload::handle);
        optionalRegistrar.playToClient(SyncFavoritesPayload.TYPE, SyncFavoritesPayload.STREAM_CODEC, SyncFavoritesPayload::handle);
        optionalRegistrar.playToClient(SyncFavoriteChangesPayload.TYPE, SyncFavoriteChangesPayload.STREAM_CODEC, SyncFavoriteChangesPayload::handle);
    }

    public static boolean trySendToggle(int inventoryIndex) {
        if (!isServerPresent()) {
            DebugLogger.debug("NeoForge toggle packet not sent: server_channel_unavailable inventoryIndex={}", inventoryIndex);
            return false;
        }
        PacketDistributor.sendToServer(new ToggleFavoritePayload(inventoryIndex));
        DebugLogger.debug("NeoForge sent toggle favorite packet: inventoryIndex={}", inventoryIndex);
        return true;
    }

    public static void sendBypassKeyState(boolean held) {
        if (!isServerPresent()) {
            DebugLogger.debug("NeoForge bypass key state packet not sent: server_channel_unavailable held={}", held);
            return;
        }
        PacketDistributor.sendToServer(new BypassKeyStatePayload(held));
        DebugLogger.debug("NeoForge sent bypass key state: held={}", held);
    }

    public static void sendFullSync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, createFullSyncPayload(player));
    }

    private static SyncFavoritesPayload createFullSyncPayload(ServerPlayer player) {
        return new SyncFavoritesPayload(ServerFavoriteService.currentRevision(player), ServerFavoriteService.getFavoritesFor(player));
    }

    private static void requestFullSync() {
        if (!isServerPresent()) {
            DebugLogger.debug("NeoForge full sync request not sent: server_channel_unavailable");
            return;
        }
        PacketDistributor.sendToServer(RequestFavoriteSyncPayload.INSTANCE);
        DebugLogger.debug("NeoForge requested full favorite sync");
    }

    public static boolean isServerPresent() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null
            && NetworkRegistry.hasChannel(minecraft.getConnection().getConnection(), ConnectionProtocol.PLAY, ToggleFavoritePayload.TYPE.id());
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

        public static void handle(ToggleFavoritePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                var result = ServerFavoriteService.toggleFavorite(player, payload.inventoryIndex());
                if (result.accepted()) {
                    Set<Integer> addedSlots = result.nowFavorite() ? Set.of(result.changedSlot()) : Set.of();
                    Set<Integer> removedSlots = result.nowFavorite() ? Set.of() : Set.of(result.changedSlot());
                    PacketDistributor.sendToPlayer(player, new SyncFavoriteChangesPayload(result.revision(), addedSlots, removedSlots));
                } else {
                    sendFullSync(player);
                }
            });
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

        public static void handle(RequestFavoriteSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> sendFullSync((ServerPlayer) context.player()));
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

        public static void handle(BypassKeyStatePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ServerFavoriteService.updateBypassState((ServerPlayer) context.player(), payload.held()));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncFavoritesPayload(long revision, Set<Integer> favoriteSlots) implements CustomPacketPayload {
        public static final Type<SyncFavoritesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "sync_favorites")
        );
        public static final StreamCodec<FriendlyByteBuf, SyncFavoritesPayload> STREAM_CODEC =
            StreamCodec.of(SyncFavoritesPayload::write, SyncFavoritesPayload::read);

        private static void write(FriendlyByteBuf buffer, SyncFavoritesPayload payload) {
            buffer.writeLong(payload.revision);
            writeSlots(buffer, payload.favoriteSlots);
        }

        private static SyncFavoritesPayload read(FriendlyByteBuf buffer) {
            return new SyncFavoritesPayload(buffer.readLong(), readSlots(buffer));
        }

        public static void handle(SyncFavoritesPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientFavoriteSyncService.applyFullSync(payload.revision(), payload.favoriteSlots()));
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

        public static void handle(SyncFavoriteChangesPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                var result = ClientFavoriteSyncService.applyIncrementalSync(payload.revision(), payload.addedSlots(), payload.removedSlots());
                if (result == ClientFavoriteSyncService.ApplyResult.GAP) {
                    requestFullSync();
                }
            });
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
