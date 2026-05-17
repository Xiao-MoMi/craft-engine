package net.momirealms.craftengine.bukkit.compatibility.skript.condition;

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
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Is CraftEngine Item")
@Description({"Checks if the Item is CraftEngine item."})
@Since("1.0")
public final class CondIsCustomItem extends Condition {

    public static void register(SkriptAddon addon) {
        SyntaxInfo<CondIsCustomItem> condition = SyntaxInfo.builder(CondIsCustomItem.class)
                .addPattern("%itemstack/itemtype/slot% (is [a[n]]|are) (custom|ce|craft-engine) item[s]")
                .addPattern("%itemstack/itemtype/slot% (isn't|is not|aren't|are not) [a[n]] (custom|ce|craft-engine) item[s]")
                .build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.CONDITION, condition);
    }

    private Expression<?> item;

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

        return isNegated() ^ (checkItemStack != null && CraftEngineItems.isCustomItem(checkItemStack));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, item, "itemtypes");
    }
}
