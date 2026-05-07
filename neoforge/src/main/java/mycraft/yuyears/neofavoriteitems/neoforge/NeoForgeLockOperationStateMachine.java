package mycraft.yuyears.neofavoriteitems.neoforge;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Method;
import java.util.List;

public final class NeoForgeLockOperationStateMachine {
    public static final NeoForgeLockOperationStateMachine INSTANCE = new NeoForgeLockOperationStateMachine();

    private static final int SOPHISTICATED_PLAYER_SLOT_COUNT = 36;
    private static final String SOPHISTICATED_STORAGE_SCREEN_BASE = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase";

    private boolean active;
    private Slot lastSlot;

    private NeoForgeLockOperationStateMachine() {
    }

    public boolean beginPress(double mouseX, double mouseY, String source) {
        AbstractContainerScreen<?> screen = currentContainerScreen();
        if (screen == null || !NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()) {
            finish();
            return false;
        }

        Slot slot = findLockOperationSlot(screen, mouseX, mouseY);
        if (slot == null || !NeoForgeSlotResolver.isPlayerInventorySlot(slot)) {
            DebugLogger.debug(
                "NeoForge lock state machine press ignored: source={} screen={} reason=no_player_slot mouseX={} mouseY={}",
                source,
                screen.getClass().getName(),
                mouseX,
                mouseY
            );
            finish();
            return false;
        }

        beginOperation();

        DebugLogger.debug(
            "NeoForge lock state machine press: source={} screen={} slotNull={} playerSlot={} slotId={} mouseX={} mouseY={}",
            source,
            screen.getClass().getName(),
            slot == null,
            slot != null && NeoForgeSlotResolver.isPlayerInventorySlot(slot),
            slot == null ? -1 : slot.index,
            mouseX,
            mouseY
        );
        toggle(slot, "click");
        return true;
    }

    public boolean isPlayerInventoryTarget(double mouseX, double mouseY, String source) {
        AbstractContainerScreen<?> screen = currentContainerScreen();
        if (screen == null) {
            return false;
        }

        Slot slot = findLockOperationSlot(screen, mouseX, mouseY);
        boolean playerInventoryTarget = slot != null && NeoForgeSlotResolver.isPlayerInventorySlot(slot);
        if (!playerInventoryTarget) {
            DebugLogger.debug(
                "NeoForge lock state machine target passed through: source={} screen={} slotNull={} slotId={} mouseX={} mouseY={}",
                source,
                screen.getClass().getName(),
                slot == null,
                slot == null ? -1 : slot.index,
                mouseX,
                mouseY
            );
        }
        return playerInventoryTarget;
    }

    public boolean consumeActiveDrag(String source) {
        if (!canContinueOperation()) {
            return false;
        }

        DebugLogger.debug("NeoForge lock state machine drag consumed without sampling: source={}", source);
        return true;
    }

    public boolean toggleEnteredSlot(Slot slot, String source) {
        if (!canContinueOperation()) {
            return false;
        }

        toggle(slot, source);
        return true;
    }

    public boolean toggleLeakedSlotClick(Slot slot, String source) {
        if (currentContainerScreen() == null || !NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()) {
            finish();
            return false;
        }
        if (slot == null || !NeoForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return false;
        }

        beginOperation();

        toggle(slot, source);
        return true;
    }

    public void tick() {
        if (!active) {
            return;
        }

        if (currentContainerScreen() == null || !NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()) {
            finish();
            return;
        }
        // Drag sampling is driven only by Mouse Tweaks slot-enter detection when that mod is present.
        // The client tick only cleans up stale state when the screen/key disappears.
    }

    public boolean isActive() {
        return active;
    }

