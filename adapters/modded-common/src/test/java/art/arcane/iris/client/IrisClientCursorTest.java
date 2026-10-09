package art.arcane.iris.client;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class IrisClientCursorTest {
    @Test
    public void refreshesAnUnchangedPositionAfterTheRefreshInterval() {
        AtomicLong clock = new AtomicLong(500L);
        AtomicInteger frames = new AtomicInteger();
        IrisClientCursor cursor = new IrisClientCursor(frame -> frames.incrementAndGet(), clock::get);

        cursor.requestFor(10, 20);
        clock.addAndGet(1_999L);
        cursor.requestFor(10, 20);
        clock.incrementAndGet();
        cursor.requestFor(10, 20);

        assertEquals(2, frames.get());
    }

    @Test
    public void throttlesRapidPositionChanges() {
        AtomicLong clock = new AtomicLong(500L);
        AtomicInteger frames = new AtomicInteger();
        IrisClientCursor cursor = new IrisClientCursor(frame -> frames.incrementAndGet(), clock::get);

        cursor.requestFor(10, 20);
        clock.addAndGet(499L);
        cursor.requestFor(11, 20);
        clock.incrementAndGet();
        cursor.requestFor(11, 20);

        assertEquals(2, frames.get());
    }

    @Test
    public void clearAllowsImmediateRequestAtTheSamePosition() {
        AtomicLong clock = new AtomicLong(500L);
        AtomicInteger frames = new AtomicInteger();
        IrisClientCursor cursor = new IrisClientCursor(frame -> frames.incrementAndGet(), clock::get);

        cursor.requestFor(10, 20);
        cursor.clear();
        cursor.requestFor(10, 20);

        assertEquals(2, frames.get());
    }
}
