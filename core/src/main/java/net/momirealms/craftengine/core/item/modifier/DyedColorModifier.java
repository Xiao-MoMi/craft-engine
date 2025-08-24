package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class DyedColorModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Color color;

    public DyedColorModifier(Color color) {
        this.color = color;
    }

    public Color dyedColor() {
        return color;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.DYED_COLOR;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        return item.dyedColor(this.color);
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.DYED_COLOR;
    }

    @Override
    public @Nullable Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"display", "color"};
    }

    @Override
    public String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.color";
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            if (arg instanceof Integer integer) {
                return new DyedColorModifier<>(Color.fromDecimal(integer));
            } else {
                Vector3f vector3f = MiscUtils.getAsVector3f(arg, "dyed-color");
                return new DyedColorModifier<>(Color.fromVector3f(vector3f));
            }
        }
    }
}
