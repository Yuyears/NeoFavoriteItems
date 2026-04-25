package mycraft.yuyears.neofavoriteitems.fabric.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

public class FabricCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String MOUSE_TWEAKS_MAIN_CLASS = "yalter.mousetweaks.Main";
    private static final String APPENG_MENU_CLASS = "appeng.menu.AEBaseMenu";
    private static final String APPENG_STORAGE_MENU_CLASS = "appeng.menu.me.common.MEStorageMenu";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("MouseTweaksMainMixin")) {
            return isClassPresent(MOUSE_TWEAKS_MAIN_CLASS);
        }
        if (mixinClassName.endsWith("AppEngMenuMixin")) {
            return isClassPresent(APPENG_MENU_CLASS);
        }
        if (mixinClassName.endsWith("AppEngStorageMenuMixin")) {
            return isClassPresent(APPENG_STORAGE_MENU_CLASS);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private boolean isClassPresent(String className) {
        try {
            MixinService.getService().getClassProvider().findClass(className, false);
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                return true;
            } catch (ClassNotFoundException ignored) {
                return false;
            }
        }
    }
}
