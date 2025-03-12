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

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.SectionPos;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SectionPosUtils {

    private SectionPosUtils() {}

    public static Map<Long, BitSet> nearbySectionsMap(Set<SectionPos> sections, int minLightSection, int maxLightSection) {
        Map<Long, BitSet> nearby = new Long2ObjectOpenHashMap<>();
        for (SectionPos section : sections) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    long chunkPos = ChunkPos.asLong(section.x() + i, section.z() + j);
                    BitSet posSet = nearby.computeIfAbsent(chunkPos, k -> new BitSet(maxLightSection - minLightSection));
                    for (int k = -1; k <= 1; k++) {
                        int y = section.y() + k;
                        if (y >= minLightSection && y <= maxLightSection) {
                            posSet.set(y - minLightSection);
                        }
                    }
                }
            }
        }
        return nearby;
    }

    public static Map<Long, BitSet> toMap(Set<SectionPos> sections, int minLightSection, int maxLightSection) {
        int nBits = maxLightSection - minLightSection;
        Map<Long, BitSet> nearby = new Long2ObjectOpenHashMap<>(Math.max(8, sections.size() / 2), 0.5f);
        for (SectionPos section : sections) {
            long chunkPos = ChunkPos.asLong(section.x(), section.z());
            BitSet posSet = nearby.computeIfAbsent(chunkPos, k -> new BitSet(nBits));
            int y = section.y();
            if (y >= minLightSection && y <= maxLightSection) {
                posSet.set(y - minLightSection);
            }
        }
        return nearby;
    }

    public static Set<SectionPos> calculateAffectedRegions(int sx, int sy, int sz, int x) {
        Set<SectionPos> regions = new HashSet<>();
        int rxStart = (sx - x) >> 4;
        int rxEnd = (sx + x) >> 4;
        int ryStart = (sy - x) >> 4;
        int ryEnd = (sy + x) >> 4;
        int rzStart = (sz - x) >> 4;
        int rzEnd = (sz + x) >> 4;
        for (int rx = rxStart; rx <= rxEnd; rx++) {
            for (int ry = ryStart; ry <= ryEnd; ry++) {
                for (int rz = rzStart; rz <= rzEnd; rz++) {
                    int xMin = rx << 4;
                    int xMax = xMin + 15;
                    int dxMin = calculateMinDistance(sx, xMin, xMax);
                    int yMin = ry << 4;
                    int yMax = yMin + 15;
                    int dyMin = calculateMinDistance(sy, yMin, yMax);
                    int zMin = rz << 4;
                    int zMax = zMin + 15;
                    int dzMin = calculateMinDistance(sz, zMin, zMax);
                    if (dxMin + dyMin + dzMin <= x) {
                        regions.add(new SectionPos(rx, ry, rz));
                    }
                }
            }
        }
        return regions;
    }

    private static int calculateMinDistance(int coord, int min, int max) {
        if (coord < min) {
            return min - coord;
        } else if (coord > max) {
            return coord - max;
        } else {
            return 0;
        }
    }
}
