package mycraft.yuyears.neofavoriteitems.render.pipeline;

import mycraft.yuyears.neofavoriteitems.render.OverlayLayerType;
import mycraft.yuyears.neofavoriteitems.render.OverlayColorMode;
import net.minecraft.resources.ResourceLocation;

public record OverlayDrawCommand(
    OverlayLayerType type,
    int zIndex,
    int x,
    int y,
    int width,
    int height,
    float rotationDegrees,
    ResourceLocation texture,
    int color,
    float opacity,
    OverlayColorMode colorMode,
    boolean clipToSlot,
    int clipX,
    int clipY,
    int clipWidth,
    int clipHeight
) {}
