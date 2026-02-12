package net.momirealms.craftengine.core.plugin.locale;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class LangData {
    private static final Map<String, Function<String, List<String>>> LANG_KEY_PROCESSORS = new HashMap<>();
    public Map<String, String> translations = new LinkedHashMap<>();

    static {
        LANG_KEY_PROCESSORS.put("block_name", (id) -> {
            Key blockId = Key.of(id);
            Optional<CustomBlock> blockOptional = CraftEngine.instance().blockManager().blockById(blockId);
            if (blockOptional.isPresent() && Config.generateModAssets()) {
                List<String> keys = new ArrayList<>();
                List<ImmutableBlockState> states = blockOptional.get().variantProvider().states();
                if (states.size() == 1) {
                    keys.add("block." + stateToRealBlockId(states.getFirst()));
                } else {
                    for (ImmutableBlockState state : states) {
                        keys.add("block." + stateToRealBlockId(state));
                    }
                }
                keys.add("block." + id.replace(":", "."));
                return keys;
            }
            return List.of("block." + id.replace(":", "."));
        });
    }

    public void processTranslations() {
        Map<String, String> temp = new LinkedHashMap<>(Math.max(10, this.translations.size()));
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
        return "LangData{" + translations + "}";
    }

    public static void merge(Map<String, LangData> target, Map<String, LangData> source) {
        source.forEach((key, value) -> {
            LangData copy = new LangData();
            copy.addTranslations(value.translations);
            target.merge(key, copy, (existing, newData) -> {
                existing.addTranslations(newData.translations);
                return existing;
            });
        });
    }

    private static String stateToRealBlockId(ImmutableBlockState state) {
        String id = state.customBlockState().literalObject().toString();
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
