package net.momirealms.craftengine.bukkit.compatibility.skript.effect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffPlaceCustomBlock extends Effect {

    public static void register() {
        Skript.registerEffect(EffPlaceCustomBlock.class, "place (custom|ce|craft-engine) block %customblockstates% [at] [%directions% %locations%]");
    }

    private Expression<ImmutableBlockState> blocks;
    private Expression<Location> locations;

    @Override
    protected void execute(Event e) {
        ImmutableBlockState[] os = blocks.getArray(e);
        for (Location l : locations.getArray(e)) {
            for (ImmutableBlockState o : os) {
                CraftEngineBlocks.place(l, o, false);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "place custom block " + blocks.toString(event, debug) + " " + locations.toString(event, debug);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        blocks = (Expression<ImmutableBlockState>) expressions[0];
        locations = Direction.combine((Expression<? extends Direction>) expressions[1], (Expression<? extends Location>) expressions[2]);
        return true;
    }
}
