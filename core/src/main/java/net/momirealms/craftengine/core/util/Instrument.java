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

package net.momirealms.craftengine.core.util;

public enum Instrument {
    HARP("harp"),
    BASEDRUM("basedrum"),
    SNARE("snare"),
    HAT("hat"),
    BASS("bass"),
    FLUTE("flute"),
    BELL("bell"),
    GUITAR("guitar"),
    CHIME("chime"),
    XYLOPHONE("xylophone"),
    IRON_XYLOPHONE("iron_xylophone"),
    COW_BELL("cow_bell"),
    DIDGERIDOO("didgeridoo"),
    BIT("bit"),
    BANJO("banjo"),
    PLING("pling"),
    ZOMBIE("zombie"),
    SKELETON("skeleton"),
    CREEPER("creeper"),
    DRAGON("dragon"),
    WITHER_SKELETON("wither_skeleton"),
    PIGLIN("piglin"),
    CUSTOM_HEAD("custom_head");

    private final String id;

    Instrument(final String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
