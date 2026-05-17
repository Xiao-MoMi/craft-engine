package net.momirealms.craftengine.bukkit.compatibility.skript.condition;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class CondIsFurniture extends Condition {

    public static void register(SkriptAddon addon) {
        SyntaxInfo<CondIsFurniture> condition = SyntaxInfo.builder(CondIsFurniture.class)
                .addPattern("%entities% (is|are) [a[n]] [(custom|ce|craft-engine)] furniture[s]")
                .addPattern("%entities% (is|are) (n't| not) [a[n]] [(custom|ce|craft-engine)] furniture[s]")
                .build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.CONDITION, condition);
    }

    private Expression<Entity> entities;

    @Override
    public boolean check(Event event) {
        return entities.check(event, CraftEngineFurniture::isFurniture, isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, entities, "furniture");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        entities = (Expression<Entity>) expressions[0];
        setNegated(matchedPattern > 1);
        return true;
    }
}
