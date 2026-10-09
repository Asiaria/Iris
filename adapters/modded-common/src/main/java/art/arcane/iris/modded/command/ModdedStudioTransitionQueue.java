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

package art.arcane.iris.modded.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class ModdedStudioTransitionQueue {
    private final Object lock = new Object();
    private final Map<UUID, CompletableFuture<Void>> tails = new HashMap<>();

    CompletableFuture<Void> submit(UUID owner, Supplier<CompletableFuture<Void>> transition) {
        Objects.requireNonNull(owner, "Studio transition owner");
        Objects.requireNonNull(transition, "Studio transition");
        synchronized (lock) {
            CompletableFuture<Void> previous = tails.get(owner);
            CompletableFuture<Void> admission = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous.handle((ignored, failure) -> null);
            CompletableFuture<Void> current = admission.thenCompose((ignored) -> transition.get());
            tails.put(owner, current);
            current.whenComplete((ignored, failure) -> remove(owner, current));
            return current;
        }
    }

    void clear() {
        synchronized (lock) {
            tails.clear();
        }
    }

    private void remove(UUID owner, CompletableFuture<Void> transition) {
        synchronized (lock) {
            tails.remove(owner, transition);
        }
    }
}
