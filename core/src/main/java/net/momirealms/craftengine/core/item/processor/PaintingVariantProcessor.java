package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class PaintingVariantProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<PaintingVariantProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[]{"EntityTag", "variant"};
    public final String id;

    public PaintingVariantProcessor(String id) {
        this.id = id;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (VersionHelper.isOrAbove1_21_5) {
            item.setJavaComponent(DataComponentKeys.PAINTING_VARIANT, this.id);
        } else if (VersionHelper.isOrAbove1_20_5) {
            item.setJavaComponent(DataComponentKeys.ENTITY_DATA, Map.of(
                    "id", "minecraft:painting",
                    "variant", this.id
            ));
        } else {
            item.setJavaTag(this.id, NBT_PATH);
        }
        return item;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return VersionHelper.isOrAbove1_21_5 ? DataComponentKeys.PAINTING_VARIANT : DataComponentKeys.ENTITY_DATA;
    }

    @Override
    public @Nullable Object[] nbtPath(Item item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "EntityTag.variant";
    }

    private static class Factory implements ItemProcessorFactory<PaintingVariantProcessor> {

        @Override
        public PaintingVariantProcessor create(ConfigValue value) {
            return new PaintingVariantProcessor(value.getAsIdentifier().asString());
        }
    }
}
