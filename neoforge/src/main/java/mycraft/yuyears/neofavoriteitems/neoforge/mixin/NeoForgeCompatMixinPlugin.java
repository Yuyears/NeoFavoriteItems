package mycraft.yuyears.neofavoriteitems.neoforge.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/**
 * Mixin plugin to conditionally apply compatibility mixins based on mod availability.
 */
public class NeoForgeCompatMixinPlugin implements IMixinConfigPlugin {
    
    private static final String SOPHISTICATED_SORTER_MOD_ID = "sophisticatedsorter";
    private static final String SOPHISTICATED_CORE_MOD_ID = "sophisticatedcore";
    
    @Override
    public void onLoad(String mixinPackage) {
        // Initialization if needed
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Check if SophisticatedSorterCoreUtilsMixin should be applied
        if (mixinClassName.endsWith("SophisticatedSorterCoreUtilsMixin")) {
            return isModLoaded(SOPHISTICATED_SORTER_MOD_ID);
        }
        
        // Check if SophisticatedCoreInventoryHelperMixin should be applied
        if (mixinClassName.endsWith("SophisticatedCoreInventoryHelperMixin")) {
            return isModLoaded(SOPHISTICATED_CORE_MOD_ID);
        }

        if (mixinClassName.endsWith("MouseTweaksMainMixin")) {
            return true;
        }
        
        // Apply other mixins by default
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // Not needed
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Not needed
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Not needed
    }
    
    /**
     * Check if a mod is loaded using NeoForge's ModList.
     */
    private boolean isModLoaded(String modId) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            return (Boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (Exception e) {
            // If we can't check, assume the mod is not loaded
            return false;
        }
    }

}
