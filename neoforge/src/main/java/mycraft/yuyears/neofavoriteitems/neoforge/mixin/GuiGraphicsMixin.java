package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import mycraft.yuyears.neofavoriteitems.neoforge.render.NeoForgeOverlayRenderer;
import mycraft.yuyears.neofavoriteitems.render.HudSlotProbe;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderPhase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"), require = 0)
    private void neoFavoriteItems$renderDirectHudSlotBelow(LivingEntity entity, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (entity instanceof Player player && !HudSlotProbe.insideGuiSlot())
            NeoForgeOverlayRenderer.renderHudSlot((GuiGraphics) (Object) this, player, stack, x, y, OverlayRenderPhase.BELOW_ITEM);
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("RETURN"), require = 0)
    private void neoFavoriteItems$renderDirectHudSlotAbove(LivingEntity entity, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (entity instanceof Player player && !HudSlotProbe.insideGuiSlot())
            NeoForgeOverlayRenderer.renderHudSlot((GuiGraphics) (Object) this, player, stack, x, y, OverlayRenderPhase.ABOVE_ITEM);
    }
}
