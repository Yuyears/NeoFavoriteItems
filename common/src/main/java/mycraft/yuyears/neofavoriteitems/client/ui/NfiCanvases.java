package mycraft.yuyears.neofavoriteitems.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/** Shared canvas drawings. Each shape has one immutable cached instance. */
public final class NfiCanvases {
    private static final int PLAY_SIZE = 9;
    private static final ResourceLocation DROPDOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "neo_favorite_items", "textures/dropdown.png");
    public static final NfiCanvas PLAY = NfiCanvas.cached("play", NfiCanvases::drawPlay);
    public static final NfiCanvas DROPDOWN = NfiCanvas.cached("dropdown", NfiCanvases::drawDropdown);

    private NfiCanvases() {}

    private static void drawPlay(net.minecraft.client.gui.GuiGraphics graphics,
                                 int x, int y, int width, int height, int color) {
        int size = playSize(width, height);
        float left = x + (width - size) / 2.0f;
        float top = y + (height - size) / 2.0f;
        float bottom = top + size;
        float pointX = left + size;
        float pointY = top + size / 2.0f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var matrix = graphics.pose().last().pose();
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, left, top, 0.0f).setColor(color);
        buffer.addVertex(matrix, left, bottom, 0.0f).setColor(color);
        buffer.addVertex(matrix, pointX, pointY, 0.0f).setColor(color);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawDropdown(net.minecraft.client.gui.GuiGraphics graphics,
                                     int x, int y, int width, int height, int color) {
        int size = Math.max(1, Math.min(width, height) - 8);
        graphics.blit(DROPDOWN_TEXTURE, x + (width - size) / 2, y + (height - size) / 2,
            0, 0, size, size, size, size);
    }

    static int playSize(int width, int height) {
        int size = Math.max(3, Math.min(PLAY_SIZE, Math.min(width, height) - 4));
        return (size & 1) == 0 ? size - 1 : size;
    }
}
