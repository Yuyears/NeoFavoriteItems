package mycraft.yuyears.neofavoriteitems.client.ui.layout;

import java.util.List;

/** Common tree node contract for rows and nested sections. */
public interface NfiConfigNode {
    String stableKey();
    int depth();
    List<NfiConfigNode> children();
    int headerHeight();
    int contentHeight();
    boolean expanded();
}
