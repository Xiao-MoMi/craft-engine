package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.TypeUtils;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

public class TagsModifier<I> implements ItemDataModifier<I> {
    private final Map<String, Object> arguments;

    public TagsModifier(Map<String, Object> arguments) {
        this.arguments = mapToMap(arguments);
    }

    public Map<String, Object> arguments() {
        return arguments;
    }

    @Override
    public String name() {
        return "tags";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            item.setTag(value, key);
        }
        return item;
    }

    // TODO NOT PERFECT
    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
            Tag previous = item.getTag(entry.getKey());
            if (previous != null) {
                networkData.put(entry.getKey(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(entry.getKey(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
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
