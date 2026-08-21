package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.FavoritesManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.application.InstantSwapCompatService;
import mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeFavoriteNetworking;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(targets = "com.susinstantswap.client.SwapEngine", remap = false)
public abstract class SusInstantSwapEngineCompatMixin {
    @Unique
    private static final ThreadLocal<InstantSwapContext> neoFavoriteItems$activeSwap = new ThreadLocal<>();

    @Unique
    private static Method neoFavoriteItems$rowSlotIndexMethod;

    @Unique
    private static final ThreadLocal<CreativeRowContext> neoFavoriteItems$creativeRow = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<CreativeSingleContext> neoFavoriteItems$creativeSingle = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<InstantSwapCompatService.ModifierMode> neoFavoriteItems$rowModifier = new ThreadLocal<>();

    @Unique
    private static Field neoFavoriteItems$closePendingTicksField;

    @Inject(method = "performRowSwap", at = @At("HEAD"))
    private static void neoFavoriteItems$beginRowSwap(
        Minecraft minecraft,
        AbstractContainerScreen<?> screen,
        @Coerce Object config,
        CallbackInfoReturnable<Boolean> cir
    ) {
        neoFavoriteItems$rowModifier.set(NeoFavoriteItemsNeoForge.instantSwapModifierMode());
    }

    @Inject(method = "performRowSwap", at = @At("RETURN"))
    private static void neoFavoriteItems$finishRowSwap(
        Minecraft minecraft,
        AbstractContainerScreen<?> screen,
        @Coerce Object config,
        CallbackInfoReturnable<Boolean> cir
    ) {
        InstantSwapCompatService.ModifierMode modifierMode = neoFavoriteItems$rowModifier.get();
        neoFavoriteItems$rowModifier.remove();
        if (modifierMode == InstantSwapCompatService.ModifierMode.MOVE_LOCKS
            && cir.getReturnValueZ()
            && !(screen instanceof CreativeModeInventoryScreen)
            && NeoForgeFavoriteNetworking.isServerPresent()) {
            NeoForgeFavoriteNetworking.completeInstantSwap();
        }
    }

