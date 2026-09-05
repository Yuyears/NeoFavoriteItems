package mycraft.yuyears.neofavoriteitems.neoforge.render;

import com.mojang.blaze3d.platform.InputConstants;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.client.ClientInteractionFeedback;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoFavoriteItemsNeoForge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeLockOperationStateMachine;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeMouseTweaksBridge;
import mycraft.yuyears.neofavoriteitems.neoforge.NeoForgeSlotResolver;
import mycraft.yuyears.neofavoriteitems.render.HotbarSlotLayout;
import mycraft.yuyears.neofavoriteitems.render.HudSlotProbe;
import mycraft.yuyears.neofavoriteitems.render.HudSlotLayouts;
import mycraft.yuyears.neofavoriteitems.render.OverlayProfile;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderer;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderPhase;
import mycraft.yuyears.neofavoriteitems.render.SlotRenderTarget;
import mycraft.yuyears.neofavoriteitems.render.pipeline.OverlayDrawEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NeoForgeOverlayRenderer extends OverlayRenderer {
    private static NeoForgeOverlayRenderer instance;
    private final OverlayDrawEngine drawEngine = new OverlayDrawEngine();

    public NeoForgeOverlayRenderer() {
        instance = this;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            DebugLogger.debug(
                "NeoForge mouse click pre: screen={} button={} lockOperation={} bypass={}",
                screen.getClass().getName(),
                event.getButton(),
                NeoFavoriteItemsNeoForge.isLockOperationKeyHeld(),
                NeoFavoriteItemsNeoForge.isBypassKeyHeld()
            );
            Slot slot = findSlotAt(screen, event.getMouseX(), event.getMouseY());
            boolean lockOperation = NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()
                && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;

            if (!lockOperation) {
                guardFavoriteSlotClick(event, slot);
            } else {
                if (NeoForgeLockOperationStateMachine.INSTANCE.beginPress(event.getMouseX(), event.getMouseY(), "screen-event")) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (event.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
            && !NeoForgeMouseTweaksBridge.isAvailable()
            && NeoForgeLockOperationStateMachine.INSTANCE.consumeActiveDrag("screen-event-drag")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && NeoForgeLockOperationStateMachine.INSTANCE.isActive()) {
            NeoForgeLockOperationStateMachine.INSTANCE.finish();
            event.setCanceled(true);
            return;
        }

        if ((event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            Slot slot = findSlotAt(screen, event.getMouseX(), event.getMouseY());
            if (shouldCancelFavoriteSlotInteraction(slot)) {
                event.setCanceled(true);
            }
        }
    }

    private void guardFavoriteSlotClick(ScreenEvent.MouseButtonPressed.Pre event, Slot slot) {
        if (shouldCancelFavoriteSlotInteraction(slot)) {
            ClientInteractionFeedback.playDeniedSound();
            event.setCanceled(true);
        }
    }

    private boolean shouldCancelFavoriteSlotInteraction(Slot slot) {
        if (slot == null || !isPlayerInventorySlot(slot)) {
            return false;
        }

        int inventoryIndex = getContainerSlotIndex(slot);
        InteractionType interactionType = hasShiftDown() ? InteractionType.QUICK_MOVE : InteractionType.CLICK;
        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            interactionType,
            NeoFavoriteItemsNeoForge.isBypassKeyHeld(),
            hasItem(slot)
        );
        if (decision.denied()) {
            DebugLogger.debug("NeoForge slot interaction canceled: inventoryIndex={} interactionType={}", inventoryIndex, interactionType);
            return true;
        }
        return false;
    }

    private boolean hasShiftDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static void renderScreenAbove(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
        if (instance != null) instance.renderContainerScreenOverlays(screen, graphics);
    }

    private void renderContainerScreenOverlays(AbstractContainerScreen<?> screen, GuiGraphics context) {
        boolean isHoldingBypassKey = NeoFavoriteItemsNeoForge.isBypassKeyHeld();
        boolean isHoldingLockOperationKey = NeoFavoriteItemsNeoForge.isLockOperationKeyHeld();

        drawEngine.beginFrame();
        for (Slot slot : screen.getMenu().slots) {
            if (!isPlayerInventorySlot(slot)) {
                continue;
            }

            var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot));
            if (logicalSlot.isEmpty()) {
                continue;
            }

            boolean hasItem = hasItem(slot);
            if (isLockableSlot(logicalSlot.get(), hasItem) || shouldRenderOverlay(logicalSlot.get())) {
                int x = slot.x + getScreenLeft(screen);
                int y = slot.y + getScreenTop(screen);
                submitSlotOverlay(
                    SlotRenderTarget.standard(logicalSlot.get(), hasItem, x, y),
                    isHoldingBypassKey,
                    isHoldingLockOperationKey,
                    OverlayRenderPhase.ABOVE_ITEM
                );
            }
        }
        drawEngine.render(context);
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean isHoldingBypassKey) {
        renderSlotOverlay(context, SlotRenderTarget.standard(slotIndex, true, x, y), isHoldingBypassKey, false);
    }

    public void renderSlotOverlay(GuiGraphics context, int x, int y, LogicalSlotIndex slotIndex, boolean hasItem, boolean isHoldingBypassKey, boolean isHoldingLockOperationKey) {
        renderSlotOverlay(
            context,
            SlotRenderTarget.standard(slotIndex, hasItem, x, y),
            isHoldingBypassKey,
            isHoldingLockOperationKey
        );
    }

    private void renderSlotOverlay(GuiGraphics context, SlotRenderTarget target, boolean isHoldingBypassKey, boolean isHoldingLockOperationKey) {
        drawEngine.beginFrame();
        submitSlotOverlay(target, isHoldingBypassKey, isHoldingLockOperationKey, OverlayRenderPhase.ALL);
        drawEngine.render(context);
    }

    private void submitSlotOverlay(SlotRenderTarget target, boolean isHoldingBypassKey,
                                   boolean isHoldingLockOperationKey, OverlayRenderPhase phase) {
        OverlayProfile profile = resolveProfile(target, isHoldingBypassKey, isHoldingLockOperationKey);
        if (profile != null) drawEngine.submit(target, profile, phase);
    }

    private void submitHudSlotOverlay(SlotRenderTarget target, boolean isHoldingBypassKey, OverlayRenderPhase phase) {
        OverlayProfile profile = resolveHudProfile(target, isHoldingBypassKey);
        if (profile != null) drawEngine.submit(target, profile, phase);
    }

    public static void renderSlotBelow(AbstractContainerScreen<?> screen, GuiGraphics graphics, Slot slot) {
        if (instance != null) instance.renderSlotBelowInternal(screen, graphics, slot);
    }

    private void renderSlotBelowInternal(AbstractContainerScreen<?> screen, GuiGraphics graphics, Slot slot) {
        if (!isPlayerInventorySlot(slot)) return;
        var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(getContainerSlotIndex(slot));
        if (logicalSlot.isEmpty()) return;
        boolean hasItem = hasItem(slot);
        if (!isLockableSlot(logicalSlot.get(), hasItem) && !shouldRenderOverlay(logicalSlot.get())) return;
        SlotRenderTarget target = SlotRenderTarget.standard(
            logicalSlot.get(), hasItem, slot.x, slot.y
        );
        drawEngine.beginFrame();
        submitSlotOverlay(
            target,
            NeoFavoriteItemsNeoForge.isBypassKeyHeld(),
            NeoFavoriteItemsNeoForge.isLockOperationKeyHeld(),
            OverlayRenderPhase.BELOW_ITEM
        );
        drawEngine.render(graphics);
    }

    public void renderHotbarOverlays(GuiGraphics context) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        boolean isHoldingBypassKey = NeoFavoriteItemsNeoForge.isBypassKeyHeld();
        drawEngine.beginFrame();
        boolean supplied = HudSlotLayouts.collect(client.player, screenWidth, screenHeight, target -> {
            if (!target.hasItem() || !HudSlotProbe.wasObserved(target.logicalSlot(), target.x(), target.y()))
                submitHudSlotOverlay(target, isHoldingBypassKey, OverlayRenderPhase.ALL);
        });
        for (int hotbarSlot = 0; !supplied && hotbarSlot < 9; hotbarSlot++) {
            LogicalSlotIndex slotIndex = LogicalSlotIndex.of(hotbarSlot);
            boolean hasItem = !client.player.getInventory().getItem(hotbarSlot).isEmpty();
            if (hasItem) continue;
            submitHudSlotOverlay(
                SlotRenderTarget.standard(
                    slotIndex,
                    hasItem,
                    HotbarSlotLayout.x(screenWidth, hotbarSlot),
                    HotbarSlotLayout.y(screenHeight)
                ),
                isHoldingBypassKey,
                OverlayRenderPhase.ALL
            );
        }
        drawEngine.render(context);
        HudSlotProbe.clearObserved();
    }

    public static void renderHudSlot(GuiGraphics graphics, Player player, ItemStack stack, int x, int y,
                                     OverlayRenderPhase phase) {
        if (instance != null) instance.renderHudSlotInternal(graphics, player, stack, x, y, phase);
    }

    private void renderHudSlotInternal(GuiGraphics graphics, Player player, ItemStack stack, int x, int y,
                                       OverlayRenderPhase phase) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        var logicalSlot = HudSlotProbe.resolve(player, stack);
        if (logicalSlot.isEmpty()) return;
        HudSlotProbe.markObserved(logicalSlot.get(), x, y);
        SlotRenderTarget target = SlotRenderTarget.standard(
            logicalSlot.get(), true, x, y
        );
        drawEngine.beginFrame();
        submitHudSlotOverlay(
            target,
            NeoFavoriteItemsNeoForge.isBypassKeyHeld(),
            phase
        );
        drawEngine.render(graphics);
    }

    private Slot findSlotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot lockSlot = NeoForgeLockOperationStateMachine.INSTANCE.findLockOperationSlot(screen, mouseX, mouseY);
        if (lockSlot != null) {
            return lockSlot;
        }

        Slot sophisticatedSlot = invokeSophisticatedFindSlot(screen, mouseX, mouseY);
        if (sophisticatedSlot != null) {
            return sophisticatedSlot;
        }

        Slot screenSlot = invokeScreenFindSlot(screen, mouseX, mouseY);
        if (screenSlot != null) {
            return screenSlot;
        }

        Slot hoveredSlot = readSlotField(screen, "hoveredSlot");
        if (hoveredSlot != null) {
            return hoveredSlot;
        }

        int left = getScreenLeft(screen);
        int top = getScreenTop(screen);
        for (Slot slot : screen.getMenu().slots) {
            int x = left + slot.x;
            int y = top + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    private Slot invokeScreenFindSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        try {
            Method method = findMethod(screen.getClass(), "findSlot", double.class, double.class);
            if (method == null) {
                return null;
            }
            Object result = method.invoke(screen, mouseX, mouseY);
            return result instanceof Slot slot ? slot : null;
        } catch (ReflectiveOperationException e) {
            DebugLogger.debug("NeoForge screen slot lookup failed: screen={} error={}", screen.getClass().getName(), e.toString());
            return null;
        }
    }

    private Slot invokeSophisticatedFindSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!isSophisticatedStorageScreen(screen)) {
            return null;
        }

        try {
            Method method = screen.getClass().getMethod("findSlot", double.class, double.class);
            Object result = method.invoke(screen, mouseX, mouseY);
            return result instanceof Slot slot ? slot : null;
        } catch (ReflectiveOperationException e) {
            DebugLogger.debug("NeoForge lock-operation sophisticated slot lookup failed: {}", e.toString());
            return null;
        }
    }

    private Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private boolean isSophisticatedStorageScreen(AbstractContainerScreen<?> screen) {
        Class<?> current = screen.getClass();
        while (current != null) {
            if ("net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase".equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        return NeoForgeSlotResolver.isPlayerInventorySlot(slot);
    }

    private boolean hasItem(Slot slot) {
        return NeoForgeSlotResolver.hasItem(slot);
    }

    private int getContainerSlotIndex(Slot slot) {
        return NeoForgeSlotResolver.getPlayerInventoryIndex(slot);
    }

    private int getScreenLeft(AbstractContainerScreen<?> screen) {
        Integer value = readIntField(screen, "leftPos");
        return value == null ? 0 : value;
    }

    private int getScreenTop(AbstractContainerScreen<?> screen) {
        Integer value = readIntField(screen, "topPos");
        return value == null ? 0 : value;
    }

    private Integer readIntField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    private Slot readSlotField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                return value instanceof Slot slot ? slot : null;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

}
