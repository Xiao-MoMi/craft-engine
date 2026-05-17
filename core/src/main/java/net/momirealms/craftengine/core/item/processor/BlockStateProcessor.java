package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlockStateWrapper;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class BlockStateProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<BlockStateProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[]{"BlockStateTag"};
    private final LazyReference<Map<String, String>> wrapper;

    public BlockStateProcessor(LazyReference<Map<String, String>> wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        return item.blockState(this.wrapper.get());
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "BlockStateTag";
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.BLOCK_STATE;
    }

    private static class Factory implements ItemProcessorFactory<BlockStateProcessor> {

        @Override
        public BlockStateProcessor create(ConfigValue value) {
            if (value.is(Map.class)) {
                Map<String, String> properties = new HashMap<>();
                for (Map.Entry<String, Object> entry : value.getAsMap().entrySet()) {
                    properties.put(entry.getKey(), entry.getValue().toString());
                }
                return new BlockStateProcessor(LazyReference.lazyReference(() -> properties));
            } else {
                String blockStateTag = value.getAsString();
                return new BlockStateProcessor(LazyReference.lazyReference(() -> {
                    BlockStateWrapper blockState = CraftEngine.instance().blockManager().createBlockState(blockStateTag);
                    if (blockState instanceof CustomBlockStateWrapper customBlockStateWrapper) {
                        blockState = customBlockStateWrapper.visualBlockState();
                    }
                    if (blockState != null) {
                        Map<String, String> properties = new HashMap<>(4);
                        for (String property : blockState.getPropertyNames()) {
                            Object propertyValue = blockState.getProperty(property);
                            properties.put(property, String.valueOf(propertyValue).toLowerCase(Locale.ROOT)); // 可能是 Enum
                        }
                        return properties;
                    }
                    return Collections.emptyMap();
                }));
            }
        }
    }
}
