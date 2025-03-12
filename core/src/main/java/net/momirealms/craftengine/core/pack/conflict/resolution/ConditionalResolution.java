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

package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.pack.conflict.matcher.PathMatcher;
import net.momirealms.craftengine.core.pack.conflict.matcher.PathMatchers;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.nio.file.Path;
import java.util.Map;

public record ConditionalResolution(PathMatcher matcher, Resolution resolution) implements Resolution {
    public static final Factory FACTORY = new Factory();

    @Override
    public void run(Path existing, Path conflict) {
        if (this.matcher.test(existing)) {
            this.resolution.run(existing, conflict);
        }
    }

    @Override
    public Key type() {
        return Resolutions.CONDITIONAL;
    }

    public static class Factory implements ResolutionFactory {

        @Override
        public ConditionalResolution create(Map<String, Object> arguments) {
            Map<String, Object> term = MiscUtils.castToMap(arguments.get("term"), false);
            Map<String, Object> resolution = MiscUtils.castToMap(arguments.get("resolution"), false);
            return new ConditionalResolution(PathMatchers.fromMap(term), Resolutions.fromMap(resolution));
        }
    }
}
