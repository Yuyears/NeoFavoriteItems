
package mycraft.yuyears.newitemfavorites.fabric.render;

import com.mojang.blaze3d.systems.RenderSystem;
import mycraft.yuyears.newitemfavorites.NewItemFavoritesConfig;
import mycraft.yuyears.newitemfavorites.domain.LogicalSlotIndex;
import mycraft.yuyears.newitemfavorites.fabric.NewItemFavoritesFabric;
import mycraft.yuyears.newitemfavorites.integration.SlotMappingService;
import mycraft.yuyears.newitemfavorites.render.OverlayRenderer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FabricOverlayRenderer extends OverlayRenderer {
    private static final ResourceLocation LOCK_ICON = ResourceLocation.fromNamespaceAndPath("new_item_favorites", LOCK_ICON_PATH);
    private static final ResourceLocation STAR_ICON = ResourceLocation.fromNamespaceAndPath("new_item_favorites", STAR_ICON_PATH);
    private static final ResourceLocation CHECKMARK_ICON = ResourceLocation.fromNamespaceAndPath("new_item_favorites", CHECKMARK_ICON_PATH);

    public FabricOverlayRenderer() {
        registerEvents();
    }

    private void registerEvents() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?>) {
                ScreenEvents.afterRender(screen).register((screen1, context, mouseX, mouseY, delta) -> {
                    if (screen1 instanceof AbstractContainerScreen<?>) {
                        renderHandledScreenOverlays((AbstractContainerScreen<?>) screen1, context, mouseX, mouseY, delta);
                    }
                });
            }
        });
    }

    private void renderHandledScreenOverlays(AbstractContainerScreen<?> screen, GuiGraphics context, int mouseX, int mouseY, float delta) {
        boolean isHoldingBypassKey = NewItemFavoritesFabric.isBypassKeyHeld();
        
        for (Slot slot : screen.getMenu().slots) {
            var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot));
            if (logicalSlot.isEmpty()) {
                continue;
            }
            if (slot.hasItem() || shouldRenderOverlay(logicalSlot.get())) {
                int x = slot.x + getScreenLeft(screen);
                int y = slot.y + getScreenTop(screen);
                renderSlotOverlay(context, x, y, logicalSlot.get(), isHoldingBypassKey);
            }
        }
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean isHoldingBypassKey) {
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

    public void renderTooltipOverlay(GuiGraphics context, int x, int y, String text) {
        context.renderTooltip(Minecraft.getInstance().font, net.minecraft.network.chat.Component.literal(text), x, y);
    }

    private void renderLockIcon(GuiGraphics context, int x, int y, float opacity) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        context.blit(LOCK_ICON, x + 12, y + 12, 0, 0, 8, 8, 8, 8);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderStarIcon(GuiGraphics context, int x, int y, float opacity) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        context.blit(STAR_ICON, x + 12, y + 12, 0, 0, 8, 8, 8, 8);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderCheckmarkIcon(GuiGraphics context, int x, int y, float opacity) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        context.blit(CHECKMARK_ICON, x + 12, y + 12, 0, 0, 8, 8, 8, 8);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderBorderGlow(GuiGraphics context, int x, int y, int color, float opacity) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        RenderSystem.setShaderColor(r, g, b, opacity);
        context.fill(x - 1, y - 1, x + 17, y + 17, 0x80FFFFFF);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderColorOverlay(GuiGraphics context, int x, int y, int color, float opacity) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        RenderSystem.setShaderColor(r, g, b, opacity * 0.3f);
        context.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private int getContainerSlotIndex(Slot slot) {
        Integer methodResult = invokeIntMethod(slot, "getContainerSlot");
        if (methodResult != null) {
            return methodResult;
        }

        Integer fieldResult = readIntField(slot, "slot");
        if (fieldResult != null) {
            return fieldResult;
        }

        fieldResult = readIntField(slot, "index");
        return fieldResult == null ? -1 : fieldResult;
    }

    private int getScreenLeft(AbstractContainerScreen<?> screen) {
        Integer value = readIntField(screen, "leftPos");
        return value == null ? 0 : value;
    }

    private int getScreenTop(AbstractContainerScreen<?> screen) {
        Integer value = readIntField(screen, "topPos");
        return value == null ? 0 : value;
    }

    private Integer invokeIntMethod(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return (Integer) method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Integer readIntField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }
}
