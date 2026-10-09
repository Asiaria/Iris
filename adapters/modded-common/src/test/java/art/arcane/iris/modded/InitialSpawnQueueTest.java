package art.arcane.iris.modded;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InitialSpawnQueueTest {
    @Test
    public void rejectsDuplicatesAndEnforcesCapacityWhileInFlight() {
        InitialSpawnQueue queue = new InitialSpawnQueue(2);

        assertTrue(queue.offer(1L));
        assertFalse(queue.offer(1L));
        assertTrue(queue.offer(2L));
        assertEquals(Long.valueOf(1L), queue.poll());
        assertFalse(queue.offer(3L));
        queue.complete(1L);
        assertTrue(queue.offer(3L));
        assertEquals(2, queue.size());
    }

    @Test
    public void retryRotatesWithoutBlockingReadyEntries() {
        InitialSpawnQueue queue = new InitialSpawnQueue(4);
        queue.offer(1L);
        queue.offer(2L);
        queue.offer(3L);

        int budget = queue.batchSize(8);
        Long unavailable = queue.poll();
        queue.retry(unavailable);
        Long firstReady = queue.poll();
        queue.complete(firstReady);
        Long secondReady = queue.poll();
        queue.complete(secondReady);

        assertEquals(3, budget);
        assertEquals(Long.valueOf(1L), unavailable);
        assertEquals(Long.valueOf(2L), firstReady);
        assertEquals(Long.valueOf(3L), secondReady);
        assertEquals(1, queue.batchSize(8));
        assertEquals(Long.valueOf(1L), queue.poll());
    }

    @Test
    public void retryIsDeduplicated() {
        InitialSpawnQueue queue = new InitialSpawnQueue(2);
        queue.offer(7L);
        Long key = queue.poll();

        queue.retry(key);
        queue.retry(key);

        assertEquals(1, queue.batchSize(8));
        assertEquals(Long.valueOf(7L), queue.poll());
        assertNull(queue.poll());
    }

    @Test
    public void closeClearsAndRejectsFurtherWork() {
        InitialSpawnQueue queue = new InitialSpawnQueue(2);
        queue.offer(1L);

        queue.close();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertFalse(queue.offer(2L));
        assertEquals(0, queue.batchSize(8));
    }

    @Test
    public void clearAllowsLaterWork() {
        InitialSpawnQueue queue = new InitialSpawnQueue(2);
        queue.offer(1L);

        queue.clear();

        assertTrue(queue.offer(2L));
        assertEquals(Long.valueOf(2L), queue.poll());
    }

    @Test
    public void expiredEntriesReleaseCapacity() {
        AtomicLong now = new AtomicLong();
        InitialSpawnQueue queue = new InitialSpawnQueue(2, 100L, now::get);
        queue.offer(1L);
        queue.offer(2L);

        now.set(100L);

        assertTrue(queue.offer(3L));
        assertEquals(1, queue.size());
        assertEquals(Long.valueOf(3L), queue.poll());
    }

    @Test
    public void expireKeepsYoungerOffersWhenReleasingCapacity() {
        AtomicLong now = new AtomicLong();
        InitialSpawnQueue queue = new InitialSpawnQueue(2, 100L, now::get);
        queue.offer(1L);
        now.set(60L);
        queue.offer(2L);

        now.set(100L);

        assertTrue(queue.offer(3L));
        assertEquals(2, queue.size());
        assertEquals(Long.valueOf(2L), queue.poll());
        assertEquals(Long.valueOf(3L), queue.poll());
        assertNull(queue.poll());
    }

    @Test
    public void expiredInFlightEntryCannotRetry() {
        AtomicLong now = new AtomicLong();
        InitialSpawnQueue queue = new InitialSpawnQueue(2, 100L, now::get);
        queue.offer(1L);
        Long inFlight = queue.poll();

        now.set(100L);
        queue.retry(inFlight);

        assertEquals(0, queue.size());
        assertNull(queue.poll());
    }

    @Test
    public void concurrentOffersRemainDeduplicatedAndBounded() throws Exception {
        InitialSpawnQueue queue = new InitialSpawnQueue(256);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int thread = 0; thread < 8; thread++) {
            futures.add(executor.submit(() -> {
                for (long key = 0L; key < 512L; key++) {
                    queue.offer(key);
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertTrue(executor.awaitTermination(10L, TimeUnit.SECONDS));
        assertEquals(256, queue.size());
        assertEquals(256, queue.batchSize(512));
    }
}
