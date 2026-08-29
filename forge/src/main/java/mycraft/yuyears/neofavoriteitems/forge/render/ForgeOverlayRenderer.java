package mycraft.yuyears.neofavoriteitems.forge.render;

import com.mojang.blaze3d.platform.InputConstants;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;
import mycraft.yuyears.neofavoriteitems.application.InteractionGuardService;
import mycraft.yuyears.neofavoriteitems.client.ClientInteractionFeedback;
import mycraft.yuyears.neofavoriteitems.domain.InteractionType;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.forge.ForgeSlotResolver;
import mycraft.yuyears.neofavoriteitems.forge.NeoFavoriteItemsForge;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
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
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;

import java.lang.reflect.Method;

public class ForgeOverlayRenderer extends OverlayRenderer {
    private static ForgeOverlayRenderer instance;
    private final OverlayDrawEngine drawEngine = new OverlayDrawEngine();

    public ForgeOverlayRenderer() {
        instance = this;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            Slot slot = findSlotAt(screen, event.getMouseX(), event.getMouseY());
            boolean lockOperation = NeoFavoriteItemsForge.isLockOperationKeyHeld()
                && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;

            if (!lockOperation) {
                guardFavoriteSlotClick(event, slot);
            } else {
                DebugLogger.debug("Forge lock-operation click left for container state machine");
            }
        }
    }

    private void guardFavoriteSlotClick(ScreenEvent.MouseButtonPressed.Pre event, Slot slot) {
        if (slot == null || !isPlayerInventorySlot(slot)) {
            return;
        }

        int inventoryIndex = getContainerSlotIndex(slot);
        InteractionType interactionType = hasShiftDown() ? InteractionType.QUICK_MOVE : InteractionType.CLICK;
        var decision = InteractionGuardService.getInstance().evaluate(
            inventoryIndex,
            interactionType,
            NeoFavoriteItemsForge.isBypassKeyHeld(),
            hasItem(slot)
        );
        if (decision.denied()) {
            DebugLogger.debug("Forge slot interaction canceled: inventoryIndex={} interactionType={}", inventoryIndex, interactionType);
            ClientInteractionFeedback.playDeniedSound();
            event.setCanceled(true);
        }
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
        boolean isHoldingBypassKey = NeoFavoriteItemsForge.isBypassKeyHeld();
        boolean isHoldingLockOperationKey = NeoFavoriteItemsForge.isLockOperationKeyHeld();

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
            NeoFavoriteItemsForge.isBypassKeyHeld(),
            NeoFavoriteItemsForge.isLockOperationKeyHeld(),
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
        boolean isHoldingBypassKey = NeoFavoriteItemsForge.isBypassKeyHeld();
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
            NeoFavoriteItemsForge.isBypassKeyHeld(),
            phase
        );
        drawEngine.render(graphics);
    }

    public void renderTooltipOverlay(GuiGraphics context, int x, int y, String text) {
        context.renderTooltip(Minecraft.getInstance().font, Component.literal(text), x, y);
    }


    private Slot findSlotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot screenSlot = invokeScreenFindSlot(screen, mouseX, mouseY);
        if (screenSlot != null) {
            return screenSlot;
        }

        Slot hoveredSlot = ReflectionHelper.readField(screen, "hoveredSlot", Slot.class);
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
            DebugLogger.debug("Forge screen slot lookup failed: screen={} error={}", screen.getClass().getName(), e.toString());
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

    private boolean isPlayerInventorySlot(Slot slot) {
        return ForgeSlotResolver.isPlayerInventorySlot(slot);
    }

    private boolean hasItem(Slot slot) {
        return ForgeSlotResolver.hasItem(slot);
    }

    private int getContainerSlotIndex(Slot slot) {
        return ForgeSlotResolver.getPlayerInventoryIndex(slot);
    }

    private int getScreenLeft(AbstractContainerScreen<?> screen) {
        Integer value = ReflectionHelper.readIntField(screen, "leftPos");
        return value == null ? 0 : value;
    }

    private int getScreenTop(AbstractContainerScreen<?> screen) {
        Integer value = ReflectionHelper.readIntField(screen, "topPos");
        return value == null ? 0 : value;
    }

}
