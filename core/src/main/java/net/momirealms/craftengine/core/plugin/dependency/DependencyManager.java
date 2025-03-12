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

import java.util.Collection;
import java.util.Set;

/**
 * Loads and manages runtime dependencies for the plugin.
 */
public interface DependencyManager extends AutoCloseable {

    /**
     * Loads dependencies.
     *
     * @param dependencies the dependencies to load
     */
    void loadDependencies(Collection<Dependency> dependencies);

    /**
     * Obtains an isolated classloader containing the given dependencies.
     *
     * @param dependencies the dependencies
     * @return the classloader
     */
    ClassLoader obtainClassLoaderWith(Set<Dependency> dependencies);

    @Override
    void close();
}
