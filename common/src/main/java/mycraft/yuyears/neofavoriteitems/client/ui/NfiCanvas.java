package mycraft.yuyears.neofavoriteitems.client.ui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.GuiGraphics;

/** Reusable programmatic drawing surface for control content. */
public final class NfiCanvas {
    private static final Map<String, NfiCanvas> CACHE = new ConcurrentHashMap<>();

    private final Painter painter;

    private NfiCanvas(Painter painter) {
        this.painter = painter;
    }

    public static NfiCanvas cached(String id, Painter painter) {
        return CACHE.computeIfAbsent(id, ignored -> new NfiCanvas(painter));
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width > 0 && height > 0) painter.render(graphics, x, y, width, height, color);
    }

    @FunctionalInterface
    public interface Painter {
        void render(GuiGraphics graphics, int x, int y, int width, int height, int color);
    }
}
