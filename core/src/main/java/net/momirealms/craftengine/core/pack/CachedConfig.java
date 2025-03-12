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

package net.momirealms.craftengine.core.pack;

import java.nio.file.Path;
import java.util.Map;

public class CachedConfig {
    private final Pack pack;
    private final Path filePath;
    private final Map<String, Object> config;

    public CachedConfig(Map<String, Object> config, Path filePath, Pack pack) {
        this.config = config;
        this.filePath = filePath;
        this.pack = pack;
    }

    public Map<String, Object> config() {
        return config;
    }

    public Path filePath() {
        return filePath;
    }

    public Pack pack() {
        return pack;
    }
}
