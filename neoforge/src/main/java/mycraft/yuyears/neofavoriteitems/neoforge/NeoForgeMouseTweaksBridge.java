package mycraft.yuyears.neofavoriteitems.neoforge;

public final class NeoForgeMouseTweaksBridge {
    private static final String MOUSE_TWEAKS_MAIN = "yalter.mousetweaks.Main";
    private static Boolean available;

    private NeoForgeMouseTweaksBridge() {
    }

    public static boolean isAvailable() {
        if (available == null) {
            available = isClassPresent(MOUSE_TWEAKS_MAIN);
        }
        return available;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
