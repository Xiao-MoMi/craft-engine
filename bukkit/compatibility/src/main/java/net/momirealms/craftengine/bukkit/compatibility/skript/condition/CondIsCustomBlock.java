package net.momirealms.craftengine.bukkit.compatibility.skript.condition;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class CondIsCustomBlock extends Condition {

    public static void register() {
        Skript.registerCondition(CondIsCustomBlock.class,
                "%blocks% (is|are) [a[n]] (custom|ce|craft-engine) block[s]",
                "%blocks% (is|are) (n't| not) [a[n]] (custom|ce|craft-engine) block[s]");
    }

    private Expression<Block> blocks;

    @Override
    public boolean check(Event event) {
        return blocks.check(event, CraftEngineBlocks::isCustomBlock, isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, blocks, "custom block");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        blocks = (Expression<Block>) expressions[0];
        setNegated(matchedPattern > 1);
        return true;
    }
}
