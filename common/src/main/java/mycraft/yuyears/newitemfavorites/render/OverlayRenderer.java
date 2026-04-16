
package mycraft.yuyears.newitemfavorites.render;

import mycraft.yuyears.newitemfavorites.ConfigManager;
import mycraft.yuyears.newitemfavorites.FavoritesManager;
import mycraft.yuyears.newitemfavorites.NewItemFavoritesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public abstract class OverlayRenderer {
    protected static final Identifier LOCK_ICON = new Identifier("new_item_favorites", "textures/lock.png");
    protected static final Identifier STAR_ICON = new Identifier("new_item_favorites", "textures/star.png");
    protected static final Identifier CHECKMARK_ICON = new Identifier("new_item_favorites", "textures/checkmark.png");
    
    protected final MinecraftClient client;
    protected final FavoritesManager favoritesManager;
    protected final ConfigManager configManager;

    public OverlayRenderer() {
        this.client = MinecraftClient.getInstance();
        this.favoritesManager = FavoritesManager.getInstance();
        this.configManager = ConfigManager.getInstance();
    }

    public abstract void renderSlotOverlay(DrawContext context, int x, int y, int slotIndex, boolean isHoldingBypassKey);

    public abstract void renderTooltipOverlay(DrawContext context, int x, int y, String text);

    protected NewItemFavoritesConfig.OverlayStyle getOverlayStyle(int slotIndex, boolean isHoldingBypassKey) {
        boolean isFavorite = favoritesManager.isSlotFavorite(slotIndex);
        NewItemFavoritesConfig config = configManager.getConfig();
        
        if (isHoldingBypassKey) {
            return isFavorite ? config.overlay.holdingKeyLockedStyle : config.overlay.holdingKeyUnlockedStyle;
        } else {
            return isFavorite ? config.overlay.lockedStyle : config.overlay.unlockedStyle;
        }
    }

    protected int getOverlayColor() {
        return configManager.getConfig().overlay.overlayColor;
    }

    protected float getOverlayOpacity() {
        return configManager.getConfig().overlay.overlayOpacity;
    }

    protected boolean shouldRenderOverlay(int slotIndex) {
        return favoritesManager.isSlotFavorite(slotIndex);
    }
}
