
package mycraft.yuyears.neofavoriteitems.fabric.render;

import mycraft.yuyears.neofavoriteitems.fabric.FabricSlotResolver;
import mycraft.yuyears.neofavoriteitems.fabric.NeoFavoriteItemsFabricClient;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.integration.SlotMappingService;
import mycraft.yuyears.neofavoriteitems.render.HotbarSlotLayout;
import mycraft.yuyears.neofavoriteitems.render.HudSlotProbe;
import mycraft.yuyears.neofavoriteitems.render.HudSlotLayouts;
import mycraft.yuyears.neofavoriteitems.render.OverlayProfile;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderer;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderPhase;
import mycraft.yuyears.neofavoriteitems.render.SlotRenderTarget;
import mycraft.yuyears.neofavoriteitems.render.pipeline.OverlayDrawEngine;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import mycraft.yuyears.neofavoriteitems.common.util.ReflectionHelper;

public class FabricOverlayRenderer extends OverlayRenderer {
    private static FabricOverlayRenderer instance;
    private final OverlayDrawEngine drawEngine = new OverlayDrawEngine();
    private boolean lastLoggedLockOperationKeyState;

    public FabricOverlayRenderer() {
        instance = this;
        registerEvents();
        registerHudEvents();
        DebugLogger.debug("Fabric overlay renderer registered");
    }

    private void registerEvents() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?>) {
                DebugLogger.debug("Fabric container screen initialized: screen={} size={}x{}", screen.getClass().getName(), scaledWidth, scaledHeight);
                ScreenMouseEvents.allowMouseClick(screen).register((screen1, mouseX, mouseY, button) -> {
                    if (screen1 instanceof AbstractContainerScreen<?> containerScreen) {
                        return allowMouseClick(containerScreen, mouseX, mouseY, button);
                    }
                    return true;
                });
            }
        });
    }

    private boolean allowMouseClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (button != 0 || !NeoFavoriteItemsFabricClient.isLockOperationKeyHeld()) {
            return true;
        }

        DebugLogger.debug("Fabric lock-operation click left for container state machine");
        return true;
    }

    private void registerHudEvents() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHotbarOverlays(context));
    }

    public static void renderScreenAbove(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
        if (instance != null) instance.renderHandledScreenOverlays(screen, graphics);
    }

    private void renderHandledScreenOverlays(AbstractContainerScreen<?> screen, GuiGraphics context) {
        boolean isHoldingBypassKey = NeoFavoriteItemsFabricClient.isBypassKeyHeld();
        boolean isHoldingLockOperationKey = NeoFavoriteItemsFabricClient.isLockOperationKeyHeld();
        NeoFavoriteItemsFabricClient.logKeyStatesIfChanged();
        
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int highlightableSlots = 0;
        int lockedSlots = 0;
        drawEngine.beginFrame();
        for (Slot slot : screen.getMenu().slots) {
            // 只处理属于玩家物品栏的槽位
            if (!FabricSlotResolver.isPlayerInventorySlot(slot, player)) {
                continue;
            }
            
            int inventoryIndex = FabricSlotResolver.getPlayerInventoryIndex(slot);
            boolean hasItem = FabricSlotResolver.hasItem(slot);
            var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(inventoryIndex);
            if (logicalSlot.isEmpty()) {
                continue;
            }
            if (hasItem) {
                highlightableSlots++;
            }
            if (shouldRenderOverlay(logicalSlot.get())) {
                lockedSlots++;
            }
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
        if (isHoldingLockOperationKey != lastLoggedLockOperationKeyState) {
            DebugLogger.debug(
                "Fabric overlay lock-operation render state: active={} screen={} highlightableSlots={} lockedSlots={}",
                isHoldingLockOperationKey,
                screen.getClass().getName(),
                highlightableSlots,
                lockedSlots
            );
            lastLoggedLockOperationKeyState = isHoldingLockOperationKey;
        }
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
        OverlayProfile profile = resolveProfile(target, isHoldingBypassKey, isHoldingLockOperationKey);
        if (profile != null) {
            drawEngine.renderOne(context, target, profile);
        }
    }

    private void submitSlotOverlay(SlotRenderTarget target, boolean isHoldingBypassKey,
                                   boolean isHoldingLockOperationKey, OverlayRenderPhase phase) {
        OverlayProfile profile = resolveProfile(target, isHoldingBypassKey, isHoldingLockOperationKey);
        if (profile != null) {
            drawEngine.submit(target, profile, phase);
        }
    }

    private void submitHudSlotOverlay(SlotRenderTarget target, boolean isHoldingBypassKey, OverlayRenderPhase phase) {
        OverlayProfile profile = resolveHudProfile(target, isHoldingBypassKey);
        if (profile != null) drawEngine.submit(target, profile, phase);
    }

    public static void renderSlotBelow(AbstractContainerScreen<?> screen, GuiGraphics graphics, Slot slot) {
        if (instance != null) instance.renderSlotBelowInternal(screen, graphics, slot);
    }

    private void renderSlotBelowInternal(AbstractContainerScreen<?> screen, GuiGraphics graphics, Slot slot) {
        var player = Minecraft.getInstance().player;
        if (player == null || !FabricSlotResolver.isPlayerInventorySlot(slot, player)) return;
        var logicalSlot = SlotMappingService.fromPlayerInventoryIndex(FabricSlotResolver.getPlayerInventoryIndex(slot));
        if (logicalSlot.isEmpty()) return;
        boolean hasItem = FabricSlotResolver.hasItem(slot);
        if (!isLockableSlot(logicalSlot.get(), hasItem) && !shouldRenderOverlay(logicalSlot.get())) return;
        SlotRenderTarget target = SlotRenderTarget.standard(
            logicalSlot.get(), hasItem, slot.x, slot.y
        );
        OverlayProfile profile = resolveProfile(
            target,
            NeoFavoriteItemsFabricClient.isBypassKeyHeld(),
            NeoFavoriteItemsFabricClient.isLockOperationKeyHeld()
        );
        if (profile != null) drawEngine.renderOne(graphics, target, profile, OverlayRenderPhase.BELOW_ITEM);
    }

    public void renderHotbarOverlays(GuiGraphics context) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        boolean isHoldingBypassKey = NeoFavoriteItemsFabricClient.isBypassKeyHeld();
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
            NeoFavoriteItemsFabricClient.isBypassKeyHeld(),
            phase
        );
        drawEngine.render(graphics);
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
