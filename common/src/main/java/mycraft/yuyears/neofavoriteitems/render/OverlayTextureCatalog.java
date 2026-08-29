package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class OverlayTextureCatalog {
    public static final String NO_MATERIAL = "builtin:none";
    private enum BuiltinMaterial {
        HEART("preset:heart", "heart"),
        ARROW("preset:arrow", "arrow_down");

        private final String id;
        private final String filename;

        BuiltinMaterial(String id, String filename) {
            this.id = id;
            this.filename = filename;
        }
    }

    private OverlayTextureCatalog() {}

    public static List<String> presetIds() {
        List<String> ids = new java.util.ArrayList<>();
        for (NeoFavoriteItemsConfig.OverlayStyle style : NeoFavoriteItemsConfig.OverlayStyle.values()) {
            if (style != NeoFavoriteItemsConfig.OverlayStyle.COLOR_OVERLAY) ids.add(presetId(style));
        }
        for (BuiltinMaterial material : BuiltinMaterial.values()) ids.add(material.id);
        return List.copyOf(ids);
    }

    public static String presetId(NeoFavoriteItemsConfig.OverlayStyle style) {
        return "preset:" + style.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static NeoFavoriteItemsConfig.OverlayStyle presetStyle(String materialId) {
        if (materialId == null || !materialId.startsWith("preset:")) return null;
        try {
            return NeoFavoriteItemsConfig.OverlayStyle.valueOf(
                materialId.substring("preset:".length()).toUpperCase(java.util.Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    static ResourceLocation textureFor(NeoFavoriteItemsConfig.OverlayStyle style, String materialId) {
        if (NO_MATERIAL.equals(materialId)) return null;
        if (materialId != null && materialId.startsWith("custom:")) {
            var manager = mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport.getCustomTextureManager();
            ResourceLocation custom = manager == null ? null : manager.resolve(materialId);
            if (custom != null) return custom;
        }
        for (BuiltinMaterial material : BuiltinMaterial.values()) {
            if (material.id.equals(materialId)) return texture(material.filename);
        }
        NeoFavoriteItemsConfig.OverlayStyle selectedPreset = presetStyle(materialId);
        return textureFor(selectedPreset == null ? style : selectedPreset);
    }

    static ResourceLocation textureFor(NeoFavoriteItemsConfig.OverlayStyle style) {
        String filename = switch (style) {
            case BORDER -> "border";
            case CLASSIC -> "classic";
            case FRAMEWORK -> "framework";
            case HIGHLIGHT -> "highlight";
            case BRACKETS -> "brackets";
            case LOCK -> "lock";
            case MARK -> "mark";
            case TAG -> "tag";
            case STAR -> "star";
            case COLOR_OVERLAY -> "color_overlay";
        };
        return style == NeoFavoriteItemsConfig.OverlayStyle.COLOR_OVERLAY
            ? null
            : texture(filename);
    }

    private static ResourceLocation texture(String filename) {
        return ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "textures/" + filename + ".png");
    }
}
