package mycraft.yuyears.neofavoriteitems.forge.mixin;

import mycraft.yuyears.neofavoriteitems.forge.render.ForgeOverlayRenderer;
import mycraft.yuyears.neofavoriteitems.render.HudSlotProbe;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderPhase;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void neoFavoriteItems$renderBelowHotbarItem(GuiGraphics graphics, int x, int y,
                                                         DeltaTracker deltaTracker, Player player,
                                                         ItemStack stack, int seed, CallbackInfo ci) {
        HudSlotProbe.enterGuiSlot();
        ForgeOverlayRenderer.renderHudSlot(graphics, player, stack, x, y, OverlayRenderPhase.BELOW_ITEM);
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void neoFavoriteItems$renderAboveHotbarItem(GuiGraphics graphics, int x, int y,
                                                         DeltaTracker deltaTracker, Player player,
                                                         ItemStack stack, int seed, CallbackInfo ci) {
        ForgeOverlayRenderer.renderHudSlot(graphics, player, stack, x, y, OverlayRenderPhase.ABOVE_ITEM);
        HudSlotProbe.exitGuiSlot();
    }
}
