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

package net.momirealms.craftengine.core.plugin.dependency;

import com.google.gson.JsonElement;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyRegistry {
    private static final Set<String> DO_NOT_AUTO_LOAD = Stream.of(
            Dependencies.ASM, Dependencies.ASM_COMMONS, Dependencies.JAR_RELOCATOR, Dependencies.ZSTD
    ).map(Dependency::id).collect(Collectors.toSet());

    private static final String GROUP_ID = "net.momirealms";

    public boolean shouldAutoLoad(Dependency dependency) {
        return !DO_NOT_AUTO_LOAD.contains(dependency.id());
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean isGsonRelocated() {
        return JsonElement.class.getName().startsWith(GROUP_ID);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean slf4jPresent() {
        return classExists("org.slf4j.Logger") && classExists("org.slf4j.LoggerFactory");
    }
}
