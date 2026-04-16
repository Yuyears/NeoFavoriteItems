
package mycraft.yuyears.newitemfavorites.neoforge.render;

import mycraft.yuyears.newitemfavorites.NewItemFavoritesConfig;
import mycraft.yuyears.newitemfavorites.render.OverlayRenderer;
import mycraft.yuyears.newitemfavorites.neoforge.NewItemFavoritesNeoForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class NeoForgeOverlayRenderer extends OverlayRenderer {
    public NeoForgeOverlayRenderer() {
        // 注册事件处理
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            renderContainerScreenOverlays((AbstractContainerScreen<?>) event.getScreen(), event.getDrawContext(), event.getMouseX(), event.getMouseY(), event.getPartialTicks());
        }
    }

    private void renderContainerScreenOverlays(AbstractContainerScreen<?> screen, DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isHoldingBypassKey = NewItemFavoritesNeoForge.isBypassKeyHeld();
        
        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem() || shouldRenderOverlay(slot.index)) {
                int x = slot.x + screen.getGuiLeft();
                int y = slot.y + screen.getGuiTop();
                renderSlotOverlay(context, x, y, slot.index, isHoldingBypassKey);
            }
        }
    }

    @Override
    public void renderSlotOverlay(DrawContext context, int x, int y, int slotIndex, boolean isHoldingBypassKey) {
        if (!shouldRenderOverlay(slotIndex)) {
            return;
        }

        NewItemFavoritesConfig.OverlayStyle style = getOverlayStyle(slotIndex, isHoldingBypassKey);
        int color = getOverlayColor();
        float opacity = getOverlayOpacity();

        switch (style) {
            case LOCK_ICON -> renderLockIcon(context, x, y, opacity);
            case STAR -> renderStarIcon(context, x, y, opacity);
            case CHECKMARK -> renderCheckmarkIcon(context, x, y, opacity);
            case BORDER_GLOW -> renderBorderGlow(context, x, y, color, opacity);
            case COLOR_OVERLAY -> renderColorOverlay(context, x, y, color, opacity);
        }
    }

    @Override
    public void renderTooltipOverlay(DrawContext context, int x, int y, String text) {
        context.drawTooltip(client.font, text, x, y);
    }

    private void renderLockIcon(DrawContext context, int x, int y, float opacity) {
        context.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        context.drawTexture(LOCK_ICON, x + 12, y + 12, 0, 0, 8, 8, 8, 8);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderStarIcon(DrawContext context, int x, int y, float opacity) {
        context.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        context.drawTexture(STAR_ICON, x + 12, y + 12, 0, 0, 8, 8, 8, 8);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderCheckmarkIcon(DrawContext context, int x, int y, float opacity) {
        context.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        context.drawTexture(CHECKMARK_ICON, x + 12, y + 12, 0, 0, 8, 8, 8, 8);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderBorderGlow(DrawContext context, int x, int y, int color, float opacity) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        context.setShaderColor(r, g, b, opacity);
        context.fill(x - 1, y - 1, x + 17, y + 17, 0x80FFFFFF);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderColorOverlay(DrawContext context, int x, int y, int color, float opacity) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        context.setShaderColor(r, g, b, opacity * 0.3f);
        context.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
