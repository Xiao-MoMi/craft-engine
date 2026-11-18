package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ChainParameterSource;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ListUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

record ItemDataParameters(Item item, boolean legacy) implements ChainParameterSource {

    static ItemDataParameters components(Item item) {
        return new ItemDataParameters(item, false);
    }

    static ItemDataParameters nbt(Item item) {
        return new ItemDataParameters(item, true);
    }

    @Override
    public <T> Optional<T> getParameter(ContextKey<T> key) {
        String node = key.node();
        int firstIndex = node.indexOf('[');
        String root = firstIndex < 0 ? node : node.substring(0, firstIndex);
        Object data = this.legacy ? this.item.getTagAsJava(root) : this.item.getComponentAsJava(root);
        return value(data, node, firstIndex);
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> value(Object data, String node, int firstIndex) {
        if (firstIndex >= 0) {
            data = ListUtils.getIndexed(data, node, firstIndex);
        }
        if (data instanceof Map<?, ?> || data instanceof List<?>) {
            return Optional.of((T) new NestedValue(data));
        }
        return Optional.ofNullable((T) data);
    }

    private record NestedValue(Object data) implements ChainParameterSource {

        @Override
        public <T> Optional<T> getParameter(ContextKey<T> key) {
            String node = key.node();
            int firstIndex = node.indexOf('[');
            if (firstIndex == 0) {
                return value(this.data, node, firstIndex);
            }
            if (this.data instanceof Map<?, ?> map) {
                String field = firstIndex < 0 ? node : node.substring(0, firstIndex);
                return value(map.get(field), node, firstIndex);
            }
            return Optional.empty();
        }

        @Override
        public String toString() {
            return GsonHelper.get().toJson(this.data);
        }
    }
}
