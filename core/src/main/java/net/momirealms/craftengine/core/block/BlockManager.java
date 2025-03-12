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

package net.momirealms.craftengine.core.block;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generator.ModelGenerator;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface BlockManager extends Reloadable, ModelGenerator, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "blocks";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    Collection<ModelGeneration> modelsToGenerate();

    Map<Key, Map<String, JsonElement>> blockOverrides();

    Map<Key, CustomBlock> blocks();

    Optional<CustomBlock> getBlock(Key key);

    Collection<Suggestion> cachedSuggestions();

    Map<Key, Key> soundMapper();

    void initSuggestions();

    void delayedLoad();

    void delayedInit();

    default int loadingSequence() {
        return LoadingSequence.BLOCK;
    }
}
