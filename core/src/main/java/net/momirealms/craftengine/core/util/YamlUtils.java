package net.momirealms.craftengine.core.util;

import net.momirealms.sparrow.yaml.node.ParentNode;
import net.momirealms.sparrow.yaml.node.SectionNode;
import net.momirealms.sparrow.yaml.node.SequenceNode;
import net.momirealms.sparrow.yaml.node.YamlNode;
import net.momirealms.sparrow.yaml.route.Route;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class YamlUtils {

    private YamlUtils() {
    }

    /**
     * Adapts CraftEngine's dot-separated configuration paths to Sparrow YAML routes.
     */
    public static Route route(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isEmpty()) {
            throw new IllegalArgumentException("YAML path cannot be empty");
        }

        return Route.from((Object[]) StringUtils.split(path, '.'));
    }

    public static Reader reader(ParentNode<?> node) {
        return new Reader(node);
    }

    /**
     * Typed reader that preserves the dot-separated path convention used by CraftEngine's
     * configuration files while delegating parsing to Sparrow YAML serializers.
     */
    public static final class Reader {
        private final ParentNode<?> node;

        private Reader(ParentNode<?> node) {
            this.node = Objects.requireNonNull(node, "node");
        }

        @Nullable
        public String getString(String path) {
            return get(String.class, null, path);
        }

        public String getString(String path, String defaultValue) {
            return get(String.class, defaultValue, path);
        }

        public boolean getBoolean(String path) {
            return getBoolean(path, false);
        }

        public boolean getBoolean(String path, boolean defaultValue) {
            return get(Boolean.class, defaultValue, path);
        }

        public int getInt(String path) {
            return getInt(path, 0);
        }

        public int getInt(String path, int defaultValue) {
            return get(Integer.class, defaultValue, path);
        }

        public long getLong(String path, long defaultValue) {
            return get(Long.class, defaultValue, path);
        }

        public double getDouble(String path, double defaultValue) {
            return get(Double.class, defaultValue, path);
        }

        public List<String> getStringList(String path) {
            SequenceNode sequence = this.node.getSequenceOrNull(route(path));
            if (sequence == null) {
                return List.of();
            }
            List<Object> values = sequence.getValues();
            List<String> result = new ArrayList<>(values.size());
            for (Object value : values) {
                result.add(String.valueOf(value));
            }
            return result;
        }

        @Nullable
        public List<?> getList(String path) {
            SequenceNode sequence = this.node.getSequenceOrNull(route(path));
            return sequence == null ? null : sequence.getValues();
        }

        @Nullable
        public List<Map<?, ?>> getMapList(String path) {
            List<?> values = getList(path);
            if (values == null) {
                return null;
            }
            List<Map<?, ?>> result = new ArrayList<>(values.size());
            for (Object value : values) {
                if (value instanceof Map<?, ?> map) {
                    result.add(map);
                }
            }
            return result;
        }

        public boolean contains(String path) {
            return this.node.getNodeOrNull(route(path)) != null;
        }

        @Nullable
        public SectionNode getSection(String path) {
            return this.node.getSectionOrNull(route(path));
        }

        @Nullable
        public Object getValue(String path) {
            YamlNode<?> yamlNode = this.node.getNodeOrNull(route(path));
            return yamlNode == null ? null : yamlNode.representValue();
        }

        @Nullable
        public Object get(String path, @Nullable Object defaultValue) {
            return this.node.getOrDefault(Object.class, defaultValue, route(path).routeKeys());
        }

        private <T> T get(Class<T> type, T defaultValue, String path) {
            return this.node.getOrDefault(type, defaultValue, route(path).routeKeys());
        }
    }
}
