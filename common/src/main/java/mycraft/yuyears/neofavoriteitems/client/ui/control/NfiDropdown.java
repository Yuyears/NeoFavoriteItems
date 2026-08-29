package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiCanvas;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiWidgetGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class NfiDropdown<T> {
    private static final int ROW_HEIGHT = 20;
    private static final int POPUP_MARGIN = 4;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int OPTION_ACTION_WIDTH = 24;
    private final NfiValueBinding<T> binding;
    private List<T> values;
    private final Function<T, Component> labelFactory;
    private final NfiButton selectionButton;
    private final List<NfiButton> optionButtons = new ArrayList<>();
    private final List<NfiButton> optionActionButtons = new ArrayList<>();
    private final NfiWidgetGroup group;
    private int firstVisible;
    private int popupRowCapacity;
    private int visibleRows;
    private boolean showEndMessage;
    private boolean open;
    private boolean popupAbove;
    private int popupLeft = Integer.MIN_VALUE;
    private int popupWidth;
    private Component endMessage;
    private boolean allowEmptyOpen;
    private boolean endReached;
    private OptionRenderer<T> optionRenderer = (graphics, value, x, y, width, height, selected) -> {};
    private Function<T, Component> optionTooltipFactory;
    private OptionAction<T> optionAction;
    private NfiCanvas optionActionCanvas;

    public NfiDropdown(int x, int y, int width, int maxVisibleRows, NfiValueBinding<T> binding,
                       List<T> values, Function<T, Component> labelFactory) {
        if (maxVisibleRows < 1) throw new IllegalArgumentException("maxVisibleRows must be positive");
        this.binding = binding;
        this.values = List.copyOf(values);
        this.labelFactory = labelFactory;
        this.popupWidth = width;
        this.popupRowCapacity = maxVisibleRows;
        this.group = new NfiWidgetGroup(x, y);
        this.selectionButton = new NfiButton(0, 0, width, ROW_HEIGHT, Component.empty(), ignored -> setOpen(!open));
        selectionButton.setDropdown(true);
        group.addRelative(selectionButton, 0, 0);
        for (int slot = 0; slot < maxVisibleRows; slot++) {
            final int selectedSlot = slot;
            NfiButton option = new NfiButton(0, 0, width, ROW_HEIGHT, Component.empty(), ignored -> selectVisible(selectedSlot));
            optionButtons.add(option);
            group.addRelative(option, 0, ROW_HEIGHT + slot * ROW_HEIGHT);
            NfiButton action = new NfiButton(0, 0, OPTION_ACTION_WIDTH, ROW_HEIGHT,
                Component.empty(), ignored -> runOptionAction(selectedSlot));
            action.setEmbeddedDropdownTrigger(true);
            action.setSilent(true);
            optionActionButtons.add(action);
        }
        selectionButton.active = !values.isEmpty();
        setOpen(false);
        syncFromBinding();
    }

    public NfiWidgetGroup group() {
        return group;
    }

    public NfiButton button() {
        return selectionButton;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean containsPopup(double mouseX, double mouseY) {
        return open && isInsidePopup(mouseX, mouseY);
    }

    int scrollPosition() {
        return firstVisible;
    }

    int visibleRowCount() { return visibleRows; }

    public void open() {
        setOpen(true);
    }

    public void close() {
        setOpen(false);
    }

    public void syncFromBinding() {
        selectionButton.setMessage(values.isEmpty() ? Component.empty() : labelFactory.apply(binding.get()));
        refreshOptions();
    }

    public void setValues(List<T> values) {
        this.values = List.copyOf(values);
        updateVisibleWindow();
        selectionButton.active = allowEmptyOpen || !values.isEmpty();
        syncFromBinding();
    }

    public void setOptionRenderer(OptionRenderer<T> optionRenderer) {
        this.optionRenderer = optionRenderer == null
            ? (graphics, value, x, y, width, height, selected) -> {}
            : optionRenderer;
    }

    public void setOptionTooltipFactory(Function<T, Component> factory) {
        optionTooltipFactory = factory;
        refreshOptions();
    }

    public void setOptionAction(NfiCanvas canvas, OptionAction<T> action) {
        optionActionCanvas = canvas;
        optionAction = action;
        optionActionButtons.forEach(button -> button.setCanvas(canvas));
        refreshOptions();
    }

    /** Keeps compact trigger button while allowing an embedded full-width popup. */
    public void setPopupBounds(int left, int width) {
        popupLeft = left;
        popupWidth = Math.max(selectionButton.getWidth(), width);
    }

    public void setEndMessage(Component message) { endMessage = message; }
    public void setEndReached(boolean reached) {
        endReached = reached;
        updateVisibleWindow();
        refreshOptions();
    }

    public void setAllowEmptyOpen(boolean allow) {
        allowEmptyOpen = allow;
        selectionButton.active = allow || !values.isEmpty();
    }

    public void setEmbeddedTrigger(boolean embedded) {
        selectionButton.setEmbeddedDropdownTrigger(embedded);
    }

    public void layoutPopup(int screenHeight) {
        int desiredRows = Math.max(1, Math.min(optionButtons.size(), values.size()) + (hasEndRow() ? 1 : 0));
        int below = Math.max(0, screenHeight - group.y() - ROW_HEIGHT - 1);
        int above = Math.max(0, group.y() - 1);
        int desiredHeight = desiredRows * ROW_HEIGHT;
        popupAbove = desiredHeight > below && above > below;
        int available = popupAbove ? above : below;
        popupRowCapacity = Math.max(1, Math.min(desiredRows, available / ROW_HEIGHT));
        updateVisibleWindow();
        for (int slot = 0; slot < optionButtons.size(); slot++) {
            int rowY = popupTop() + slot * ROW_HEIGHT;
            int actionWidth = optionAction == null ? 0 : OPTION_ACTION_WIDTH;
            optionButtons.get(slot).setY(rowY);
            optionButtons.get(slot).setX(popupX() + POPUP_MARGIN);
            optionButtons.get(slot).setWidth(popupWidth - POPUP_MARGIN * 2 - actionWidth);
            optionActionButtons.get(slot).setY(rowY);
            optionActionButtons.get(slot).setX(popupX() + popupWidth - POPUP_MARGIN - OPTION_ACTION_WIDTH);
        }
        refreshOptions();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!open || !isInsidePopup(mouseX, mouseY)) return false;
        int maximum = maximumScroll();
        if (maximum <= 0) return true;
        firstVisible = Math.clamp(firstVisible + (delta < 0 ? 1 : -1), 0, maximum);
        updateVisibleWindow();
        refreshOptions();
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) return selectionButton.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && isInsideScrollbar(mouseX, mouseY)) {
            int maximum = maximumScroll();
            int top = popupTop();
            int trackHeight = popupHeight();
            firstVisible = Math.clamp(
                (int) Math.round((mouseY - top) / trackHeight * maximum),
                0,
                maximum
            );
            updateVisibleWindow();
            refreshOptions();
            return true;
        }
        for (NfiButton action : optionActionButtons) {
            if (action.visible && action.mouseClicked(mouseX, mouseY, button)) return true;
        }
        for (NfiButton option : optionButtons) {
            if (option.visible && option.mouseClicked(mouseX, mouseY, button)) return true;
        }
        if (selectionButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (isInsidePopup(mouseX, mouseY)) return true;
        close();
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!open) return false;
        if (keyCode == 256) {
            close();
            return true;
        }
        int selected = Math.max(0, values.indexOf(binding.get()));
        if ((keyCode == 264 || keyCode == 265) && !values.isEmpty()) {
            selected = Math.clamp(selected + (keyCode == 264 ? 1 : -1), 0, values.size() - 1);
            binding.set(values.get(selected));
            reveal(selected);
            syncFromBinding();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            close();
            return true;
        }
        return false;
    }

    public void renderPopup(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!open) return;
        int popupHeight = popupHeight();
        int top = popupTop();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 500.0f);
        NfiUiRenderer.panel(graphics, popupX(), top - 1,
            popupWidth, popupHeight + 2);
        for (int slot = 0; slot < visibleRows; slot++) {
            NfiButton option = optionButtons.get(slot);
            option.render(graphics, mouseX, mouseY, delta);
            int index = firstVisible + slot;
            if (index < values.size()) {
                T value = values.get(index);
                optionRenderer.render(graphics, value, option.getX(), option.getY(), option.getWidth(), ROW_HEIGHT,
                    java.util.Objects.equals(value, binding.get()));
                NfiButton action = optionActionButtons.get(slot);
                if (action.visible) action.render(graphics, mouseX, mouseY, delta);
            }
        }
        renderScrollbar(graphics, top);
        if (showEndMessage) {
            NfiUiRenderer.centeredText(graphics, net.minecraft.client.Minecraft.getInstance().font,
                endMessage, popupX() + popupWidth / 2, top + visibleRows * ROW_HEIGHT + 3,
                NfiUiRenderer.controlTextColor(true));
        }
        graphics.pose().popPose();
    }

    /** Kept for existing callers. */
    public void renderPopupBackground(GuiGraphics graphics, int color) {
        renderPopup(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, 0.0f);
    }

    private void selectVisible(int slot) {
        int index = firstVisible + slot;
        if (index >= values.size()) return;
        binding.set(values.get(index));
        selectionButton.setMessage(labelFactory.apply(binding.get()));
        close();
    }

    private void setOpen(boolean open) {
        this.open = open;
        selectionButton.setSelected(open);
        refreshOptions();
    }

    private void refreshOptions() {
        for (int slot = 0; slot < optionButtons.size(); slot++) {
            int index = firstVisible + slot;
            NfiButton option = optionButtons.get(slot);
            option.visible = open && slot < visibleRows && index < values.size();
            NfiButton action = optionActionButtons.get(slot);
            action.visible = option.visible && optionAction != null && optionActionCanvas != null;
            action.active = selectionButton.active;
            if (index < values.size()) {
                option.setMessage(labelFactory.apply(values.get(index)));
                option.setSelected(java.util.Objects.equals(values.get(index), binding.get()));
                Component tooltip = optionTooltipFactory == null ? null : optionTooltipFactory.apply(values.get(index));
                option.setTooltip(tooltip == null ? null : net.minecraft.client.gui.components.Tooltip.create(tooltip));
            } else {
                option.setTooltip(null);
            }
        }
    }

    private void reveal(int selected) {
        if (selected < firstVisible) firstVisible = selected;
        if (selected >= firstVisible + visibleRows) firstVisible = selected - Math.max(1, visibleRows) + 1;
        updateVisibleWindow();
    }

    private boolean isInside(double mouseX, double mouseY) {
        int top = popupTop();
        return mouseX >= popupX() && mouseX < popupX() + popupWidth
            && mouseY >= top && mouseY < top + visibleRows * ROW_HEIGHT;
    }

    private boolean isInsidePopup(double mouseX, double mouseY) {
        int top = popupTop();
        return mouseX >= popupX() && mouseX < popupX() + popupWidth
            && mouseY >= top && mouseY < top + popupHeight();
    }

    private boolean isInsideScrollbar(double mouseX, double mouseY) {
        return maximumScroll() > 0
            && mouseX >= popupX() + popupWidth - POPUP_MARGIN
            && isInsidePopup(mouseX, mouseY);
    }

    private int popupTop() {
        return popupAbove ? group.y() - popupHeight() : group.y() + ROW_HEIGHT;
    }

    private int popupHeight() {
        return (visibleRows + (showEndMessage ? 1 : 0)) * ROW_HEIGHT;
    }

    private boolean hasEndRow() {
        return endMessage != null && endReached;
    }

    private int totalRows() {
        return values.size() + (hasEndRow() ? 1 : 0);
    }

    private int maximumScroll() {
        return Math.max(0, totalRows() - popupRowCapacity);
    }

    private void updateVisibleWindow() {
        firstVisible = Math.clamp(firstVisible, 0, maximumScroll());
        showEndMessage = hasEndRow() && firstVisible + popupRowCapacity > values.size();
        int optionCapacity = popupRowCapacity - (showEndMessage ? 1 : 0);
        visibleRows = Math.min(optionButtons.size(), Math.min(
            Math.max(0, values.size() - firstVisible), Math.max(0, optionCapacity)));
    }

    private int popupX() {
        return popupLeft == Integer.MIN_VALUE ? group.x() : popupLeft;
    }

    private void renderScrollbar(GuiGraphics graphics, int top) {
        int maximum = maximumScroll();
        if (maximum <= 0) return;
        int trackHeight = popupHeight();
        int trackX = popupX() + popupWidth - 3;
        int thumbHeight = Math.max(8, trackHeight * popupRowCapacity / totalRows());
        int thumbY = top + (int) Math.round((double) firstVisible / maximum * (trackHeight - thumbHeight));
        graphics.fill(trackX, top, trackX + SCROLLBAR_WIDTH, top + trackHeight, NfiUiRenderer.scrollbarTrackColor());
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, NfiUiRenderer.scrollbarColor());
    }

    private void runOptionAction(int slot) {
        int index = firstVisible + slot;
        if (optionAction != null && index < values.size()) optionAction.run(values.get(index));
    }

    @FunctionalInterface
    public interface OptionRenderer<T> {
        void render(GuiGraphics graphics, T value, int x, int y, int width, int height, boolean selected);
    }

    @FunctionalInterface
    public interface OptionAction<T> {
        void run(T value);
    }
}
