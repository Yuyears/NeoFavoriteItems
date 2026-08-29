package mycraft.yuyears.neofavoriteitems.render.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import mycraft.yuyears.neofavoriteitems.render.OverlayLayerType;
import mycraft.yuyears.neofavoriteitems.render.OverlayProfile;
import mycraft.yuyears.neofavoriteitems.render.OverlayColorMode;
import mycraft.yuyears.neofavoriteitems.render.SlotRenderTarget;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderPhase;
import mycraft.yuyears.neofavoriteitems.render.OverlayZIndex;
import mycraft.yuyears.neofavoriteitems.render.TextureRegistry;
import mycraft.yuyears.neofavoriteitems.render.TintedTextureRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class OverlayDrawEngine {
    // Business zIndex is local to NFI. Every above-item layer shares one GUI depth below tooltips.
    private static final float ABOVE_ITEM_DEPTH = 320.0f;

    private final TextureRegistry textureRegistry;
    private final OverlayFrameCollector collector;
    private final TintedTextureRegistry tintedTextureRegistry = new TintedTextureRegistry();

    public OverlayDrawEngine() {
        this(new TextureRegistry());
    }

    public OverlayDrawEngine(TextureRegistry textureRegistry) {
        this.textureRegistry = textureRegistry;
        this.collector = new OverlayFrameCollector();
    }

    public void beginFrame() {
        collector.clear();
    }

    public void submit(SlotRenderTarget target, OverlayProfile profile) {
        collector.submit(target, profile);
    }

    public void submit(SlotRenderTarget target, OverlayProfile profile, OverlayRenderPhase phase) {
        collector.submit(target, profile, phase);
    }

    public void renderOne(GuiGraphics graphics, SlotRenderTarget target, OverlayProfile profile) {
        beginFrame();
        submit(target, profile);
        render(graphics);
    }

    public void renderOne(GuiGraphics graphics, SlotRenderTarget target, OverlayProfile profile, OverlayRenderPhase phase) {
        beginFrame();
        submit(target, profile, phase);
        render(graphics);
    }

    public void render(GuiGraphics graphics) {
        for (OverlayDrawCommand command : collector.sortedCommands()) {
            renderCommand(graphics, command);
        }
        collector.clear();
    }

    public int pendingCommandCount() {
        return collector.size();
    }

    public void clearTextureCache() {
        textureRegistry.clear();
        tintedTextureRegistry.clear();
    }

    private void renderCommand(GuiGraphics graphics, OverlayDrawCommand command) {
        graphics.pose().pushPose();
        if (command.zIndex() != 0) {
            graphics.pose().translate(0.0f, 0.0f, renderDepth(command.zIndex()));
        }
        if (command.rotationDegrees() != 0.0f) {
            float centerX = command.x() + command.width() / 2.0f;
            float centerY = command.y() + command.height() / 2.0f;
            graphics.pose().translate(centerX, centerY, 0.0f);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(command.rotationDegrees()));
            graphics.pose().translate(-centerX, -centerY, 0.0f);
        }
        try {
            if (command.clipToSlot()) {
                graphics.enableScissor(
                    command.clipX(),
                    command.clipY(),
                    command.clipX() + command.clipWidth(),
                    command.clipY() + command.clipHeight()
                );
            }
            switch (command.type()) {
                case FILL -> graphics.fill(
                    command.x(),
                    command.y(),
                    command.x() + command.width(),
                    command.y() + command.height(),
                    applyOpacity(command.color(), command.opacity())
                );
                case BORDER -> renderBorder(graphics, command);
                case TEXTURE -> renderTexture(graphics, command);
            }
        } finally {
            if (command.clipToSlot()) {
                graphics.disableScissor();
            }
            graphics.pose().popPose();
            resetShaderColor();
        }
    }

    private void renderBorder(GuiGraphics graphics, OverlayDrawCommand command) {
        int color = applyOpacity(command.color(), command.opacity());
        graphics.fill(command.x(), command.y(), command.x() + command.width(), command.y() + 1, color);
        graphics.fill(command.x(), command.y() + command.height() - 1, command.x() + command.width(), command.y() + command.height(), color);
        graphics.fill(command.x(), command.y() + 1, command.x() + 1, command.y() + command.height() - 1, color);
        graphics.fill(command.x() + command.width() - 1, command.y() + 1, command.x() + command.width(), command.y() + command.height() - 1, color);
    }

    private void renderTexture(GuiGraphics graphics, OverlayDrawCommand command) {
        ResourceLocation sourceTexture = command.texture();
        if (sourceTexture == null) {
            return;
        }
        TextureRegistry.TextureMetadata size = textureRegistry.get(sourceTexture);
        boolean generatedMode = command.colorMode() == OverlayColorMode.TINT
            || command.colorMode() == OverlayColorMode.GRAYSCALE
            || command.colorMode() == OverlayColorMode.REPLACE;
        ResourceLocation texture = generatedMode
            ? tintedTextureRegistry.resolve(sourceTexture, command.color(), command.colorMode())
            : sourceTexture;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(
            isNative(command.colorMode()) || command.colorMode() == OverlayColorMode.TINT ? 1.0f : colorRed(command.color()),
            isNative(command.colorMode()) || command.colorMode() == OverlayColorMode.TINT ? 1.0f : colorGreen(command.color()),
            isNative(command.colorMode()) || command.colorMode() == OverlayColorMode.TINT ? 1.0f : colorBlue(command.color()),
            command.opacity() * colorAlpha(command.color())
        );
        graphics.blit(
            texture,
            command.x(),
            command.y(),
            command.width(),
            command.height(),
            0.0f,
            0.0f,
            size.width(),
            size.height(),
            size.width(),
            size.height()
        );
    }

    private static int applyOpacity(int color, float opacity) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, opacity)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static float colorRed(int color) {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    private static float colorGreen(int color) {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    private static float colorBlue(int color) {
        return (color & 0xFF) / 255.0f;
    }

    private static float colorAlpha(int color) {
        return ((color >>> 24) & 0xFF) / 255.0f;
    }

    private static void resetShaderColor() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static boolean isNative(OverlayColorMode mode) {
        // GRAYSCALE requires a shader; keep native output on backends without one.
        return mode == OverlayColorMode.NATIVE || mode == OverlayColorMode.ORIGINAL
            || mode == OverlayColorMode.GRAYSCALE;
    }

    static float renderDepth(int zIndex) {
        return zIndex >= OverlayZIndex.ABOVE_ITEM ? ABOVE_ITEM_DEPTH : 0.0f;
    }
}
