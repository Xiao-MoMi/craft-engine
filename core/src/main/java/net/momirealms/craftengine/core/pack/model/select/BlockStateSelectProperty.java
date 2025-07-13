package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

public class BlockStateSelectProperty implements SelectProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String blockStateProperty;

    public BlockStateSelectProperty(String blockStateProperty) {
        this.blockStateProperty = blockStateProperty;
    }

    @Override
    public Key type() {
        return SelectProperties.BLOCK_STATE;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("block_state_property", blockStateProperty);
    }

    public static class Factory implements SelectPropertyFactory {
        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            String property = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("block-state-property"), "warning.config.item.model.select.block_state.missing_property");
            return new BlockStateSelectProperty(property);
        }
    }

    public static class Reader implements SelectPropertyReader {
        @Override
        public SelectProperty read(JsonObject json) {
            String property = json.get("block_state_property").getAsString();
            return new BlockStateSelectProperty(property);
        }
    }
}
