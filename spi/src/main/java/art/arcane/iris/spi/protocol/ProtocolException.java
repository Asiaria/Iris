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

package art.arcane.iris.spi.protocol;

/**
 * Signals a malformed protocol frame: truncated, over the size cap, or carrying an impossible length prefix.
 * <p>
 * Checked on purpose - a bad frame is an expected condition on a public channel, not a bug, and the handler is
 * meant to drop the frame and carry on rather than propagate. Never used for an unknown message type;
 * {@link IrisMessageCodec#decode(byte[])} returns null for that.
 * <p>
 * Internal to Iris; not a published integration surface.
 */
public class ProtocolException extends Exception {
    /**
     * @param message what was wrong with the frame, including the byte counts involved where relevant
     */
    public ProtocolException(String message) {
        super(message);
    }
}
