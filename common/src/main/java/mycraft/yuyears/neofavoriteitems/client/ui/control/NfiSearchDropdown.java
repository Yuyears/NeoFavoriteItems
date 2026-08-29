package mycraft.yuyears.neofavoriteitems.client.ui.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiCanvas;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiWidgetGroup;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/** Dropdown/combobox shared by static, searchable, queried, and lazy data sources. */
public final class NfiSearchDropdown<T> {
    public static final int PAGE_SIZE = 10;
    private static final long SEARCH_DELAY_MS = 500L;

    private final NfiValueBinding<T> binding;
    private final Function<T, String> textFactory;
    private final Function<String, T> valueParser;
    private final PageSource<T> source;
    private final Options options;
    private final int width;
    private final NfiEditBox search;
    private final NfiDropdown<T> dropdown;
    private final NfiWidgetGroup group;
    private final List<T> loaded = new ArrayList<>();
    private boolean syncing;
    private boolean endReached;
    private String appliedQuery;
    private long searchAt;

    public NfiSearchDropdown(Font font, int x, int y, int width, NfiValueBinding<T> binding,
                             Function<T, String> textFactory, Function<String, T> valueParser,
                             PageSource<T> source, Options options, Component endMessage) {
        this.binding = binding;
        this.textFactory = textFactory;
        this.valueParser = valueParser;
        this.source = source;
        this.options = options;
        this.width = width;
        this.group = new NfiWidgetGroup(x, y);
        int triggerWidth = 24;
        // One shared border pixel joins input and trigger without overlapping their content.
        this.search = new NfiEditBox(font, 0, 0, Math.max(1, width - triggerWidth + 1), 20, Component.empty());
        this.dropdown = new NfiDropdown<>(0, 0, 24, options.pageSize(), binding, List.of(),
            value -> Component.literal(textFactory.apply(value)));
        dropdown.setAllowEmptyOpen(true);
        dropdown.setEmbeddedTrigger(true);
        dropdown.setEndMessage(endMessage);
        dropdown.setPopupBounds(x, width);
        group.addRelative(search, 0, 0);
        group.addRelative(dropdown.group(), width - triggerWidth, 0);
        search.setEditable(options.searchable());
        search.setResponder(value -> {
            if (syncing) return;
            binding.set(valueParser.apply(value));
            if (options.searchable()) searchAt = System.currentTimeMillis() + options.debounceMillis();
        });
        syncFromBinding();
    }

    public NfiWidgetGroup group() { return group; }
    public List<AbstractWidget> widgets() { return List.of(search, dropdown.button()); }
    public NfiEditBox searchBox() { return search; }
    public NfiButton trigger() { return dropdown.button(); }
    public boolean isOpen() { return dropdown.isOpen(); }
    public boolean containsPopup(double mouseX, double mouseY) { return dropdown.containsPopup(mouseX, mouseY); }
    public void close() { dropdown.close(); }
    public void setOptionAction(NfiCanvas canvas, NfiDropdown.OptionAction<T> action) {
        dropdown.setOptionAction(canvas, action);
    }
    public void setOptionTooltipFactory(Function<T, Component> factory) {
        dropdown.setOptionTooltipFactory(factory);
    }

    public void tick() {
        if (options.searchable() && searchAt > 0L && System.currentTimeMillis() >= searchAt
            && !Objects.equals(appliedQuery, search.getValue())) {
            reloadSearch();
        }
    }

    public void syncFromBinding() {
        syncing = true;
        search.setValue(textFactory.apply(binding.get()));
        syncing = false;
        appliedQuery = null;
        searchAt = 0L;
    }

