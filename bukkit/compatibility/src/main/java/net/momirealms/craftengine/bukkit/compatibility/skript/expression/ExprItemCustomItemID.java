package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Name("CraftEngine Item ID")
@Description({"Get CraftEngine item id."})
@Since("1.0")
public class ExprItemCustomItemID extends SimpleExpression<String> {

    public static void register() {
        Skript.registerExpression(ExprItemCustomItemID.class, String.class, ExpressionType.PROPERTY,
                "(custom|ce|craft-engine) item [namespace] id of %itemstack/itemtype/slot%",
                "%itemstack/itemtype/slot%'[s] (custom|ce|craft-engine) item [namespace] id"
        );
    }

    private Expression<?> itemStackExpr;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        itemStackExpr = exprs[0];
        return true;
    }

    @Override
    protected String[] get(Event event) {
        Object single = itemStackExpr.getSingle(event);

        String result = null;
        if (single instanceof ItemStack itemStack) {
            result =  Optional.of(itemStack).map(this::getCraftEngineItemId).orElse(null);
        } else if (single instanceof ItemType itemType) {
            result = Optional.ofNullable(itemType.getTypes().getFirst().getStack()).map(this::getCraftEngineItemId).orElse(null);
        } else if (single instanceof Slot slot) {
            result = Optional.ofNullable(slot.getItem()).map(this::getCraftEngineItemId).orElse(null);
        }

        return new String[] {result};
    }


    private String getCraftEngineItemId(ItemStack itemStack) {
        return Optional.ofNullable(CraftEngineItems.getCustomItemId(itemStack))
                .map(Key::asString)
                .orElse(null);
    }

    @Override
    public boolean isSingle() {
        return itemStackExpr.isSingle();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    // 不需要处理 add, delete 等修改操作
    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "craft-engine item ID of " + itemStackExpr.toString(event, debug);
    }
}
