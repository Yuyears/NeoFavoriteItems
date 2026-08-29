package mycraft.yuyears.neofavoriteitems.render;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import net.minecraft.world.entity.player.Player;

/** Registry plus built-in layouts for known multi-hotbar mods. */
public final class HudSlotLayouts {
    private static final List<HudSlotLayoutProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final HudSlotLayoutProvider KNOWN_MODS = new KnownModsProvider();
    private static String lastDiagnostic;

    private HudSlotLayouts() {}

    public static void register(HudSlotLayoutProvider provider) {
        if (provider != null) PROVIDERS.add(provider);
    }

    public static boolean collect(Player player, int width, int height, Consumer<SlotRenderTarget> sink) {
        for (HudSlotLayoutProvider provider : PROVIDERS) {
            if (provider.collect(player, width, height, sink)) return true;
        }
        return KNOWN_MODS.collect(player, width, height, sink);
    }

    static int[] neoForgeQuadPosition(int row, int rows, int width, int height) {
        if (rows == 1) return new int[] {width / 2 - 91, height - 22};
        if (rows == 2 || rows == 4) {
            return new int[] {width / 2 - 182 * (row % 2 == 0 ? 1 : 0), height - 22 * (row < 2 ? 1 : 2)};
        }
        float column = row == 2 ? 0.5F : row == 1 ? 0.0F : 1.0F;
        return new int[] {(int) (width / 2.0F - 182.0F * column), height - 22 * (row == 2 ? 2 : 1)};
    }

    static int neoForgeDoublePhysicalIndex(int strip, int inventoryRow, int appliedColumn) {
        int column = strip % 9;
        int rowSlot = inventoryRow * 9 + column;
        boolean top = strip >= 9;
        if (column == appliedColumn) return top ? column : rowSlot;
        return top ? rowSlot : column;
    }

    private static final class KnownModsProvider implements HudSlotLayoutProvider {
        @Override
        public boolean collect(Player player, int width, int height, Consumer<SlotRenderTarget> sink) {
            if (isModLoaded("hotbaaaar")) {
                int bars = Math.clamp(width / 182, 1, 4);
                int x0 = width / 2 - bars * 91;
                for (int slot = 0; slot < bars * 9; slot++) {
                    accept(player, sink, slot, x0 + slot * 20 + 3 + slot / 9 * 2, height - 19);
                }
                diagnose("provider=hotbaaaar bars=" + bars);
                return true;
            }

            if (isModLoaded("quadhotbar")) {
                collectNeoForgeQuad(player, width, height, sink);
                return true;
            }

            if (!isModLoaded("double_hotbar")) return false;
            Object config = staticField("com.sidezbros.double_hotbar.DHModConfig", "INSTANCE");
            boolean neoForge = present("com.sidezbros.double_hotbar.DoubleHotbar$ClientForgeEvents");
            boolean fabric = present("com.sidezbros.double_hotbar.DoubleHotbar");
            if (config == null) {
                diagnose("double_hotbar detected but DHModConfig.INSTANCE unavailable; using conservative runtime layout");
                if (neoForge) {
                    collectNeoForgeDouble(player, width, height, sink, null);
                    return true;
                }
                if (fabric && staticField("com.sidezbros.double_hotbar.DoubleHotbar", "extendedHotbarSlot") != null) {
                    collectFabricQuad(player, width, height, sink, null);
                    return true;
                }
                if (fabric) {
                    collectFabricDouble(player, width, height, sink, null);
                    return true;
                }
                return false;
            }
            if (!bool(config, "displayDoubleHotbar", true) || bool(config, "disableMod", false)) return false;
            if (neoForge) {
                collectNeoForgeDouble(player, width, height, sink, config);
            } else if (bool(config, "quadHotbar", false)) {
                collectFabricQuad(player, width, height, sink, config);
                diagnose("provider=double_hotbar_fabric_quad");
            } else {
                collectFabricDouble(player, width, height, sink, config);
                diagnose("provider=double_hotbar_fabric");
            }
            return true;
        }

        private static void collectNeoForgeQuad(Player player, int width, int height,
                                                Consumer<SlotRenderTarget> sink) {
            int rows = invokeStaticInt("com.kharkivproject.quadhotbar.client.QuadHotbarClientEvents", "getRows", 1);
            rows = Math.clamp(rows, 1, 4);
            for (int row = 0; row < rows; row++) {
                int[] position = neoForgeQuadPosition(row, rows, width, height);
                for (int column = 0; column < 9; column++) {
                    accept(player, sink, row * 9 + column,
                        position[0] + 3 + column * 20, position[1] + 3);
                }
            }
            diagnose("provider=quadhotbar rows=" + rows);
        }

        private static void collectFabricDouble(Player player, int width, int height,
                                                Consumer<SlotRenderTarget> sink, Object config) {
            int shift = integer(config, "shift", 22);
            boolean reverse = bool(config, "reverseBars", false);
            int extraStart = integer(config, "inventoryRow", 3) * 9;
            int baseX = width / 2 - 88;
            int mainY = height - 19 - (reverse ? shift : 0);
            int extraY = height - 19 - (reverse ? 0 : shift);
            for (int column = 0; column < 9; column++) {
                accept(player, sink, column, baseX + column * 20, mainY);
                accept(player, sink, extraStart + column, baseX + column * 20, extraY);
            }
        }

