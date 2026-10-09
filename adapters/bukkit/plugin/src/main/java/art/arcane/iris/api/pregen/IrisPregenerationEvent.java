package art.arcane.iris.api.pregen;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public class IrisPregenerationEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final IrisPregenPhase phase;
    private final IrisPregenProgress progress;

    public IrisPregenerationEvent(IrisPregenPhase phase, IrisPregenProgress progress) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public IrisPregenPhase getPhase() {
        return phase;
    }

    public IrisPregenProgress getProgress() {
        return progress;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
