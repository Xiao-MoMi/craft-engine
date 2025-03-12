/*
 * Copyright (C) <2025> <XiaoMoMi>
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

package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.util.Key;

public class SoundUtils {

    private SoundUtils() {}

    public static Object toSoundType(BlockSounds sounds) throws ReflectiveOperationException {
        return Reflections.constructor$SoundType.newInstance(
            1f, 1f,
                getOrRegisterSoundEvent(sounds.breakSound()),
                getOrRegisterSoundEvent(sounds.stepSound()),
                getOrRegisterSoundEvent(sounds.placeSound()),
                getOrRegisterSoundEvent(sounds.hitSound()),
                getOrRegisterSoundEvent(sounds.fallSound())
        );
    }

    public static Object getOrRegisterSoundEvent(Key key) throws ReflectiveOperationException {
        return Reflections.method$SoundEvent$createVariableRangeEvent.invoke(null,
                Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, key.namespace(), key.value())
        );
    }
}