        private static void collectNeoForgeDouble(Player player, int width, int height,
                                                  Consumer<SlotRenderTarget> sink, Object config) {
            int shift = integer(config, "shift", 22);
            boolean reverse = bool(config, "reverseBars", false);
            int row = integer(config, "inventoryRow", 3) * 9;
            // NeoForge renderer swaps physical stacks in the active column. Read
            // its applied column directly; virtualSlot alone loses that state.
            // Double Hotbar keeps swap state on outer class, not event subscriber.
            String events = "com.sidezbros.double_hotbar.DoubleHotbar$ClientForgeEvents";
            boolean topSelected = staticBool(events, "isTopRowSelected", false);
            int virtual = staticInt(events, "virtualSlot", -1);
            int bottomY = reverse ? height - 19 - shift : height - 19;
            int topY = reverse ? height - 19 : height - 19 - shift;
            int baseX = width / 2 - 88;
            diagnose("provider=double_hotbar_neoforge topSelected=" + topSelected
                + " virtualSlot=" + virtual + " row=" + row + " shift=" + shift + " reverse=" + reverse);
            for (int column = 0; column < 9; column++) {
                int bottomSlot = topSelected ? row + column : column;
                int topSlot = topSelected ? column : row + column;
                accept(player, sink, bottomSlot, baseX + column * 20, bottomY);
                accept(player, sink, topSlot, baseX + column * 20, topY);
            }
        }

        private static void collectFabricQuad(Player player, int width, int height,
                                              Consumer<SlotRenderTarget> sink, Object config) {
            int shift = integer(config, "shift", 0);
            boolean reverse = bool(config, "reverseBars", false);
            boolean leftReal = staticBool("com.sidezbros.double_hotbar.DoubleHotbar", "leftIsRealHotbar", true);
            int leftX = width / 2 - 181;
            int rightX = leftX + 181;
            int bottomY = height - 21 - shift;
            int topY = bottomY - 21;
            int realY = reverse ? topY : bottomY;
            int extraY = reverse ? bottomY : topY;
            for (int column = 0; column < 9; column++) {
                accept(player, sink, 18 + column, leftX + column * 20 + 3, extraY + 3);
                accept(player, sink, 9 + column, rightX + column * 20 + 3, extraY + 3);
                accept(player, sink, (leftReal ? 0 : 27) + column, leftX + column * 20 + 3, realY + 3);
                accept(player, sink, (leftReal ? 27 : 0) + column, rightX + column * 20 + 3, realY + 3);
            }
        }

        private static void accept(Player player, Consumer<SlotRenderTarget> sink, int slot, int x, int y) {
            if (slot < 0 || slot >= 36) return;
            sink.accept(SlotRenderTarget.standard(LogicalSlotIndex.of(slot), !player.getInventory().getItem(slot).isEmpty(), x, y));
        }

        private static boolean present(String name) {
            try { Class.forName(name, false, HudSlotLayouts.class.getClassLoader()); return true; }
            catch (ClassNotFoundException ignored) { return false; }
        }

        private static boolean isModLoaded(String modId) {
            for (String loader : List.of("net.neoforged.fml.ModList", "net.minecraftforge.fml.ModList")) {
                try {
                    Class<?> type = Class.forName(loader, false, HudSlotLayouts.class.getClassLoader());
                    Object list = type.getMethod("get").invoke(null);
                    return Boolean.TRUE.equals(type.getMethod("isLoaded", String.class).invoke(list, modId));
                } catch (ClassNotFoundException ignored) {
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    return false;
                }
            }
            try {
                Class<?> type = Class.forName("net.fabricmc.loader.api.FabricLoader", false, HudSlotLayouts.class.getClassLoader());
                Object loader = type.getMethod("getInstance").invoke(null);
                return Boolean.TRUE.equals(type.getMethod("isModLoaded", String.class).invoke(loader, modId));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }

        private static Object staticField(String className, String name) {
            try { return field(Class.forName(className, false, HudSlotLayouts.class.getClassLoader()), name).get(null); }
            catch (ReflectiveOperationException | RuntimeException exception) {
                if (className.contains("double_hotbar")) {
                    diagnose("double_hotbar reflection failed field=" + name + " type=" + exception.getClass().getSimpleName());
                }
                return null;
            }
        }

        private static int staticInt(String className, String name, int fallback) {
            Object value = staticField(className, name);
            return value instanceof Number number ? number.intValue() : fallback;
        }

        private static boolean staticBool(String className, String name, boolean fallback) {
            Object value = staticField(className, name);
            return value instanceof Boolean bool ? bool : fallback;
        }

        private static int invokeStaticInt(String className, String name, int fallback) {
            try {
                var method = Class.forName(className, false, HudSlotLayouts.class.getClassLoader()).getDeclaredMethod(name);
                method.setAccessible(true);
                Object value = method.invoke(null);
                return value instanceof Number number ? number.intValue() : fallback;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return fallback;
            }
        }

        private static int integer(Object owner, String name, int fallback) {
            Object value = value(owner, name);
            return value instanceof Number number ? number.intValue() : fallback;
        }

        private static boolean bool(Object owner, String name, boolean fallback) {
            Object value = value(owner, name);
            return value instanceof Boolean bool ? bool : fallback;
        }

        private static Object value(Object owner, String name) {
            if (owner == null) return null;
            try { return field(owner.getClass(), name).get(owner); }
            catch (ReflectiveOperationException | RuntimeException ignored) { return null; }
        }

        private static Field field(Class<?> type, String name) throws NoSuchFieldException {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }

        private static void diagnose(String message) {
            if (message.equals(lastDiagnostic)) return;
            lastDiagnostic = message;
            DebugLogger.debug("HUD layout compatibility: {}", message);
        }
    }
}
