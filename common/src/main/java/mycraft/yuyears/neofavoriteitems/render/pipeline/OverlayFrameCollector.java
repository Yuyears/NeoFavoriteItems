package mycraft.yuyears.neofavoriteitems.render.pipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mycraft.yuyears.neofavoriteitems.render.OverlayLayer;
import mycraft.yuyears.neofavoriteitems.render.OverlayProfile;
import mycraft.yuyears.neofavoriteitems.render.SlotRenderTarget;
import mycraft.yuyears.neofavoriteitems.render.OverlayRenderPhase;

public final class OverlayFrameCollector {
    private static final Comparator<OverlayDrawCommand> DRAW_ORDER = Comparator
        .comparingInt(OverlayDrawCommand::zIndex)
        .thenComparing(OverlayDrawCommand::clipToSlot)
        .thenComparing(command -> command.texture() == null ? "" : command.texture().toString())
        .thenComparing(command -> command.type().ordinal());

    private final List<OverlayDrawCommand> commands = new ArrayList<>();
    public void clear() {
        commands.clear();
    }

    public void submit(SlotRenderTarget target, OverlayProfile profile) {
        submit(target, profile, OverlayRenderPhase.ALL);
    }

    public void submit(SlotRenderTarget target, OverlayProfile profile, OverlayRenderPhase phase) {
        for (OverlayLayer layer : profile.layers()) {
            if (!layer.enabled() || !phase.includes(layer.zIndex())) {
                continue;
            }
            var bounds = layer.placement().resolve(target);
            commands.add(new OverlayDrawCommand(
                layer.type(),
                layer.zIndex(),
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                bounds.rotationDegrees(),
                layer.material().texture(),
                layer.material().color(),
                layer.material().opacity(),
                layer.material().colorMode(),
                layer.clipToSlot(),
                target.x(),
                target.y(),
                target.width(),
                target.height()
            ));
        }
    }

    public List<OverlayDrawCommand> sortedCommands() {
        commands.sort(DRAW_ORDER);
        return commands;
    }

    public int size() {
        return commands.size();
    }
}
