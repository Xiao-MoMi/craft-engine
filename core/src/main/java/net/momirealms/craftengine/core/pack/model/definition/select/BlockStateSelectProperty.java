package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import java.util.Objects;

public final class BlockStateSelectProperty implements SelectProperty {
    public static final SelectPropertyFactory<BlockStateSelectProperty> FACTORY = new Factory();
    public static final SelectPropertyReader<BlockStateSelectProperty> READER = new Reader();
    private final String blockStateProperty;

    public BlockStateSelectProperty(String blockStateProperty) {
        this.blockStateProperty = blockStateProperty;
    }

    public String blockStateProperty() {
        return this.blockStateProperty;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "block_state");
        model.addProperty("block_state_property", this.blockStateProperty);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BlockStateSelectProperty that)) {
            return false;
        }
        return Objects.equals(this.blockStateProperty, that.blockStateProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.blockStateProperty);
    }

    private static class Factory implements SelectPropertyFactory<BlockStateSelectProperty> {
        private static final String[] BLOCK_STATE_PROPERTY = ConfigKeys.of("block_state_property");

        @Override
        public BlockStateSelectProperty create(ConfigSection section) {
            return new BlockStateSelectProperty(section.getNonNullString(BLOCK_STATE_PROPERTY));
        }
    }

    private static class Reader implements SelectPropertyReader<BlockStateSelectProperty> {
        @Override
        public BlockStateSelectProperty read(JsonObject json) {
            return new BlockStateSelectProperty(json.get("block_state_property").getAsString());
        }
    }
}
