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

package net.momirealms.craftengine.core.plugin.locale;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientLangMangerImpl implements ClientLangManager {
    private final Plugin plugin;
    private final Map<String, I18NData> i18nData = new HashMap<>();

    public ClientLangMangerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        this.i18nData.clear();
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        I18NData data = this.i18nData.computeIfAbsent(id.value(), k -> new I18NData());
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String key = entry.getKey();
            data.addTranslation(key, entry.getValue().toString());
        }
    }

    @Override
    public Map<String, I18NData> langData() {
        return Collections.unmodifiableMap(i18nData);
    }
}
