package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.ArrayList;
import java.util.List;

@Name("CraftEngine Item")
@Description({"Get CraftEngine items."})
@Since("1.0")
public final class ExprCustomItem extends SimpleExpression<ItemType> {

    public static void register(SkriptAddon addon) {
        DefaultSyntaxInfos.Expression<ExprCustomItem, ItemType> expression = DefaultSyntaxInfos.Expression.builder(ExprCustomItem.class, ItemType.class)
                .priority(SyntaxInfo.SIMPLE)
                .addPattern("[(the|a)] (custom|ce|craft-engine) item [with [namespace] id] %strings%")
                .build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.EXPRESSION, expression);
    }

    private Expression<?> itemIds;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        itemIds = exprs[0];
        return true;
    }

    @Override
    protected ItemType[] get(Event event) {
        BukkitServerPlayer player = event instanceof PlayerEvent e ? BukkitAdaptor.adapt(e.getPlayer()) : null;
        Object[] objects = itemIds.getArray(event);
        List<ItemType> items = new ArrayList<>();

        for (Object object : objects) {
            if (!(object instanceof String string)) continue;
            BukkitItemDefinition customItem = CraftEngineItems.byId(Key.of(string));
            if (customItem == null) continue;
            items.add(new ItemType(customItem.buildBukkitItem(ItemBuildContext.of(player))));
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