    @Inject(method = "creativeSwap", at = @At("HEAD"), cancellable = true)
    private static void neoFavoriteItems$guardCreativeSwap(
        Minecraft minecraft,
        CreativeModeInventoryScreen screen,
        int hotbarIndex,
        @Coerce Object config,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Slot targetSlot = screen.getSlotUnderMouse();
        int targetIndex = NeoForgeSlotResolver.getPlayerInventoryIndex(targetSlot);
        InstantSwapCompatService.ModifierMode modifierMode = neoFavoriteItems$modifierMode();
        if (modifierMode == InstantSwapCompatService.ModifierMode.MOVE_LOCKS
            && targetIndex >= 9 && targetIndex < 36
            && NeoForgeFavoriteNetworking.isServerPresent()) {
            NeoForgeFavoriteNetworking.executeCreativeSwapPairs(new int[]{targetIndex, hotbarIndex});
            neoFavoriteItems$scheduleScreenClose();
            cir.setReturnValue(true);
            return;
        }
        if (targetIndex >= 0 && modifierMode == InstantSwapCompatService.ModifierMode.MOVE_LOCKS) {
            FavoritesManager.getStateService().useClientState();
            neoFavoriteItems$creativeSingle.set(new CreativeSingleContext(
                targetIndex,
                hotbarIndex,
                FavoritesManager.getStateService().getFavoriteSlots()
            ));
        }
        boolean blocked = targetSlot != null
            && modifierMode == InstantSwapCompatService.ModifierMode.NONE
            && (neoFavoriteItems$isLocked(hotbarIndex) || neoFavoriteItems$isLocked(targetIndex));
        if (blocked) {
            DebugLogger.debug(
                "Blocked Su's Instant Swap creative single-slot swap: target={} hotbar={}",
                NeoForgeSlotResolver.getPlayerInventoryIndex(targetSlot),
                hotbarIndex
            );
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "creativeSwap", at = @At("RETURN"))
    private static void neoFavoriteItems$finishCreativeSwap(
        Minecraft minecraft,
        CreativeModeInventoryScreen screen,
        int hotbarIndex,
        @Coerce Object config,
        CallbackInfoReturnable<Boolean> cir
    ) {
        CreativeSingleContext context = neoFavoriteItems$creativeSingle.get();
        neoFavoriteItems$creativeSingle.remove();
        if (context != null && cir.getReturnValueZ()) {
            neoFavoriteItems$moveCreativeFavoritePairs(
                context.beforeFavorites(),
                context.targetIndex(),
                context.hotbarIndex()
            );
        }
    }

    @Inject(method = "creativeRowSwap", at = @At("HEAD"), cancellable = true)
    private static void neoFavoriteItems$beginCreativeRowSwap(
        Minecraft minecraft,
        CreativeModeInventoryScreen screen,
        int row,
        int selectedHotbarIndex,
        @Coerce Object config,
        CallbackInfoReturnable<Boolean> cir
    ) {
        InstantSwapCompatService.ModifierMode modifierMode = neoFavoriteItems$modifierMode();
        if (modifierMode == InstantSwapCompatService.ModifierMode.BYPASS) {
            return;
        }
        if (modifierMode == InstantSwapCompatService.ModifierMode.MOVE_LOCKS
            && NeoForgeFavoriteNetworking.isServerPresent()) {
            int[] pairs = neoFavoriteItems$creativeRowPairs(screen, row);
            if (InstantSwapCompatService.areHotbarMainInventoryPairs(pairs)) {
                NeoForgeFavoriteNetworking.executeCreativeSwapPairs(pairs);
                neoFavoriteItems$scheduleScreenClose();
                cir.setReturnValue(true);
                return;
            }
        }
        FavoritesManager.getStateService().useClientState();
        neoFavoriteItems$creativeRow.set(new CreativeRowContext(
            screen,
            row,
            modifierMode == InstantSwapCompatService.ModifierMode.MOVE_LOCKS,
            FavoritesManager.getStateService().getFavoriteSlots(),
            new ArrayList<>()
        ));
    }

    @Redirect(
        method = "creativeRowSwap",
        at = @At(value = "INVOKE", target = "Lcom/susinstantswap/client/SwapEngine;safeSet(Lnet/minecraft/client/Minecraft;ILnet/minecraft/world/item/ItemStack;)V", ordinal = 0)
    )
    private static void neoFavoriteItems$setCreativeRowHotbar(Minecraft minecraft, int hotbarIndex, ItemStack stack) {
        CreativeRowContext context = neoFavoriteItems$creativeRow.get();
        if (context == null) {
            neoFavoriteItems$safeSet(minecraft, hotbarIndex, stack);
            return;
        }
        int menuSlot = neoFavoriteItems$rowSlotIndex(context.row(), hotbarIndex);
        int targetIndex = menuSlot >= 0 && menuSlot < context.screen().getMenu().slots.size()
            ? NeoForgeSlotResolver.getPlayerInventoryIndex(context.screen().getMenu().getSlot(menuSlot))
            : -1;
        context.beginPair(targetIndex, hotbarIndex);
        if (!context.skipCurrentPair()) {
            neoFavoriteItems$safeSet(minecraft, hotbarIndex, stack);
        }
    }

    @Redirect(
        method = "creativeRowSwap",
        at = @At(value = "INVOKE", target = "Lcom/susinstantswap/client/SwapEngine;safeSet(Lnet/minecraft/client/Minecraft;ILnet/minecraft/world/item/ItemStack;)V", ordinal = 1)
    )
    private static void neoFavoriteItems$setCreativeRowTarget(Minecraft minecraft, int targetIndex, ItemStack stack) {
        CreativeRowContext context = neoFavoriteItems$creativeRow.get();
        if (context == null || !context.skipCurrentPair()) {
            neoFavoriteItems$safeSet(minecraft, targetIndex, stack);
        }
    }

    @Redirect(
        method = "creativeRowSwap",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleCreativeModeItemAdd(Lnet/minecraft/world/item/ItemStack;I)V")
    )
    private static void neoFavoriteItems$sendCreativeRowSlot(
        MultiPlayerGameMode gameMode,
        ItemStack stack,
        int slot
    ) {
        CreativeRowContext context = neoFavoriteItems$creativeRow.get();
        if (context == null || !context.skipCurrentPair()) {
            gameMode.handleCreativeModeItemAdd(stack, slot);
        }
    }

    @Inject(method = "creativeRowSwap", at = @At("RETURN"))
    private static void neoFavoriteItems$finishCreativeRowSwap(
        Minecraft minecraft,
        CreativeModeInventoryScreen screen,
        int row,
        int selectedHotbarIndex,
        @Coerce Object config,
        CallbackInfoReturnable<Boolean> cir
    ) {
        CreativeRowContext context = neoFavoriteItems$creativeRow.get();
        neoFavoriteItems$creativeRow.remove();
        if (context == null) {
            return;
        }
        if (context.lockKeyHeld() && cir.getReturnValueZ() && !context.pairs().isEmpty()) {
            neoFavoriteItems$moveCreativeFavoritePairs(
                context.beforeFavorites(),
                context.pairs().stream().mapToInt(Integer::intValue).toArray()
            );
        }
    }

    @Inject(
        method = "containerSwap(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;II)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void neoFavoriteItems$guardContainerSwap(
        AbstractContainerScreen<?> screen,
        int slotIndex,
        int hotbarIndex,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) {
            return;
        }
        InstantSwapCompatService.Decision decision = neoFavoriteItems$beginSwap(
            screen,
            screen.getMenu().getSlot(slotIndex),
            hotbarIndex,
            -1,
            InstantSwapCompatService.Operation.SWAP
        );
        if (decision == InstantSwapCompatService.Decision.SERVER_HANDLED) {
            cir.setReturnValue(true);
        } else if (decision == InstantSwapCompatService.Decision.BLOCK) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "containerSwap(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;II)Z",
        at = @At("RETURN")
    )
    private static void neoFavoriteItems$finishContainerSwap(
        AbstractContainerScreen<?> screen,
        int slotIndex,
        int hotbarIndex,
        CallbackInfoReturnable<Boolean> cir
    ) {
        neoFavoriteItems$finishSwap(cir.getReturnValueZ());
    }

