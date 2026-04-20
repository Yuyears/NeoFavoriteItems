package mycraft.yuyears.newitemfavorites.fabric.mixin;

import mycraft.yuyears.newitemfavorites.application.InteractionGuardService;
import mycraft.yuyears.newitemfavorites.domain.InteractionType;
import mycraft.yuyears.newitemfavorites.fabric.NewItemFavoritesFabric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void newItemFavorites$guardFavoriteSlot(Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci) {
        if (slot == null || !isPlayerInventorySlot(slot)) {
            return;
        }

        var decision = InteractionGuardService.getInstance().evaluate(
            slot.getContainerSlot(),
            toInteractionType(clickType),
            NewItemFavoritesFabric.isBypassKeyHeld()
        );
        if (decision.denied()) {
            ci.cancel();
        }
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        var player = Minecraft.getInstance().player;
        return player != null && slot.container == player.getInventory();
    }

    private InteractionType toInteractionType(ClickType clickType) {
        return switch (clickType) {
            case PICKUP, PICKUP_ALL -> InteractionType.CLICK;
            case QUICK_MOVE -> InteractionType.QUICK_MOVE;
            case SWAP -> InteractionType.SWAP;
            case THROW -> InteractionType.DROP;
            case QUICK_CRAFT -> InteractionType.DRAG;
            case CLONE -> InteractionType.UNKNOWN;
        };
    }
}
