package art.arcane.iris.modded.command;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModdedStudioTransitionQueueTest {
    @Test
    public void sameOwnerTransitionsRunInSubmissionOrder() {
        ModdedStudioTransitionQueue queue = new ModdedStudioTransitionQueue();
        UUID owner = UUID.randomUUID();
        CompletableFuture<Void> firstGate = new CompletableFuture<>();
        AtomicInteger starts = new AtomicInteger();

        CompletableFuture<Void> first = queue.submit(owner, () -> {
            starts.incrementAndGet();
            return firstGate;
        });
        CompletableFuture<Void> second = queue.submit(owner, () -> {
            starts.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });

        assertEquals(1, starts.get());
        assertFalse(second.isDone());
        firstGate.complete(null);
        assertTrue(first.isDone());
        assertTrue(second.isDone());
        assertEquals(2, starts.get());
    }

    @Test
    public void differentOwnersDoNotBlockEachOther() {
        ModdedStudioTransitionQueue queue = new ModdedStudioTransitionQueue();
        CompletableFuture<Void> firstGate = new CompletableFuture<>();

        CompletableFuture<Void> first = queue.submit(UUID.randomUUID(), () -> firstGate);
        CompletableFuture<Void> second = queue.submit(
                UUID.randomUUID(),
                () -> CompletableFuture.completedFuture(null));

        assertFalse(first.isDone());
        assertTrue(second.isDone());
    }

    @Test
    public void failedTransitionDoesNotPoisonTheOwnerQueue() {
        ModdedStudioTransitionQueue queue = new ModdedStudioTransitionQueue();
        UUID owner = UUID.randomUUID();
        CompletableFuture<Void> failure = CompletableFuture.failedFuture(
                new IllegalStateException("expected"));
        AtomicInteger starts = new AtomicInteger();

        CompletableFuture<Void> first = queue.submit(owner, () -> failure);
        CompletableFuture<Void> second = queue.submit(owner, () -> {
            starts.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });

        assertTrue(first.isCompletedExceptionally());
        assertTrue(second.isDone());
        assertEquals(1, starts.get());
    }
}
