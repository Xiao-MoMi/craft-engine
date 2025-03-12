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

package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;

import java.util.Map;
import java.util.regex.Pattern;

public interface TemplateManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "templates";
    Pattern PATTERN = Pattern.compile("\\{[^{}]+}");
    String TEMPLATE = "template";
    String OVERRIDES = "overrides";
    String ARGUMENTS = "arguments";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    Map<String, Object> applyTemplates(Map<String, Object> input);

    default int loadingSequence() {
        return LoadingSequence.TEMPLATE;
    }
}
