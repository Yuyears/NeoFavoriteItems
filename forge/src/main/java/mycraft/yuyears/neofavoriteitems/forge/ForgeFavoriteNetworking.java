package mycraft.yuyears.neofavoriteitems.forge;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.HashSet;
import java.util.Set;

public final class ForgeFavoriteNetworking {
    private static final SimpleChannel CHANNEL = ChannelBuilder
        .named(ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "favorites"))
        .networkProtocolVersion(NeoFavoriteItemsConstants.NETWORK_PROTOCOL_VERSION)
        .optional()
        .simpleChannel();

    private ForgeFavoriteNetworking() {}

    public static void registerPackets() {
        CHANNEL.messageBuilder(ToggleFavoritePayload.class, 0)
            .codec(ToggleFavoritePayload.STREAM_CODEC)
            .consumerMainThread(ForgeFavoriteNetworking::handleToggleFavorite)
            .add();
        CHANNEL.messageBuilder(RequestFavoriteSyncPayload.class, 1)
            .codec(RequestFavoriteSyncPayload.STREAM_CODEC)
            .consumerMainThread(ForgeFavoriteNetworking::handleRequestSync)
            .add();
        CHANNEL.messageBuilder(SyncFavoritesPayload.class, 2)
            .codec(SyncFavoritesPayload.STREAM_CODEC)
            .consumerMainThread(ForgeFavoriteNetworking::handleFullSync)
            .add();
        CHANNEL.messageBuilder(SyncFavoriteChangesPayload.class, 3)
            .codec(SyncFavoriteChangesPayload.STREAM_CODEC)
            .consumerMainThread(ForgeFavoriteNetworking::handleIncrementalSync)
            .add();
        CHANNEL.messageBuilder(BypassKeyStatePayload.class, 4)
            .codec(BypassKeyStatePayload.STREAM_CODEC)
            .consumerMainThread(ForgeFavoriteNetworking::handleBypassKeyState)
            .add();
    }

    public static boolean trySendToggle(int inventoryIndex) {
        if (!isServerPresent()) {
            DebugLogger.debug("Forge toggle packet not sent: server_channel_unavailable inventoryIndex={}", inventoryIndex);
            return false;
        }
        CHANNEL.send(new ToggleFavoritePayload(inventoryIndex), PacketDistributor.SERVER.noArg());
        DebugLogger.debug("Forge sent toggle favorite packet: inventoryIndex={}", inventoryIndex);
        return true;
    }

    public static void sendBypassKeyState(boolean held) {
        if (!isServerPresent()) {
            DebugLogger.debug("Forge bypass key state packet not sent: server_channel_unavailable held={}", held);
            return;
        }
        CHANNEL.send(new BypassKeyStatePayload(held), PacketDistributor.SERVER.noArg());
        DebugLogger.debug("Forge sent bypass key state: held={}", held);
    }

    public static void sendFullSync(ServerPlayer player) {
        if (!canSendTo(player)) {
            DebugLogger.debug("Forge full sync skipped: client_channel_unavailable player={}", player.getGameProfile().getName());
            return;
        }
        CHANNEL.send(createFullSyncPayload(player), PacketDistributor.PLAYER.with(player));
    }

    private static SyncFavoritesPayload createFullSyncPayload(ServerPlayer player) {
        return new SyncFavoritesPayload(ServerFavoriteService.currentRevision(player), ServerFavoriteService.getFavoritesFor(player));
    }

    private static void requestFullSync() {
        if (!isServerPresent()) {
            DebugLogger.debug("Forge full sync request not sent: server_channel_unavailable");
            return;
        }
        CHANNEL.send(RequestFavoriteSyncPayload.INSTANCE, PacketDistributor.SERVER.noArg());
        DebugLogger.debug("Forge requested full favorite sync");
    }

    public static boolean isServerPresent() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null
            && CHANNEL.isRemotePresent(minecraft.getConnection().getConnection());
    }

    private static void handleToggleFavorite(ToggleFavoritePayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            var result = ServerFavoriteService.toggleFavorite(player, payload.inventoryIndex());
            if (result.accepted()) {
                Set<Integer> addedSlots = result.nowFavorite() ? Set.of(result.changedSlot()) : Set.of();
                Set<Integer> removedSlots = result.nowFavorite() ? Set.of() : Set.of(result.changedSlot());
                sendIncrementalSync(player, new SyncFavoriteChangesPayload(result.revision(), addedSlots, removedSlots));
            } else {
                sendFullSync(player);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleRequestSync(RequestFavoriteSyncPayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                sendFullSync(player);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleFullSync(SyncFavoritesPayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> ClientFavoriteSyncService.applyFullSync(payload.revision(), payload.favoriteSlots()));
        context.setPacketHandled(true);
    }

    private static void handleIncrementalSync(SyncFavoriteChangesPayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            var result = ClientFavoriteSyncService.applyIncrementalSync(payload.revision(), payload.addedSlots(), payload.removedSlots());
            if (result == ClientFavoriteSyncService.ApplyResult.GAP) {
                requestFullSync();
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleBypassKeyState(BypassKeyStatePayload payload, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerFavoriteService.updateBypassState(player, payload.held());
            }
        });
        context.setPacketHandled(true);
    }

    private static void sendIncrementalSync(ServerPlayer player, SyncFavoriteChangesPayload payload) {
        if (!canSendTo(player)) {
            DebugLogger.debug("Forge incremental sync skipped: client_channel_unavailable player={}", player.getGameProfile().getName());
            return;
        }
        CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
    }

    private static boolean canSendTo(ServerPlayer player) {
        return player != null && CHANNEL.isRemotePresent(player.connection.getConnection());
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
