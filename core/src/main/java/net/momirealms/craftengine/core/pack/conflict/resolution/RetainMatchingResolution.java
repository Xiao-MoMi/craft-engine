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
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class RetainMatchingResolution implements Resolution {
    public static final Factory FACTORY = new Factory();
    private final PathMatcher matcher;

    public RetainMatchingResolution(PathMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public void run(Path existing, Path conflict) {
        if (this.matcher.test(conflict)) {
            try {
                Files.copy(conflict, existing, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                CraftEngine.instance().logger().warn("Failed to copy conflict file " + conflict + " to " + existing, e);
            }
        }
    }

    @Override
    public Key type() {
        return Resolutions.RETAIN_MATCHING;
    }

    public static class Factory implements ResolutionFactory {

        @Override
        public Resolution create(Map<String, Object> arguments) {
            Map<String, Object> term = MiscUtils.castToMap(arguments.get("term"), false);
            return new RetainMatchingResolution(PathMatchers.fromMap(term));
        }
    }
}
