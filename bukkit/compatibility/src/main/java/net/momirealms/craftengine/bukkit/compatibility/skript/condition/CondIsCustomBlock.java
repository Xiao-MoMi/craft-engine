package net.momirealms.craftengine.bukkit.compatibility.skript.condition;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class CondIsCustomBlock extends Condition {

    public static void register(SkriptAddon addon) {
        SyntaxInfo<CondIsCustomBlock> condition = SyntaxInfo.builder(CondIsCustomBlock.class)
                .addPattern("%blocks% (is|are) [a[n]] (custom|ce|craft-engine) block[s]")
                .addPattern("%blocks% (is|are) (n't| not) [a[n]] (custom|ce|craft-engine) block[s]")
                .build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.CONDITION, condition);
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
