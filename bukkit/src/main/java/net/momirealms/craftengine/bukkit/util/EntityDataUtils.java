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

public class EntityDataUtils {

    private EntityDataUtils() {}

    private static final int HAS_SHADOW = 0x01; // 1
    private static final int IS_SEE_THROUGH = 0x02; // 2
    private static final int USE_DEFAULT_BACKGROUND = 0x04; // 4
    private static final int LEFT_ALIGNMENT = 0x08; // 8
    private static final int RIGHT_ALIGNMENT = 0x10; // 16

    public static byte encodeTextDisplayMask(boolean hasShadow, boolean isSeeThrough, boolean useDefaultBackground, int alignment) {
        int bitMask = 0;
        if (hasShadow) {
            bitMask |= HAS_SHADOW;
        }
        if (isSeeThrough) {
            bitMask |= IS_SEE_THROUGH;
        }
        if (useDefaultBackground) {
            bitMask |= USE_DEFAULT_BACKGROUND;
        }
        switch (alignment) {
            case 0: // CENTER
                break;
            case 1: // LEFT
                bitMask |= LEFT_ALIGNMENT;
                break;
            case 2: // RIGHT
                bitMask |= RIGHT_ALIGNMENT;
                break;
            default:
                throw new IllegalArgumentException("Invalid alignment value");
        }
        return (byte) bitMask;
    }

    private static final int IS_ON_FIRE = 0x01;            // 1
    private static final int IS_CROUCHING = 0x02;          // 2
    private static final int UNUSED = 0x04;                // 4
    private static final int IS_SPRINTING = 0x08;          // 8
    private static final int IS_SWIMMING = 0x10;           // 16
    private static final int IS_INVISIBLE = 0x20;          // 32
    private static final int HAS_GLOWING_EFFECT = 0x40;    // 64
    private static final int IS_FLYING_WITH_ELYTRA = 0x80; // 128

    public static byte encodeCommonMask(boolean isOnFire, boolean isCrouching, boolean isUnused,
                                        boolean isSprinting, boolean isSwimming, boolean isInvisible,
                                        boolean hasGlowingEffect, boolean isFlyingWithElytra) {
        int bitMask = 0;

        if (isOnFire) {
            bitMask |= IS_ON_FIRE;
        }
        if (isCrouching) {
            bitMask |= IS_CROUCHING;
        }
        if (isUnused) {
            bitMask |= UNUSED;
        }
        if (isSprinting) {
            bitMask |= IS_SPRINTING;
        }
        if (isSwimming) {
            bitMask |= IS_SWIMMING;
        }
        if (isInvisible) {
            bitMask |= IS_INVISIBLE;
        }
        if (hasGlowingEffect) {
            bitMask |= HAS_GLOWING_EFFECT;
        }
        if (isFlyingWithElytra) {
            bitMask |= IS_FLYING_WITH_ELYTRA;
        }

        return (byte) bitMask;
    }

    public static boolean isCrouching(byte mask) {
        return (mask & IS_CROUCHING) != 0;
    }
}
