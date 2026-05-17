package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Optional;

@Name("CraftEngine Item ID")
@Description({"Get CraftEngine item id."})
@Since("1.0")
public final class ExprItemCustomItemID extends SimpleExpression<String> {

    public static void register(SkriptAddon addon) {
        DefaultSyntaxInfos.Expression<ExprItemCustomItemID, String> expression = DefaultSyntaxInfos.Expression.builder(ExprItemCustomItemID.class, String.class)
                .priority(PropertyExpression.DEFAULT_PRIORITY)
                .addPattern("(custom|ce|craft-engine) item [namespace] id of %itemstack/itemtype/slot%")
                .addPattern("%itemstack/itemtype/slot%'[s] (custom|ce|craft-engine) item [namespace] id")
                .build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.EXPRESSION, expression);
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

        ItemStack itemStack = null;
        if (single instanceof ItemStack stack) {
            itemStack = stack;
        } else if (single instanceof ItemType itemType) {
            itemStack = itemType.getTypes().getFirst().getStack();
        } else if (single instanceof Slot slot) {
            itemStack = slot.getItem();
        }

        return Optional.ofNullable(itemStack)
                .map(CraftEngineItems::getCustomItemId)
                .map(Key::asString)
                .map(it -> new String[]{it})
                .orElse(null); // 不能返回带有空值的数组
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