    @Inject(
        method = "performPickupExchange(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void neoFavoriteItems$guardPickupExchange(
        Minecraft minecraft,
        AbstractContainerScreen<?> screen,
        Slot targetSlot,
        int hotbarIndex,
        CallbackInfo ci
    ) {
        InstantSwapCompatService.Decision decision = neoFavoriteItems$beginSwap(
            screen,
            targetSlot,
            hotbarIndex,
            -1,
            InstantSwapCompatService.Operation.PICKUP_EXCHANGE
        );
        if (decision == InstantSwapCompatService.Decision.BLOCK
            || decision == InstantSwapCompatService.Decision.SERVER_HANDLED) {
            ci.cancel();
        }
    }

    @Inject(
        method = "performPickupExchange(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;I)V",
        at = @At("RETURN")
    )
    private static void neoFavoriteItems$finishPickupExchange(
        Minecraft minecraft,
        AbstractContainerScreen<?> screen,
        Slot targetSlot,
        int hotbarIndex,
        CallbackInfo ci
    ) {
        neoFavoriteItems$finishSwap(true);
    }

    @Inject(
        method = "performHotbarStashThenPickup(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;II)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void neoFavoriteItems$guardHotbarPrioritySwap(
        Minecraft minecraft,
        AbstractContainerScreen<?> screen,
        Slot targetSlot,
        int hotbarIndex,
        int emptyMenuIndex,
        CallbackInfo ci
    ) {
        InstantSwapCompatService.Decision decision = neoFavoriteItems$beginSwap(
            screen,
            targetSlot,
            hotbarIndex,
            emptyMenuIndex,
            InstantSwapCompatService.Operation.HOTBAR_STASH
        );
        if (decision == InstantSwapCompatService.Decision.BLOCK
            || decision == InstantSwapCompatService.Decision.SERVER_HANDLED) {
            ci.cancel();
        }
    }

