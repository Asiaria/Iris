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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ModdedEngineEffectsTest {
    @Test
    public void normalizesRegistryKeys() {
        assertNull(ModdedEngineEffects.normalizeRegistryKey(null));
        assertNull(ModdedEngineEffects.normalizeRegistryKey("  "));
        assertEquals("minecraft:block_amethyst_block_chime",
                ModdedEngineEffects.normalizeRegistryKey("BLOCK AMETHYST BLOCK CHIME"));
        assertEquals("example:custom_sound",
                ModdedEngineEffects.normalizeRegistryKey("Example:Custom_Sound"));
    }

    @Test
    public void normalizesLegacyPotionAliases() {
        assertEquals("minecraft:luck", ModdedEngineEffects.normalizePotionEffectKey(null));
        assertEquals("minecraft:slowness", ModdedEngineEffects.normalizePotionEffectKey("SLOW"));
        assertEquals("minecraft:mining_fatigue", ModdedEngineEffects.normalizePotionEffectKey("SLOW_DIGGING"));
        assertEquals("minecraft:instant_health", ModdedEngineEffects.normalizePotionEffectKey("minecraft:HEAL"));
        assertEquals("example:resistance", ModdedEngineEffects.normalizePotionEffectKey("example:DAMAGE_RESISTANCE"));
    }

    @Test
    public void samplesInitiallyAndAfterMovingPastThreshold() {
        assertTrue(ModdedEngineEffects.needsSample(false, 0L, 0.0D));
        assertFalse(ModdedEngineEffects.needsSample(true, 56L, 81.0D));
        assertFalse(ModdedEngineEffects.needsSample(true, 55L, 82.0D));
        assertTrue(ModdedEngineEffects.needsSample(true, 56L, 82.0D));
    }

    @Test
    public void preservesOnlyStrictlyStrongerPotionEffects() {
        assertFalse(ModdedEngineEffects.shouldReplacePotionEffect(3, 2));
        assertTrue(ModdedEngineEffects.shouldReplacePotionEffect(2, 2));
        assertTrue(ModdedEngineEffects.shouldReplacePotionEffect(1, 2));
    }
}
