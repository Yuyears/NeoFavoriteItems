package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;

/** Positions widgets and nested groups without owning their rendering or input lifecycle. */
public final class NfiWidgetGroup {
    private final List<Child> children = new ArrayList<>();
    private int x;
    private int y;

    public NfiWidgetGroup(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        children.forEach(this::place);
    }

    public void addRelative(AbstractWidget widget, int x, int y) {
        add(new Child(widget, null, x, y, true));
    }

    public void addAbsolute(AbstractWidget widget, int x, int y) {
        add(new Child(widget, null, x, y, false));
    }

    public void addRelative(NfiWidgetGroup group, int x, int y) {
        requireChild(group);
        add(new Child(null, group, x, y, true));
    }

    public void addAbsolute(NfiWidgetGroup group, int x, int y) {
        requireChild(group);
        add(new Child(null, group, x, y, false));
    }

    public List<AbstractWidget> widgets() {
        List<AbstractWidget> result = new ArrayList<>();
        collectWidgets(result);
        return List.copyOf(result);
    }

    public void clear() { children.clear(); }

    private void add(Child child) {
        children.add(child);
        place(child);
    }

    private void place(Child child) {
        int childX = child.relative ? x + child.x : child.x;
        int childY = child.relative ? y + child.y : child.y;
        if (child.widget != null) {
            child.widget.setX(childX);
            child.widget.setY(childY);
        } else {
            child.group.setPosition(childX, childY);
        }
    }

    private void collectWidgets(List<AbstractWidget> result) {
        for (Child child : children) {
            if (child.widget != null) {
                result.add(child.widget);
            } else {
                child.group.collectWidgets(result);
            }
        }
    }

    private void requireChild(NfiWidgetGroup group) {
        if (group == this) {
            throw new IllegalArgumentException("group cannot contain itself");
        }
    }

    private record Child(AbstractWidget widget, NfiWidgetGroup group, int x, int y, boolean relative) {}
}
