package net.momirealms.craftengine.bukkit.compatibility.skript.condition;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name("Is CraftEngine Item")
@Description({"Checks if the Item is CraftEngine item."})
@Since("1.0")
public class CondIsCustomItem extends Condition {

    public static void register() {
        Skript.registerCondition(CondIsCustomItem.class,
                "%itemstack/itemtype/slot% (is [a[n]]|are) (custom|ce|craft-engine) item[s]",
                "%itemstack/itemtype/slot% (isn't|is not|aren't|are not) [a[n]] (custom|ce|craft-engine) item[s]"
        );
    }

    private Expression<?> item;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        item = expressions[0];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Object single = item.getSingle(event);

        ItemStack checkItemStack = null;
        if (single instanceof ItemType itemType) {
            checkItemStack = itemType.getTypes().getFirst().getStack();
        } else if (single instanceof ItemStack itemStack) {
            checkItemStack = itemStack;
        } else if (single instanceof Slot slot) {
            checkItemStack = slot.getItem();
        }

        if (checkItemStack == null) return isNegated() ? true : false;

        boolean exists = CraftEngineItems.isCustomItem(checkItemStack);
        if (!exists) return isNegated() ? true : false;
        return isNegated() ? false : true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, item, "itemtypes");
    }
}
