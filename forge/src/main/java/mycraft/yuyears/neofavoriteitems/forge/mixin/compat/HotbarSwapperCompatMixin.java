package mycraft.yuyears.neofavoriteitems.forge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.application.HotbarRowSwapCompatService;
import mycraft.yuyears.neofavoriteitems.forge.ForgeFavoriteNetworking;
import mycraft.yuyears.neofavoriteitems.forge.NeoFavoriteItemsForge;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.ichun.mods.hotbarswapper.common.core.EventHandlerClient", remap = false)
public abstract class HotbarSwapperCompatMixin {
    @Shadow private int currentIndex;
    @Shadow private void doSwap(LocalPlayer player, Inventory inventory, int index) {}

    @Unique private boolean neoFavoriteItems$captureRowSwap;
    @Unique private boolean neoFavoriteItems$serverHandlesRowSwap;
    @Unique private boolean neoFavoriteItems$lockSeenDuringPreview;
    @Unique private int neoFavoriteItems$columnMask;

    @Inject(method = "doSwap(Z)V", at = @At("HEAD"))
    private void neoFavoriteItems$beginRowSwap(boolean isRow, CallbackInfo ci) {
        boolean serverPresent = ForgeFavoriteNetworking.isServerPresent();
        boolean rowSwapSupported = ForgeFavoriteNetworking.canSendHotbarRowSwap();
        neoFavoriteItems$captureRowSwap = isRow
            && currentIndex >= 1
            && currentIndex <= 3
            && (NeoFavoriteItemsForge.isLockOperationKeyHeld() || neoFavoriteItems$lockSeenDuringPreview)
            && (!serverPresent || rowSwapSupported);
        neoFavoriteItems$serverHandlesRowSwap = neoFavoriteItems$captureRowSwap && rowSwapSupported;
        neoFavoriteItems$columnMask = 0;
    }

    @Inject(method = "addToIndex(IZ)Z", at = @At("HEAD"))
    private void neoFavoriteItems$rememberModifier(int addAmount, boolean isRow, CallbackInfoReturnable<Boolean> cir) {
        if (isRow && NeoFavoriteItemsForge.isLockOperationKeyHeld()) {
            neoFavoriteItems$lockSeenDuringPreview = true;
        }
    }

    @Redirect(
        method = "doSwap(Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lme/ichun/mods/hotbarswapper/common/core/EventHandlerClient;doSwap(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/entity/player/Inventory;I)V",
            remap = false
        )
    )
    private void neoFavoriteItems$captureColumn(
        @Coerce Object ignored,
        LocalPlayer player,
        Inventory inventory,
        int column
    ) {
        if (!neoFavoriteItems$captureRowSwap) {
            doSwap(player, inventory, column);
            return;
        }

        neoFavoriteItems$columnMask |= 1 << column;
        if (!neoFavoriteItems$serverHandlesRowSwap) {
            doSwap(player, inventory, column);
        }
    }

    @Inject(method = "doSwap(Z)V", at = @At("TAIL"))
    private void neoFavoriteItems$finishRowSwap(boolean isRow, CallbackInfo ci) {
        if (!neoFavoriteItems$captureRowSwap || neoFavoriteItems$columnMask == 0) {
            return;
        }
        if (neoFavoriteItems$serverHandlesRowSwap) {
            ForgeFavoriteNetworking.sendHotbarRowSwap(currentIndex, neoFavoriteItems$columnMask);
        } else {
            HotbarRowSwapCompatService.moveClientFavoriteState(currentIndex, neoFavoriteItems$columnMask);
        }
        neoFavoriteItems$lockSeenDuringPreview = false;
    }
}
