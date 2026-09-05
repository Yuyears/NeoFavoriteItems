package mycraft.yuyears.neofavoriteitems.forge.mixin.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/**
 * Mixin plugin to conditionally apply compatibility mixins based on mod availability.
 */
public class ForgeCompatMixinPlugin implements IMixinConfigPlugin {
    
    private static final String SOPHISTICATED_SORTER_MOD_ID = "sophisticatedsorter";
    private static final String CLIENT_SORT_MOD_ID = "clientsort";
    private static final String HOTBAR_SWAPPER_MOD_ID = "hotbarswapper";
    
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
        if (mixinClassName.endsWith("SophisticatedSorterCoreUtilsCompatMixin")) {
            return isModLoaded(SOPHISTICATED_SORTER_MOD_ID);
        }
        
        if (mixinClassName.endsWith("MouseTweaksMainCompatMixin")) {
            return true;
        }

        if (mixinClassName.endsWith("Ae2MenuCompatMixin")) {
            return true;
        }

        if (mixinClassName.endsWith("Ae2StorageMenuCompatMixin")) {
            return true;
        }

        if (mixinClassName.endsWith("ClientSortCollectHandlerCompatMixin")
            || mixinClassName.endsWith("ClientSortSchemaValidatorCompatMixin")
            || mixinClassName.endsWith("ClientSortSortHandlerCompatMixin")
            || mixinClassName.endsWith("ClientSortStackFillHandlerCompatMixin")
            || mixinClassName.endsWith("ClientSortTransferHandlerCompatMixin")) {
            return isModLoaded(CLIENT_SORT_MOD_ID);
        }

        if (mixinClassName.endsWith("HotbarSwapperCompatMixin")) {
            return isModLoaded(HOTBAR_SWAPPER_MOD_ID);
        }
        if (mixinClassName.endsWith("WcwtStorageMenuCompatMixin")) return isModLoaded("wcwt");
        
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
    
    private boolean isModLoaded(String modId) {
        Boolean loadingResult = isModDiscoveredByLoadingList(modId);
        if (loadingResult != null) {
            return loadingResult;
        }
        return isModLoadedByRuntimeList(modId);
    }

    private Boolean isModDiscoveredByLoadingList(String modId) {
        try {
            Class<?> loadingModListClass = Class.forName("net.minecraftforge.fml.loading.LoadingModList");
            Object loadingModList = loadingModListClass.getMethod("get").invoke(null);
            if (loadingModList == null) {
                return null;
            }
            Object modFile = loadingModListClass.getMethod("getModFileById", String.class).invoke(loadingModList, modId);
            return modFile != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private boolean isModLoadedByRuntimeList(String modId) {
        try {
            Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            return modList != null && (Boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

}
