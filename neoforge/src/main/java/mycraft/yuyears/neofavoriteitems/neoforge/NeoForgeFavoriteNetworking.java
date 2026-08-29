package mycraft.yuyears.neofavoriteitems.neoforge;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConstants;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.ServerConfigAccess;
import mycraft.yuyears.neofavoriteitems.ServerRulesSnapshot;
import mycraft.yuyears.neofavoriteitems.application.ClientFavoriteSyncService;
import mycraft.yuyears.neofavoriteitems.application.InstantSwapCompatService;
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
        ServerFavoriteService.setCorrectionSyncSender(NeoForgeFavoriteNetworking::sendFullSync);
        PayloadRegistrar optionalRegistrar = registrar.optional();
        optionalRegistrar.playToServer(ToggleFavoritePayload.TYPE, ToggleFavoritePayload.STREAM_CODEC, ToggleFavoritePayload::handle);
        optionalRegistrar.playToServer(RequestFavoriteSyncPayload.TYPE, RequestFavoriteSyncPayload.STREAM_CODEC, RequestFavoriteSyncPayload::handle);
        optionalRegistrar.playToServer(BypassKeyStatePayload.TYPE, BypassKeyStatePayload.STREAM_CODEC, BypassKeyStatePayload::handle);
        optionalRegistrar.playToServer(PrepareInstantSwapPayload.TYPE, PrepareInstantSwapPayload.STREAM_CODEC, PrepareInstantSwapPayload::handle);
        optionalRegistrar.playToServer(CompleteInstantSwapPayload.TYPE, CompleteInstantSwapPayload.STREAM_CODEC, CompleteInstantSwapPayload::handle);
        optionalRegistrar.playToServer(MoveCreativeFavoritePairsPayload.TYPE, MoveCreativeFavoritePairsPayload.STREAM_CODEC, MoveCreativeFavoritePairsPayload::handle);
        optionalRegistrar.playToServer(RequestServerConfigPayload.TYPE, RequestServerConfigPayload.STREAM_CODEC, RequestServerConfigPayload::handle);
        optionalRegistrar.playToServer(UpdateServerConfigPayload.TYPE, UpdateServerConfigPayload.STREAM_CODEC, UpdateServerConfigPayload::handle);
        optionalRegistrar.playToClient(SyncFavoritesPayload.TYPE, SyncFavoritesPayload.STREAM_CODEC, SyncFavoritesPayload::handle);
        optionalRegistrar.playToClient(SyncFavoriteChangesPayload.TYPE, SyncFavoriteChangesPayload.STREAM_CODEC, SyncFavoriteChangesPayload::handle);
        optionalRegistrar.playToClient(ServerConfigStatusPayload.TYPE, ServerConfigStatusPayload.STREAM_CODEC, ServerConfigStatusPayload::handle);
    }

    public static void configureClient() {
        ServerConfigAccess.configureClient(
            () -> hasServerChannel(RequestServerConfigPayload.TYPE.id()),
            () -> PacketDistributor.sendToServer(RequestServerConfigPayload.INSTANCE),
            rules -> PacketDistributor.sendToServer(new UpdateServerConfigPayload(rules))
        );
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

    public static boolean tryPrepareInstantSwap(
        InstantSwapCompatService.Operation operation,
        int containerId,
        int targetMenuSlot,
        int hotbarIndex,
        int auxiliaryMenuSlot
    ) {
        if (!hasServerChannel(PrepareInstantSwapPayload.TYPE.id())) {
            return false;
        }
        PacketDistributor.sendToServer(new PrepareInstantSwapPayload(
            operation,
            containerId,
            targetMenuSlot,
            hotbarIndex,
            auxiliaryMenuSlot
        ));
        return true;
    }

    public static void completeInstantSwap() {
        if (hasServerChannel(CompleteInstantSwapPayload.TYPE.id())) {
            PacketDistributor.sendToServer(CompleteInstantSwapPayload.INSTANCE);
        }
    }

    public static void moveCreativeFavoritePairs(int[] slotPairs) {
        if (hasServerChannel(MoveCreativeFavoritePairsPayload.TYPE.id())) {
            PacketDistributor.sendToServer(new MoveCreativeFavoritePairsPayload(slotPairs.clone(), false));
        }
    }

    public static void executeCreativeSwapPairs(int[] slotPairs) {
        if (hasServerChannel(MoveCreativeFavoritePairsPayload.TYPE.id())) {
            PacketDistributor.sendToServer(new MoveCreativeFavoritePairsPayload(slotPairs.clone(), true));
        }
    }

    public static void sendFullSync(ServerPlayer player) {
        if (!canSendTo(player, SyncFavoritesPayload.TYPE.id())) {
            DebugLogger.debug("NeoForge full sync skipped: client_channel_unavailable player={}", player.getGameProfile().getName());
            return;
        }
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
        return hasServerChannel(ToggleFavoritePayload.TYPE.id());
    }

    private static boolean hasServerChannel(ResourceLocation payloadId) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null
            && NetworkRegistry.hasChannel(minecraft.getConnection().getConnection(), ConnectionProtocol.PLAY, payloadId);
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
                    sendIncrementalSync(player, new SyncFavoriteChangesPayload(result.revision(), addedSlots, removedSlots));
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

    public record PrepareInstantSwapPayload(
        InstantSwapCompatService.Operation operation,
        int containerId,
        int targetMenuSlot,
        int hotbarIndex,
        int auxiliaryMenuSlot
    ) implements CustomPacketPayload {
        public static final Type<PrepareInstantSwapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "prepare_instant_swap")
        );
        public static final StreamCodec<FriendlyByteBuf, PrepareInstantSwapPayload> STREAM_CODEC =
            StreamCodec.of(PrepareInstantSwapPayload::write, PrepareInstantSwapPayload::read);

        private static void write(FriendlyByteBuf buffer, PrepareInstantSwapPayload payload) {
            buffer.writeEnum(payload.operation);
            buffer.writeInt(payload.containerId);
            buffer.writeInt(payload.targetMenuSlot);
            buffer.writeInt(payload.hotbarIndex);
            buffer.writeInt(payload.auxiliaryMenuSlot);
        }

        private static PrepareInstantSwapPayload read(FriendlyByteBuf buffer) {
            return new PrepareInstantSwapPayload(
                buffer.readEnum(InstantSwapCompatService.Operation.class),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt()
            );
        }

        public static void handle(PrepareInstantSwapPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                ServerFavoriteService.executeInstantSwap(
                    player,
                    payload.operation(),
                    payload.containerId(),
                    payload.targetMenuSlot(),
                    payload.hotbarIndex(),
                    payload.auxiliaryMenuSlot()
                );
                sendFullSync(player);
            });
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CompleteInstantSwapPayload() implements CustomPacketPayload {
        public static final CompleteInstantSwapPayload INSTANCE = new CompleteInstantSwapPayload();
        public static final Type<CompleteInstantSwapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "complete_instant_swap")
        );
        public static final StreamCodec<FriendlyByteBuf, CompleteInstantSwapPayload> STREAM_CODEC =
            StreamCodec.of(CompleteInstantSwapPayload::write, CompleteInstantSwapPayload::read);

        private static void write(FriendlyByteBuf buffer, CompleteInstantSwapPayload payload) {
        }

        private static CompleteInstantSwapPayload read(FriendlyByteBuf buffer) {
            return INSTANCE;
        }

        public static void handle(CompleteInstantSwapPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                ServerFavoriteService.completeInstantSwap(player);
                sendFullSync(player);
            });
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MoveCreativeFavoritePairsPayload(int[] slotPairs, boolean executeSwap) implements CustomPacketPayload {
        public static final Type<MoveCreativeFavoritePairsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "move_creative_favorite_pairs")
        );
        public static final StreamCodec<FriendlyByteBuf, MoveCreativeFavoritePairsPayload> STREAM_CODEC =
            StreamCodec.of(MoveCreativeFavoritePairsPayload::write, MoveCreativeFavoritePairsPayload::read);

        private static void write(FriendlyByteBuf buffer, MoveCreativeFavoritePairsPayload payload) {
            buffer.writeVarInt(payload.slotPairs.length);
            for (int slot : payload.slotPairs) {
                buffer.writeVarInt(slot);
            }
            buffer.writeBoolean(payload.executeSwap);
        }

        private static MoveCreativeFavoritePairsPayload read(FriendlyByteBuf buffer) {
            int length = buffer.readVarInt();
            if (length < 0 || length > 18) {
                return new MoveCreativeFavoritePairsPayload(new int[0], false);
            }
            int[] slots = new int[length];
            for (int i = 0; i < length; i++) {
                slots[i] = buffer.readVarInt();
            }
            return new MoveCreativeFavoritePairsPayload(slots, buffer.readBoolean());
        }

        public static void handle(MoveCreativeFavoritePairsPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                if (payload.executeSwap()) {
                    ServerFavoriteService.executeCreativeSwapPairs(player, payload.slotPairs());
                } else {
                    ServerFavoriteService.moveCreativeFavoritePairs(player, payload.slotPairs());
                }
                sendFullSync(player);
            });
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

    private static void sendIncrementalSync(ServerPlayer player, SyncFavoriteChangesPayload payload) {
        if (!canSendTo(player, SyncFavoriteChangesPayload.TYPE.id())) {
            DebugLogger.debug("NeoForge incremental sync skipped: client_channel_unavailable player={}", player.getGameProfile().getName());
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static boolean canSendTo(ServerPlayer player, ResourceLocation payloadId) {
        return player != null
            && NetworkRegistry.hasChannel(player.connection.getConnection(), ConnectionProtocol.PLAY, payloadId);
    }

    public record RequestServerConfigPayload() implements CustomPacketPayload {
        public static final RequestServerConfigPayload INSTANCE = new RequestServerConfigPayload();
        public static final Type<RequestServerConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "request_server_config")
        );
        public static final StreamCodec<FriendlyByteBuf, RequestServerConfigPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {}, buffer -> INSTANCE);
        public static void handle(RequestServerConfigPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> sendServerConfigStatus((ServerPlayer) context.player()));
        }
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
        public static void handle(UpdateServerConfigPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                if (player.hasPermissions(2)) {
                    payload.rules.applyTo(ConfigManager.getInstance().getConfig());
                    ConfigManager.getInstance().saveServerConfig();
                }
                sendServerConfigStatus(player);
            });
        }
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
        public static void handle(ServerConfigStatusPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ServerConfigAccess.receive(true, payload.allowed, payload.rules));
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void sendServerConfigStatus(ServerPlayer player) {
        if (!canSendTo(player, ServerConfigStatusPayload.TYPE.id())) return;
        boolean allowed = player.hasPermissions(2);
        PacketDistributor.sendToPlayer(player, new ServerConfigStatusPayload(
            allowed, allowed ? ServerRulesSnapshot.from(ConfigManager.getInstance().getConfig()) : null
        ));
    }
}