    public Slot findLockOperationSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot playerSlot = isSophisticatedStorageScreen(screen)
            ? findSophisticatedPlayerSlotByCoordinates(screen, mouseX, mouseY)
            : findVanillaPlayerSlot(screen, mouseX, mouseY);
        if (playerSlot == null) {
            DebugLogger.debug(
                "NeoForge lock state machine slot lookup missed: screen={} mouseX={} mouseY={} candidates={}",
                screen.getClass().getName(),
                mouseX,
                mouseY,
                candidateSlots(screen).size()
            );
        }
        return playerSlot != null && NeoForgeSlotResolver.isPlayerInventorySlot(playerSlot) ? playerSlot : null;
    }

    private void toggle(Slot slot, String phase) {
        if (slot == null || slot == lastSlot || !NeoForgeSlotResolver.isPlayerInventorySlot(slot)) {
            return;
        }

        lastSlot = slot;
        if (NeoForgeSlotInteractionHandler.handleLockOperationToggle(slot)) {
            DebugLogger.debug(
                "NeoForge lock state machine {} handled: inventoryIndex={}",
                phase,
                NeoForgeSlotResolver.getPlayerInventoryIndex(slot)
            );
        }
    }

    private void beginOperation() {
        if (!active) {
            lastSlot = null;
            active = true;
        }
    }

    private boolean canContinueOperation() {
        if (!active) {
            return false;
        }
        if (currentContainerScreen() == null || !NeoFavoriteItemsNeoForge.isLockOperationKeyHeld()) {
            finish();
            return false;
        }
        return true;
    }

    private AbstractContainerScreen<?> currentContainerScreen() {
        return Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen ? screen : null;
    }

    private Slot findVanillaPlayerSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot screenSlot = invokeScreenFindSlot(screen, mouseX, mouseY);
        if (screenSlot != null && NeoForgeSlotResolver.isPlayerInventorySlot(screenSlot)) {
            return screenSlot;
        }
        return findPlayerSlotByCoordinates(screen, mouseX, mouseY);
    }

    private Slot findPlayerSlotByCoordinates(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        for (Slot slot : candidateSlots(screen)) {
            if (NeoForgeSlotResolver.isPlayerInventorySlot(slot)
                && slot.isActive()
                && isPointInSlot(screen, slot, mouseX, mouseY)) {
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
            DebugLogger.debug("NeoForge lock state machine screen slot lookup failed: screen={} error={}", screen.getClass().getName(), e.toString());
            return null;
        }
    }

    private Slot findSophisticatedPlayerSlotByCoordinates(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot screenSlot = invokeScreenFindSlot(screen, mouseX, mouseY);
        if (screenSlot != null && NeoForgeSlotResolver.isPlayerInventorySlot(screenSlot)) {
            DebugLogger.debug(
                "NeoForge lock state machine sophisticated screen lookup hit: slotId={} inventoryIndex={}",
                screenSlot.index,
                NeoForgeSlotResolver.getPlayerInventoryIndex(screenSlot)
            );
            return screenSlot;
        }

        Integer inventorySlotCount = invokeIntMethod(screen.getMenu(), "getInventorySlotsSize");
        if (inventorySlotCount == null || inventorySlotCount < SOPHISTICATED_PLAYER_SLOT_COUNT) {
            DebugLogger.debug(
                "NeoForge lock state machine sophisticated pure lookup unavailable: screen={} inventorySlots={}",
                screen.getClass().getName(),
                inventorySlotCount
            );
            return findPlayerSlotByCoordinates(screen, mouseX, mouseY);
        }

        int firstPlayerSlot = inventorySlotCount - SOPHISTICATED_PLAYER_SLOT_COUNT;
        for (int slotId = firstPlayerSlot; slotId < inventorySlotCount; slotId++) {
            Slot slot = getMenuSlot(screen, slotId);
            if (slot != null
                && NeoForgeSlotResolver.isPlayerInventorySlot(slot)
                && slot.isActive()
                && isPointInSlot(screen, slot, mouseX, mouseY)) {
                DebugLogger.debug(
                    "NeoForge lock state machine sophisticated pure lookup hit: slotId={} inventoryIndex={}",
                    slotId,
                    NeoForgeSlotResolver.getPlayerInventoryIndex(slot)
                );
                return slot;
            }
        }
        return null;
    }

    private List<Slot> candidateSlots(AbstractContainerScreen<?> screen) {
        return screen.getMenu().slots;
    }

    private boolean isPointInSlot(AbstractContainerScreen<?> screen, Slot slot, double mouseX, double mouseY) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int slotLeft = left + slot.x;
        int slotTop = top + slot.y;
        return mouseX >= slotLeft
            && mouseX < slotLeft + 16
            && mouseY >= slotTop
            && mouseY < slotTop + 16;
    }

    private Slot getMenuSlot(AbstractContainerScreen<?> screen, int slotId) {
        try {
            if (slotId < 0 || slotId >= screen.getMenu().slots.size()) {
                return null;
            }
            return screen.getMenu().getSlot(slotId);
        } catch (RuntimeException e) {
            DebugLogger.debug("NeoForge lock state machine menu slot lookup failed: slotId={} error={}", slotId, e.toString());
            return null;
        }
    }

    private Integer invokeIntMethod(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            Object result = method.invoke(target);
            return result instanceof Integer integer ? integer : null;
        } catch (ReflectiveOperationException e) {
            DebugLogger.debug("NeoForge lock state machine method lookup failed: method={} error={}", name, e.toString());
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
            if (SOPHISTICATED_STORAGE_SCREEN_BASE.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    public void finish() {
        active = false;
        lastSlot = null;
    }
}
