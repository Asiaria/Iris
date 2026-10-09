/*
 * Iris is a World Generator for Minecraft Bukkit Servers
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

package art.arcane.iris.spi;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide registry mapping a service interface to the single implementation the active adapter
 * installed for it. Core resolves platform-provided collaborators through here instead of importing them.
 * <p>
 * Backed by a {@link ConcurrentHashMap}; every method is safe from any thread. Registration happens during
 * adapter startup and removal during shutdown, so a lookup racing a shutdown can legitimately miss - resolve
 * lazily at the point of use rather than caching, and prefer {@link #getOrNull(Class)} on optional paths.
 * <p>
 * Internal to Iris; not a published integration surface.
 */
public final class IrisServices {
    private static final ConcurrentHashMap<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    private IrisServices() {
    }

    /**
     * Binds {@code implementation} as the provider for {@code type}, replacing any previous binding.
     * <p>
     * Both parameters are untyped rather than a generic {@code <T> (Class<T>, T)} pair on purpose: callers
     * register from wildcard-typed loops - {@code Class<? extends IrisService>} paired with an
     * {@code IrisService} instance - and from maps whose value type is the supertype, neither of which a
     * generic signature accepts without casts at every call site. The pairing is enforced at runtime by
     * {@link Class#cast(Object)}, which throws {@link ClassCastException} on a mismatch, so a wrong pairing
     * fails at registration rather than at first use. Null arguments throw.
     */
    public static void register(Class<?> type, Object implementation) {
        SERVICES.put(type, type.cast(implementation));
    }

    /**
     * The provider registered for {@code type}. Never returns null.
     *
     * @throws IllegalStateException if nothing is registered for {@code type}
     */
    public static <T> T get(Class<T> type) {
        Object implementation = SERVICES.get(type);
        if (implementation == null) {
            throw new IllegalStateException("No Iris service registered for " + type.getName());
        }
        return type.cast(implementation);
    }

    /**
     * The provider registered for {@code type}, or null when nothing is registered.
     */
    public static <T> T getOrNull(Class<T> type) {
        Object implementation = SERVICES.get(type);
        return implementation == null ? null : type.cast(implementation);
    }

    /**
     * Unbinds {@code type}. No-op when nothing is registered.
     */
    public static void remove(Class<?> type) {
        SERVICES.remove(type);
    }

    /**
     * Unbinds every service. Called on adapter shutdown and between tests.
     */
    public static void clear() {
        SERVICES.clear();
    }
}
