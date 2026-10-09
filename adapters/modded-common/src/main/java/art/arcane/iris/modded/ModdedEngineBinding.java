/*
 * Iris is a World Generator for Minecraft Servers
 * Copyright (c) 2026 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package art.arcane.iris.modded;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class ModdedEngineBinding<T> {
    private final long timeout;
    private final TimeUnit timeoutUnit;
    private volatile CompletableFuture<T> future = new CompletableFuture<>();

    ModdedEngineBinding(long timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    T await(String dimensionKey) {
        try {
            return future.get(timeout, timeoutUnit);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Iris generator '"
                    + dimensionKey + "' to bind", error);
        } catch (ExecutionException error) {
            throw new IllegalStateException("Iris generator '" + dimensionKey + "' failed to bind",
                    error.getCause());
        } catch (TimeoutException error) {
            throw new IllegalStateException("Timed out waiting for Iris generator '"
                    + dimensionKey + "' to bind", error);
        }
    }

    void complete(T value) {
        future.complete(value);
    }

    void fail(Throwable error) {
        future.completeExceptionally(error);
    }

    void throwIfFailed(String dimensionKey) {
        CompletableFuture<T> current = future;
        if (!current.isCompletedExceptionally()) {
            return;
        }
        try {
            current.join();
        } catch (CompletionException error) {
            throw new IllegalStateException("Iris generator '" + dimensionKey + "' failed to bind",
                    error.getCause());
        }
    }

    void reset() {
        future = new CompletableFuture<>();
    }
}