    public void layoutPopup(int screenHeight) {
        dropdown.setPopupBounds(search.getX(), width);
        dropdown.layoutPopup(screenHeight);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!dropdown.button().isMouseOver(mouseX, mouseY) && search.isMouseOver(mouseX, mouseY)) {
            if (button != 0) return false;
            search.setFocused(true);
            search.onClick(mouseX, mouseY);
            return true;
        }
        return handleDropdownClick(mouseX, mouseY, button);
    }

    private boolean handleDropdownClick(double mouseX, double mouseY, int button) {
        T before = binding.get();
        boolean wasOpen = dropdown.isOpen();
        if (!dropdown.mouseClicked(mouseX, mouseY, button)) return false;
        if (!wasOpen && dropdown.isOpen()) reloadSearch();
        search.setFocused(false);
        if (!Objects.equals(before, binding.get())) syncFromBinding();
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta < 0 && options.lazyLoad() && dropdown.isOpen() && !endReached
            && dropdown.scrollPosition() + options.pageSize() >= loaded.size()) {
            loadNextPage();
        }
        return dropdown.mouseScrolled(mouseX, mouseY, delta);
    }

    public boolean keyPressed(int keyCode) {
        T before = binding.get();
        boolean handled = dropdown.keyPressed(keyCode);
        if (handled && !Objects.equals(before, binding.get())) syncFromBinding();
        return handled;
    }

    public void open(int screenHeight) {
        if (appliedQuery == null) reloadSearch();
        dropdown.open();
        layoutPopup(screenHeight);
    }

    public void renderPopup(GuiGraphics graphics, int mouseX, int mouseY, float delta, int screenHeight) {
        layoutPopup(screenHeight);
        dropdown.renderPopup(graphics, mouseX, mouseY, delta);
    }

    private void reloadSearch() {
        appliedQuery = options.searchable() ? search.getValue() : "";
        searchAt = 0L;
        loaded.clear();
        endReached = false;
        loadNextPage();
    }

    private void loadNextPage() {
        if (!options.query()) {
            String needle = appliedQuery == null ? "" : appliedQuery.trim().toLowerCase(Locale.ROOT);
            loaded.clear();
            loaded.addAll(source.load("", 0, Integer.MAX_VALUE).values().stream()
                .filter(value -> needle.isEmpty()
                    || textFactory.apply(value).toLowerCase(Locale.ROOT).contains(needle))
                .toList());
            endReached = true;
            updateDropdown();
            return;
        }
        int offset = options.lazyLoad() ? loaded.size() : 0;
        int limit = options.lazyLoad() ? options.pageSize() : Integer.MAX_VALUE;
        Page<T> page = source.load(appliedQuery == null ? "" : appliedQuery, offset, limit);
        if (!options.lazyLoad()) loaded.clear();
        loaded.addAll(page.values());
        endReached = !options.lazyLoad() || page.values().size() < options.pageSize();
        updateDropdown();
    }

    private void updateDropdown() {
        dropdown.setValues(loaded);
        dropdown.setEndReached(endReached);
    }

    public record Page<T>(List<T> values) {
        public Page { values = List.copyOf(values); }
    }

    public record Options(boolean searchable, boolean query, boolean lazyLoad, int pageSize, long debounceMillis) {
        public Options {
            if (pageSize < 1) throw new IllegalArgumentException("pageSize must be positive");
            if (debounceMillis < 0) throw new IllegalArgumentException("debounceMillis must not be negative");
            if (lazyLoad && !query) throw new IllegalArgumentException("lazyLoad requires query");
        }

        public static Options plainDropdown() {
            return new Options(false, false, false, PAGE_SIZE, SEARCH_DELAY_MS);
        }

        public static Options searchableDropdown() {
            return new Options(true, false, false, PAGE_SIZE, SEARCH_DELAY_MS);
        }

        public static Options queryDropdown() {
            return new Options(false, true, false, PAGE_SIZE, SEARCH_DELAY_MS);
        }

        public static Options searchableQuery() {
            return new Options(true, true, false, PAGE_SIZE, SEARCH_DELAY_MS);
        }

        public static Options searchableLazyQuery() {
            return new Options(true, true, true, PAGE_SIZE, SEARCH_DELAY_MS);
        }
    }

    @FunctionalInterface
    public interface PageSource<T> {
        Page<T> load(String query, int offset, int limit);
    }
}