    @Inject(
        method = "performHotbarStashThenPickup(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;II)V",
        at = @At("RETURN")
    )
    private static void neoFavoriteItems$finishHotbarPrioritySwap(
        Minecraft minecraft,
        AbstractContainerScreen<?> screen,
        Slot targetSlot,
        int hotbarIndex,
        int emptyMenuIndex,
        CallbackInfo ci
    ) {
        neoFavoriteItems$finishSwap(true);
    }

    @Unique
    private static InstantSwapCompatService.Decision neoFavoriteItems$beginSwap(
        AbstractContainerScreen<?> screen,
        Slot targetSlot,
        int hotbarIndex,
        int auxiliaryMenuSlot,
        InstantSwapCompatService.Operation operation
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || targetSlot == null) {
            return InstantSwapCompatService.Decision.PASS;
        }

        int targetIndex = NeoForgeSlotResolver.getPlayerInventoryIndex(targetSlot);
        if (targetIndex >= 0 && !InstantSwapCompatService.isHotbarMainInventoryPair(targetIndex, hotbarIndex)) {
            return InstantSwapCompatService.Decision.PASS;
        }

        int[] favoriteCycle;
        if (operation == InstantSwapCompatService.Operation.HOTBAR_STASH) {
            if (auxiliaryMenuSlot < 0 || auxiliaryMenuSlot >= screen.getMenu().slots.size()) {
                return InstantSwapCompatService.Decision.PASS;
            }
            Slot emptySlot = screen.getMenu().getSlot(auxiliaryMenuSlot);
            int emptyIndex = NeoForgeSlotResolver.getPlayerInventoryIndex(emptySlot);
            if (emptyIndex < 0 || emptyIndex > 8) {
                return InstantSwapCompatService.Decision.PASS;
            }
            favoriteCycle = new int[]{targetIndex, hotbarIndex, emptyIndex};
        } else {
            favoriteCycle = new int[]{targetIndex, hotbarIndex};
        }

        FavoritesManager.getStateService().useClientState();
        boolean anyLocked = false;
        for (int inventoryIndex : favoriteCycle) {
            anyLocked |= inventoryIndex >= 0 && FavoritesManager.getInstance().isSlotFavorite(inventoryIndex);
        }
        InstantSwapCompatService.ModifierMode modifierMode = neoFavoriteItems$modifierMode();
        if (!anyLocked && modifierMode == InstantSwapCompatService.ModifierMode.MOVE_LOCKS) {
            return InstantSwapCompatService.Decision.PASS;
        }

        InstantSwapCompatService.Decision decision = InstantSwapCompatService.decide(
            anyLocked,
            false,
            modifierMode == InstantSwapCompatService.ModifierMode.BYPASS,
            modifierMode == InstantSwapCompatService.ModifierMode.MOVE_LOCKS
        );
        if (decision != InstantSwapCompatService.Decision.SWAP_WITH_LOCKS) {
            return decision;
        }
        if (operation != InstantSwapCompatService.Operation.SWAP && !screen.getMenu().getCarried().isEmpty()) {
            return InstantSwapCompatService.Decision.BLOCK;
        }

        Slot hotbarSlot = neoFavoriteItems$findPlayerMenuSlot(screen, hotbarIndex);
        if (hotbarSlot == null) {
            return InstantSwapCompatService.Decision.BLOCK;
        }
        List<Slot> contentSlots = operation == InstantSwapCompatService.Operation.HOTBAR_STASH
            ? List.of(targetSlot, hotbarSlot, screen.getMenu().getSlot(auxiliaryMenuSlot))
            : List.of(targetSlot, hotbarSlot);
        List<ItemStack> beforeStacks = contentSlots.stream().map(slot -> slot.getItem().copy()).toList();

        boolean serverPresent = NeoForgeFavoriteNetworking.isServerPresent();
        if (serverPresent) {
            boolean sent = NeoForgeFavoriteNetworking.tryPrepareInstantSwap(
                operation,
                screen.getMenu().containerId,
                targetSlot.index,
                hotbarIndex,
                auxiliaryMenuSlot
            );
            if (sent) {
                neoFavoriteItems$moveClientFavoriteCycle(favoriteCycle);
                return InstantSwapCompatService.Decision.SERVER_HANDLED;
            }
            return InstantSwapCompatService.Decision.BLOCK;
        }

