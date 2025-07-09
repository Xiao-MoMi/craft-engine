package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ExprItemCustomItemID extends SimplePropertyExpression<Object, String> {

    public static void register() {
        register(ExprItemCustomItemID.class, String.class, "custom item id", "itemstacks/itemtypes");
    }

    @Override
    public @Nullable String convert(Object object) {
        if (object instanceof ItemStack itemStack)
            return Optional.ofNullable(CraftEngineItems.byItemStack(itemStack)).map(it -> it.id().toString()).orElse(null);
        if (object instanceof ItemType itemType) {
            ItemStack itemStack = new ItemStack(itemType.getMaterial());
            itemStack.setItemMeta(itemType.getItemMeta());
            return Optional.ofNullable(CraftEngineItems.byItemStack(itemStack)).map(it -> it.id().toString()).orElse(null);
        }
        return null;
    }

    @Override
    protected String getPropertyName() {
        return "custom item id";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return CollectionUtils.array(String.class);
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Key id = Key.of((String) delta[0]);
        for (Object item : getExpr().getArray(e)) {
            if (item instanceof ItemStack itemStack) {
                Item<ItemStack> item1 = BukkitItemManager.instance().wrap(itemStack);
                Item<ItemStack> item2 = BukkitItemManager.instance().createWrappedItem(id, null);
                item1.merge(item2);
            } else if (item instanceof ItemType itemType) {
                Item<ItemStack> item2 = BukkitItemManager.instance().createWrappedItem(id, null);
                itemType.setItemMeta(item2.getItem().getItemMeta());
            }
        }
    }
}
