package art.arcane.iris.api.world;

import art.arcane.iris.api.terrain.IrisWorldInfo;
import org.bukkit.World;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class IrisWorldEngineEventTest {
    private static final IrisWorldInfo INFO =
            new IrisWorldInfo("overworld", "minecraft:world", 7L, -64, 320, -1, false);

    @Test
    public void aPhaseIsStillReportableWhenTheEngineCannotBeDescribed() {
        IrisWorldEngineEvent event = new IrisWorldEngineEvent(world(), IrisWorldPhase.ENGINE_CLOSING, null);

        assertEquals(IrisWorldPhase.ENGINE_CLOSING, event.getPhase());
        assertFalse(event.getInfo().isPresent());
    }

    @Test
    public void aDescribableEngineCarriesItsInfo() {
        World world = world();
        IrisWorldEngineEvent event = new IrisWorldEngineEvent(world, IrisWorldPhase.ENGINE_READY, INFO);

        assertSame(world, event.getWorld());
        assertEquals(Optional.of(INFO), event.getInfo());
    }

    @Test
    public void theWorldAndPhaseAreAlwaysRequired() {
        assertThrows(NullPointerException.class,
                () -> new IrisWorldEngineEvent(null, IrisWorldPhase.ENGINE_READY, INFO));
        assertThrows(NullPointerException.class,
                () -> new IrisWorldEngineEvent(world(), null, INFO));
    }

    @Test
    public void everyPhaseSharesOneHandlerList() {
        assertNotNull(IrisWorldEngineEvent.getHandlerList());
        assertSame(IrisWorldEngineEvent.getHandlerList(),
                new IrisWorldEngineEvent(world(), IrisWorldPhase.ENGINE_HOTLOADED, null).getHandlers());
    }

    private static World world() {
        return (World) Proxy.newProxyInstance(
                IrisWorldEngineEventTest.class.getClassLoader(),
                new Class<?>[]{World.class},
                (Object proxy, Method method, Object[] arguments) -> switch (method.getName()) {
                    case "getName", "toString" -> "world";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
