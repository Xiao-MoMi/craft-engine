package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.config.ResourceException;
import net.momirealms.craftengine.core.plugin.config.UnknownResourceException;
import net.momirealms.sparrow.yaml.node.SectionNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class CollectingConfigReader {
    private final Map<String, Object> values;
    private final Path file;
    private final List<ResourceException> errors;
    private final Set<String> invalidSections = new HashSet<>();

    public CollectingConfigReader(SectionNode node, Path file, List<ResourceException> errors) {
        this.values = node.getValues();
        this.file = file;
        this.errors = errors;
    }

    public <T> T attempt(String path, Supplier<T> parser, T fallback) {
        try {
            return parser.get();
        } catch (KnownResourceException e) {
            e.setFilePath(this.file);
            this.errors.add(e);
        } catch (RuntimeException e) {
            this.errors.add(new UnknownResourceException(this.file, path, e));
        }
        return fallback;
    }

    public void error(String path, String translationKey, String... arguments) {
        this.errors.add(new KnownResourceException(this.file, translationKey, path, arguments));
    }

    public <T> T read(String path, Function<ConfigValue, T> parser, T fallback) {
        return this.attempt(path, () -> {
            Object value = this.values;
            int start = 0;
            while (true) {
                if (!(value instanceof Map<?, ?> section)) {
                    String parent = path.substring(0, start - 1);
                    if (this.invalidSections.add(parent)) {
                        this.error(parent, ConfigConstants.PARSE_SECTION_FAILED, value.getClass().getSimpleName());
                    }
                    return fallback;
                }
                int end = path.indexOf('.', start);
                value = section.get(end == -1 ? path.substring(start) : path.substring(start, end));
                if (value == null) {
                    return fallback;
                }
                if (end == -1) {
                    return parser.apply(ConfigValue.of(path, value));
                }
                start = end + 1;
            }
        }, fallback);
    }

    public String getString(String path) {
        return this.getString(path, null);
    }

    public <T> T read(ConfigValue value, Function<ConfigValue, T> parser, T fallback) {
        return value == null ? fallback : this.attempt(value.path(), () -> parser.apply(value), fallback);
    }

    public String getString(String path, String fallback) {
        return this.read(path, CollectingConfigReader::string, fallback);
    }

    public boolean getBoolean(String path, boolean fallback) {
        return this.read(path, ConfigValue::getAsBoolean, fallback);
    }

    public int getInt(String path, int fallback) {
        return this.read(path, ConfigValue::getAsInt, fallback);
    }

    public int getInt(String path, int fallback, int min, int max) {
        int value = this.getInt(path, fallback);
        if (value < min) {
            this.error(path, "number.no_less_than", Integer.toString(value), Integer.toString(min));
            return fallback;
        }
        if (value > max) {
            this.error(path, "number.no_greater_than", Integer.toString(value), Integer.toString(max));
            return fallback;
        }
        return value;
    }

    public long getLong(String path, long fallback) {
        return this.read(path, ConfigValue::getAsLong, fallback);
    }

    public double getDouble(String path, double fallback) {
        return this.read(path, ConfigValue::getAsDouble, fallback);
    }

    public <T extends Enum<T>> T getEnum(String path, Class<T> type, T fallback) {
        return this.read(path, value -> value.getAsEnum(type), fallback);
    }

    public Key getKey(String path, String fallback) {
        return this.read(path, CollectingConfigReader::identifier, Key.of(fallback));
    }

    public Map<String, Object> getSection(String path) {
        return this.read(path, value -> {
            if (!(value.value() instanceof Map<?, ?>)) {
                throw new KnownResourceException(ConfigConstants.PARSE_SECTION_FAILED, path, value.value().getClass().getSimpleName());
            }
            return value.getAsSection().values();
        }, Map.of());
    }

    public int getClampedInt(String path, int fallback, int min, int max) {
        int value = this.getInt(path, fallback);
        if (value < min) {
            this.error(path, "number.no_less_than", Integer.toString(value), Integer.toString(min));
        } else if (value > max) {
            this.error(path, "number.no_greater_than", Integer.toString(value), Integer.toString(max));
        }
        return Math.clamp(value, min, max);
    }

    public double getClampedDouble(String path, double fallback, double min) {
        double value = this.getDouble(path, fallback);
        if (value < min) {
            this.error(path, "number.no_less_than", Double.toString(value), Double.toString(min));
        }
        return Math.max(value, min);
    }

    public List<String> getStringList(String path) {
        return this.getList(path, CollectingConfigReader::string);
    }

    public <T> List<T> getList(String path, Function<ConfigValue, T> parser) {
        return this.read(path, value -> this.getList(value, parser), List.of());
    }

    public <T> List<T> getList(ConfigValue source, Function<ConfigValue, T> parser) {
        if (source == null) {
            return List.of();
        }
        List<?> values = this.attempt(source.path(), () -> {
            if (!(source.value() instanceof List<?> list)) {
                throw new KnownResourceException("config.expected_list", source.path());
            }
            return list;
        }, List.of());
        List<T> result = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            ConfigValue value = ConfigValue.of(source.path() + "[" + i + "]", values.get(i));
            T parsed = this.attempt(value.path(), () -> parser.apply(value), null);
            if (parsed != null) {
                result.add(parsed);
            }
        }
        return List.copyOf(result);
    }

    public List<Pattern> getPatterns(String path) {
        return this.getList(path, value -> {
            String regex = string(value);
            try {
                return Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                throw new KnownResourceException("config.invalid_regex", value.path(), regex);
            }
        });
    }

    public Map<Key, Integer> getKeyIntMap(String path) {
        Map<Key, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : this.getSection(path).entrySet()) {
            String entryPath = path + "." + entry.getKey();
            Key key = this.attempt(entryPath, () -> ConfigValue.of(entryPath, entry.getKey()).getAsIdentifier(), null);
            Integer value = this.attempt(entryPath, () -> {
                if (entry.getValue() == null) {
                    throw new KnownResourceException(ConfigConstants.PARSE_INT_FAILED, entryPath, "null");
                }
                return ConfigValue.of(entryPath, entry.getValue()).getAsInt();
            }, null);
            if (key != null && value != null) {
                result.put(key, value);
            }
        }
        return Map.copyOf(result);
    }

    public static String string(ConfigValue value) {
        Object raw = value.value();
        if (raw == null || raw instanceof Map<?, ?> || raw instanceof List<?>) {
            throw new KnownResourceException("config.expected_scalar", value.path());
        }
        return value.getAsString();
    }

    public static Key identifier(ConfigValue value) {
        string(value);
        return value.getAsIdentifier();
    }
}
