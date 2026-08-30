package net.momirealms.craftengine.core.util;

import net.momirealms.sparrow.yaml.node.ParentNode;
import net.momirealms.sparrow.yaml.node.SectionNode;
import net.momirealms.sparrow.yaml.route.Route;
import net.momirealms.sparrow.yaml.serializer.NodeSerializers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class YamlUtils {

    private YamlUtils() {
    }

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

    public static final class Reader {
        private final ParentNode<?> node;

        private Reader(ParentNode<?> node) {
            this.node = Objects.requireNonNull(node, "node");
        }

        @Nullable
        public String getString(String path) {
            return this.node.getString(route(path));
        }

        public String getString(String path, String defaultValue) {
            return this.node.getStringOrDefault(defaultValue, route(path));
        }

        public boolean getBoolean(String path) {
            return this.node.getBoolean(route(path));
        }

        public boolean getBoolean(String path, boolean defaultValue) {
            return this.node.getBooleanOrDefault(defaultValue, route(path));
        }

        public int getInt(String path) {
            return this.node.getInt(route(path));
        }

        public int getInt(String path, int defaultValue) {
            return this.node.getIntOrDefault(defaultValue, route(path));
        }

        public long getLong(String path, long defaultValue) {
            return this.node.getLongOrDefault(defaultValue, route(path));
        }

        public double getDouble(String path, double defaultValue) {
            return this.node.getDoubleOrDefault(defaultValue, route(path));
        }

        public List<String> getStringList(String path) {
            List<String> values = this.node.getStringList(route(path));
            return values == null ? List.of() : values;
        }

        @Nullable
        public List<?> getList(String path) {
            return this.node.getList(NodeSerializers.OBJECT, route(path));
        }

        @Nullable
        public List<Map<?, ?>> getMapList(String path) {
            List<Map<Object, Object>> values = this.node.getMapList(
                    NodeSerializers.OBJECT,
                    NodeSerializers.OBJECT,
                    route(path)
            );
            return values == null ? null : new ArrayList<>(values);
        }

        public boolean contains(String path) {
            return this.node.contains(route(path));
        }

        @Nullable
        public SectionNode getSection(String path) {
            return this.node.getSectionOrNull(route(path));
        }

        @Nullable
        public Object getValue(String path) {
            return this.node.getValue(route(path));
        }

        @Nullable
        public Object get(String path, @Nullable Object defaultValue) {
            return this.node.getOrDefault(defaultValue, NodeSerializers.OBJECT, route(path));
        }
    }
}
