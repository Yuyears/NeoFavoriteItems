package mycraft.yuyears.neofavoriteitems.render;

import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;

/** Supplies complete HUD slot geometry, including slots that contain ItemStack.EMPTY. */
@FunctionalInterface
public interface HudSlotLayoutProvider {
    boolean collect(Player player, int guiWidth, int guiHeight, Consumer<SlotRenderTarget> sink);
}
