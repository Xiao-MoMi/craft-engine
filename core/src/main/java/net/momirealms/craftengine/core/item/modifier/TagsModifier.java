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

package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.TypeUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class TagsModifier<I> implements ItemModifier<I> {
    private final Map<String, Object> arguments;

    public TagsModifier(Map<String, Object> arguments) {
        this.arguments = mapToMap(arguments);
    }

    @Override
    public String name() {
        return "tags";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            item.setTag(value, key);
        }
    }

    private static Map<String, Object> mapToMap(final Map<String, Object> source) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        recursiveMapProcessing(source, resultMap);
        return resultMap;
    }

    private static void recursiveMapProcessing(
            final Map<String, Object> sourceMap,
            final Map<String, Object> targetMap
    ) {
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            processMapEntry(entry.getKey(), entry.getValue(), targetMap);
        }
    }

    private static void processMapEntry(
            final String key,
            final Object value,
            final Map<String, Object> targetMap
    ) {
        if (value instanceof Map) {
            handleNestedMap(key, MiscUtils.castToMap(value, false), targetMap);
        } else if (value instanceof String) {
            handleStringValue(key, (String) value, targetMap);
        } else {
            targetMap.put(key, value);
        }
    }

    private static void handleNestedMap(
            final String key,
            final Map<String, Object> nestedSource,
            final Map<String, Object> parentMap
    ) {
        Map<String, Object> nestedTarget = new LinkedHashMap<>();
        parentMap.put(key, nestedTarget);
        recursiveMapProcessing(nestedSource, nestedTarget);
    }

    private static void handleStringValue(
            final String key,
            final String value,
            final Map<String, Object> targetMap
    ) {
        ParsedValue parsed = tryParseTypedValue(value);
        targetMap.put(key, parsed.success ? parsed.result : value);
    }

    private static ParsedValue tryParseTypedValue(final String str) {
        if (str.length() < 3 || str.charAt(0) != '(') {
            return ParsedValue.FAILURE;
        }

        int closingBracketPos = str.indexOf(')', 1);
        if (closingBracketPos == -1 || closingBracketPos + 2 > str.length()) {
            return ParsedValue.FAILURE;
        }

        if (str.charAt(closingBracketPos + 1) != ' ') {
            return ParsedValue.FAILURE;
        }

        String typeMarker = str.substring(1, closingBracketPos);
        String content = str.substring(closingBracketPos + 2);
        return new ParsedValue(
                true,
                TypeUtils.castBasicTypes(content, typeMarker)
        );
    }

    private record ParsedValue(boolean success, Object result) {
            static final ParsedValue FAILURE = new ParsedValue(false, null);
    }
}
