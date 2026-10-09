package art.arcane.iris.api.world;

import art.arcane.iris.api.terrain.IrisWorldInfo;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;
import java.util.Optional;

public class IrisWorldEngineEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final World world;
    private final IrisWorldPhase phase;
    private final IrisWorldInfo info;

    public IrisWorldEngineEvent(World world, IrisWorldPhase phase, IrisWorldInfo info) {
        this.world = Objects.requireNonNull(world, "world");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.info = info;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public World getWorld() {
        return world;
    }

    public IrisWorldPhase getPhase() {
        return phase;
    }

    public Optional<IrisWorldInfo> getInfo() {
        return Optional.ofNullable(info);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
