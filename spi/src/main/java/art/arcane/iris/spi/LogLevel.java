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

/**
 * Severity levels for platform-routed log messages. Adapters map these onto the host logger; the unbound
 * fallback in {@link IrisLogging} sends {@link #WARN} and {@link #ERROR} to {@code System.err} and the rest to
 * {@code System.out}.
 * <p>
 * Constants may be added. Switch expressions over this enum need a {@code default} arm.
 */
public enum LogLevel {
    /**
     * Diagnostic detail. Adapters route it to the host logger's own debug channel unless Iris debug logging is
     * enabled, so it is normally invisible in server output.
     */
    DEBUG,
    /** Normal operational messages. */
    INFO,
    /**
     * Lifecycle milestones an operator reads the server log to find. Not a problem, but adapters route it to
     * the host logger rather than to a console sender, so it survives into the log file the server writes.
     * Reserved for a handful of events per boot.
     */
    NOTICE,
    /** Recoverable problems and misconfiguration. */
    WARN,
    /** Failures; usually paired with {@link IrisPlatform#reportError(Throwable)}. */
    ERROR
}
