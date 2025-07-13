package net.momirealms.craftengine.core.plugin.locale;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class I18NData {
    private static final Map<String, Function<String, List<String>>> LANG_KEY_PROCESSORS = new HashMap<>();
    public Map<String, String> translations = new HashMap<>();

    static {
        LANG_KEY_PROCESSORS.put("block_name", (id) -> {
            if (id.contains("[") && id.contains("]")) {
                ImmutableBlockState parsed = BlockStateParser.deserialize(id);
                if (parsed == null) return List.of(id);
                return List.of("block." + stateToRealBlockId(parsed));
            } else {
                Key blockId = Key.of(id);
                Optional<CustomBlock> blockOptional = CraftEngine.instance().blockManager().blockById(blockId);
                if (blockOptional.isPresent()) {
                    List<ImmutableBlockState> states = blockOptional.get().variantProvider().states();
                    if (states.size() == 1) {
                        return List.of("block." + stateToRealBlockId(states.get(0)));
                    } else {
                        ArrayList<String> processed = new ArrayList<>();
                        for (ImmutableBlockState state : states) {
                            processed.add("block." + stateToRealBlockId(state));
                        }
                        return processed;
                    }
                } else {
                    return List.of(id);
                }
            }
        });
    }

    public void processTranslations() {
        Map<String, String> temp = new HashMap<>(Math.max(10, this.translations.size()));
        for (Map.Entry<String, String> entry : this.translations.entrySet()) {
            String key = entry.getKey();
            String[] split = key.split(":", 2);
            if (split.length == 2) {
                Optional.ofNullable(LANG_KEY_PROCESSORS.get(split[0]))
                        .ifPresentOrElse(processor -> {
                                    for (String result : processor.apply(split[1])) {
                                        temp.put(result, entry.getValue());
                                    }
                                },
                                () -> CraftEngine.instance().logger().warn("Unknown lang type: " + key)
                        );
            } else {
                temp.put(key, entry.getValue());
            }
        }
        this.translations = temp;
    }

    public void addTranslations(Map<String, String> data) {
        this.translations.putAll(data);
    }

    public void addTranslation(String key, String value) {
        this.translations.put(key, value);
    }

    @Nullable
    public String translate(String key) {
        return this.translations.get(key);
    }

    @Override
    public String toString() {
        return "I18NData{" + translations + "}";
    }

    public static void merge(Map<String, I18NData> target, Map<String, I18NData> source) {
        source.forEach((key, value) -> {
            I18NData copy = new I18NData();
            copy.addTranslations(value.translations);
            target.merge(key, copy, (existing, newData) -> {
                existing.addTranslations(newData.translations);
                return existing;
            });
        });
    }

    private static String stateToRealBlockId(ImmutableBlockState state) {
        String id = state.customBlockState().handle().toString();
        int first = -1, last = -1;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '{' && first == -1) {
                first = i;
            } else if (c == '}') {
                last = i;
            }
        }
        if (first == -1 || last == -1 || last <= first) {
            throw new IllegalArgumentException("Invalid block state: " + id);
        }
        int length = last - first - 1;
        char[] chars = new char[length];
        id.getChars(first + 1, last, chars, 0);
        for (int i = 0; i < length; i++) {
            if (chars[i] == ':') {
                chars[i] = '.';
            }
        }
        return new String(chars);
    }
}
