package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("CraftEngine Item")
@Description({"Get CraftEngine items."})
@Since("1.0")
public class ExprCustomItem extends SimpleExpression<ItemType> {

    public static void register() {
        Skript.registerExpression(ExprCustomItem.class, ItemType.class, ExpressionType.SIMPLE, "[(the|a)] (custom|ce|craft-engine) item [with [namespace] id] %strings%");
    }

    private Expression<?> itemIds;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        itemIds = exprs[0];
        return true;
    }

    @Override
    @Nullable
    protected ItemType[] get(Event event) {
        Object[] objects = itemIds.getArray(event);
        List<ItemType> items = new ArrayList<>();

        for (Object object : objects) {
            if (object instanceof String string) {
                CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of(string));
                if (customItem != null) {
                    ItemType itemType = new ItemType(customItem.buildItemStack(ItemBuildContext.EMPTY));
                    items.add(itemType);
                }
            }
        }

        return items.toArray(new ItemType[0]);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "craft-engine item with id " + itemIds.toString(e, debug);
    }
}