        ServerFavoriteService.beginInventoryGuardBypass(minecraft.player.getUUID());
        neoFavoriteItems$activeSwap.set(new InstantSwapContext(
            minecraft.player.getUUID(),
            favoriteCycle,
            serverPresent,
            contentSlots,
            beforeStacks
        ));
        return decision;
    }

    @Unique
    private static void neoFavoriteItems$finishSwap(boolean performed) {
        InstantSwapContext context = neoFavoriteItems$activeSwap.get();
        if (context == null) {
            return;
        }
        neoFavoriteItems$activeSwap.remove();
        ServerFavoriteService.endInventoryGuardBypass(context.playerId());

        List<ItemStack> afterStacks = context.contentSlots().stream().map(Slot::getItem).toList();
        if (performed && InstantSwapCompatService.contentFollowsCycle(
            context.beforeStacks(),
            afterStacks,
            ItemStack::matches
        )) {
            neoFavoriteItems$moveClientFavoriteCycle(context.favoriteCycle());
        }
        if (context.serverPresent()) {
            NeoForgeFavoriteNetworking.completeInstantSwap();
        }
    }

    @Unique
    private static Slot neoFavoriteItems$findPlayerMenuSlot(AbstractContainerScreen<?> screen, int inventoryIndex) {
        for (Slot slot : screen.getMenu().slots) {
            if (NeoForgeSlotResolver.getPlayerInventoryIndex(slot) == inventoryIndex) {
                return slot;
            }
        }
        return null;
    }

    @Unique
    private static void neoFavoriteItems$moveClientFavoriteCycle(int[] favoriteCycle) {
        FavoritesManager.getStateService().useClientState();
        Set<Integer> movedFavorites = InstantSwapCompatService.moveFavoriteSlots(
            FavoritesManager.getStateService().getFavoriteSlots(),
            favoriteCycle
        );
        for (int inventoryIndex : favoriteCycle) {
            if (inventoryIndex >= 0) {
                FavoritesManager.getInstance().setSlotFavorite(inventoryIndex, movedFavorites.contains(inventoryIndex));
            }
        }
    }

    @Unique
    private static boolean neoFavoriteItems$isLocked(int inventoryIndex) {
        if (inventoryIndex < 0) {
            return false;
        }
        FavoritesManager.getStateService().useClientState();
        return FavoritesManager.getInstance().isSlotFavorite(inventoryIndex);
    }

    @Unique
    private static InstantSwapCompatService.ModifierMode neoFavoriteItems$modifierMode() {
        InstantSwapCompatService.ModifierMode rowModifier = neoFavoriteItems$rowModifier.get();
        return rowModifier != null ? rowModifier : NeoFavoriteItemsNeoForge.instantSwapModifierMode();
    }

    @Unique
    private static void neoFavoriteItems$scheduleScreenClose() {
        try {
            if (neoFavoriteItems$closePendingTicksField == null) {
                Class<?> swapKeyState = Class.forName("com.susinstantswap.client.SwapKeyState");
                neoFavoriteItems$closePendingTicksField = swapKeyState.getField("closePendingTicks");
            }
            neoFavoriteItems$closePendingTicksField.setInt(null, 1);
        } catch (ReflectiveOperationException exception) {
            DebugLogger.warn("Failed to schedule Su's Instant Swap screen close: {}", exception.toString());
        }
    }

    @Unique
    private static int[] neoFavoriteItems$creativeRowPairs(CreativeModeInventoryScreen screen, int row) {
        int[] pairs = new int[18];
        for (int column = 0; column < 9; column++) {
            int menuSlot = neoFavoriteItems$rowSlotIndex(row, column);
            int target = menuSlot >= 0 && menuSlot < screen.getMenu().slots.size()
                ? NeoForgeSlotResolver.getPlayerInventoryIndex(screen.getMenu().getSlot(menuSlot))
                : -1;
            pairs[column * 2] = target;
            pairs[column * 2 + 1] = column;
        }
        return pairs;
    }

    @Unique
    private static void neoFavoriteItems$safeSet(Minecraft minecraft, int inventoryIndex, ItemStack stack) {
        if (minecraft.player != null
            && inventoryIndex >= 0
            && inventoryIndex < minecraft.player.getInventory().items.size()) {
            minecraft.player.getInventory().items.set(inventoryIndex, stack);
        }
    }

    @Unique
    private static void neoFavoriteItems$moveCreativeFavoritePairs(
        Set<Integer> beforeFavorites,
        int... slotPairs
    ) {
        if (!InstantSwapCompatService.areHotbarMainInventoryPairs(slotPairs)) {
            return;
        }
        Set<Integer> movedFavorites = InstantSwapCompatService.moveFavoritePairs(beforeFavorites, slotPairs);
        FavoritesManager.getStateService().useClientState();
        FavoritesManager favoritesManager = FavoritesManager.getInstance();
        for (int inventoryIndex : slotPairs) {
            favoritesManager.setSlotFavorite(inventoryIndex, movedFavorites.contains(inventoryIndex));
        }
        if (NeoForgeFavoriteNetworking.isServerPresent()) {
            NeoForgeFavoriteNetworking.moveCreativeFavoritePairs(slotPairs);
        }
        DebugLogger.debug("Moved Su's Instant Swap creative favorites: pairs={}", java.util.Arrays.toString(slotPairs));
    }

    @Unique
    private static int neoFavoriteItems$rowSlotIndex(int row, int column) {
        try {
            if (neoFavoriteItems$rowSlotIndexMethod == null) {
                Class<?> widget = Class.forName("com.susinstantswap.client.RowArrowWidget");
                neoFavoriteItems$rowSlotIndexMethod = widget.getDeclaredMethod("rowSlotIndex", int.class, int.class);
            }
            return (int) neoFavoriteItems$rowSlotIndexMethod.invoke(null, row, column);
        } catch (ReflectiveOperationException exception) {
            DebugLogger.warn("Failed to resolve Su's Instant Swap row slot: {}", exception.toString());
            return -1;
        }
    }

    @Unique
    private record InstantSwapContext(
        UUID playerId,
        int[] favoriteCycle,
        boolean serverPresent,
        List<Slot> contentSlots,
        List<ItemStack> beforeStacks
    ) {
    }

    @Unique
    private record CreativeSingleContext(
        int targetIndex,
        int hotbarIndex,
        Set<Integer> beforeFavorites
    ) {
    }

    @Unique
    private static final class CreativeRowContext {
        private final CreativeModeInventoryScreen screen;
        private final int row;
        private final boolean lockKeyHeld;
        private final Set<Integer> beforeFavorites;
        private final List<Integer> pairs;
        private boolean skipCurrentPair;

        private CreativeRowContext(
            CreativeModeInventoryScreen screen,
            int row,
            boolean lockKeyHeld,
            Set<Integer> beforeFavorites,
            List<Integer> pairs
        ) {
            this.screen = screen;
            this.row = row;
            this.lockKeyHeld = lockKeyHeld;
            this.beforeFavorites = beforeFavorites;
            this.pairs = pairs;
        }

        private void beginPair(int targetIndex, int hotbarIndex) {
            skipCurrentPair = targetIndex < 0
                || !lockKeyHeld && (beforeFavorites.contains(targetIndex) || beforeFavorites.contains(hotbarIndex));
            if (!skipCurrentPair && lockKeyHeld) {
                pairs.add(targetIndex);
                pairs.add(hotbarIndex);
            }
            if (skipCurrentPair) {
                DebugLogger.debug(
                    "Skipped Su's Instant Swap creative row column: row={} target={} hotbar={}",
                    row,
                    targetIndex,
                    hotbarIndex
                );
            }
        }

        private CreativeModeInventoryScreen screen() {
            return screen;
        }

        private int row() {
            return row;
        }

        private boolean lockKeyHeld() {
            return lockKeyHeld;
        }

        private Set<Integer> beforeFavorites() {
            return beforeFavorites;
        }

        private List<Integer> pairs() {
            return pairs;
        }

        private boolean skipCurrentPair() {
            return skipCurrentPair;
        }
    }
}
