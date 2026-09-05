package mycraft.yuyears.neofavoriteitems.render;

import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class OverlayTextureCatalog {
    public static final String NO_MATERIAL = "builtin:none";
    private enum BuiltinMaterial {
        BORDER("border", NeoFavoriteItemsConfig.OverlayStyle.BORDER),
        BRACKETS("brackets", NeoFavoriteItemsConfig.OverlayStyle.BRACKETS),
        CLASSIC("classic", NeoFavoriteItemsConfig.OverlayStyle.CLASSIC),
        FRAMEWORK("framework", NeoFavoriteItemsConfig.OverlayStyle.FRAMEWORK),
        HEART_0("heart_0", null),
        HEART_1("heart_1", null),
        HEART_2("heart_2", null),
        HEART("heart", null),
        HIGHLIGHT("highlight", NeoFavoriteItemsConfig.OverlayStyle.HIGHLIGHT),
        LOCK_0("lock_0", null),
        LOCK_1("lock_1", null),
        LOCK_2("lock_2", null),
        LOCK("lock", NeoFavoriteItemsConfig.OverlayStyle.LOCK),
        LOCK_FLAT("lock_flat", null),
        MARK("mark", NeoFavoriteItemsConfig.OverlayStyle.MARK),
        STAR("star", NeoFavoriteItemsConfig.OverlayStyle.STAR),
        TAG("tag", NeoFavoriteItemsConfig.OverlayStyle.TAG),
        ARROW_DOWN("arrow_down", null);

        private final String id;
        private final String filename;
        private final NeoFavoriteItemsConfig.OverlayStyle legacyStyle;

        BuiltinMaterial(String filename, NeoFavoriteItemsConfig.OverlayStyle legacyStyle) {
            this.id = "preset:" + filename;
            this.filename = filename;
            this.legacyStyle = legacyStyle;
        }

        private static BuiltinMaterial fromId(String id) {
            if ("preset:arrow".equals(id)) return ARROW_DOWN;
            for (BuiltinMaterial material : values()) {
                if (material.id.equals(id)) return material;
            }
            return null;
        }

        private static BuiltinMaterial fromLegacyStyle(NeoFavoriteItemsConfig.OverlayStyle style) {
            for (BuiltinMaterial material : values()) {
                if (material.legacyStyle == style) return material;
            }
            return null;
        }
    }

    private OverlayTextureCatalog() {}

    public static List<String> presetIds() {
        List<String> ids = new java.util.ArrayList<>();
        for (BuiltinMaterial material : BuiltinMaterial.values()) ids.add(material.id);
        return List.copyOf(ids);
    }

    public static String presetId(NeoFavoriteItemsConfig.OverlayStyle style) {
        BuiltinMaterial material = BuiltinMaterial.fromLegacyStyle(style);
        return material == null ? "preset:" + style.name().toLowerCase(java.util.Locale.ROOT) : material.id;
    }

    public static NeoFavoriteItemsConfig.OverlayStyle presetStyle(String materialId) {
        BuiltinMaterial material = BuiltinMaterial.fromId(materialId);
        return material == null ? null : material.legacyStyle;
    }

    public static String canonicalPresetId(String materialId) {
        BuiltinMaterial material = BuiltinMaterial.fromId(materialId);
        return material == null ? materialId : material.id;
    }

    static ResourceLocation textureFor(NeoFavoriteItemsConfig.OverlayStyle style, String materialId) {
        if (NO_MATERIAL.equals(materialId)) return null;
        if (materialId != null && materialId.startsWith("custom:")) {
            var manager = mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport.getCustomTextureManager();
            ResourceLocation custom = manager == null ? null : manager.resolve(materialId);
            if (custom != null) return custom;
        }
        BuiltinMaterial material = BuiltinMaterial.fromId(materialId);
        if (material != null) return texture(material.filename);
        NeoFavoriteItemsConfig.OverlayStyle selectedPreset = presetStyle(materialId);
        return textureFor(selectedPreset == null ? style : selectedPreset);
    }

    static ResourceLocation textureFor(NeoFavoriteItemsConfig.OverlayStyle style) {
        BuiltinMaterial material = BuiltinMaterial.fromLegacyStyle(style);
        return material == null ? null : texture(material.filename);
    }

    private static ResourceLocation texture(String filename) {
        return ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "textures/" + filename + ".png");
    }
}
