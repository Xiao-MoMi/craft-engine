package net.momirealms.craftengine.bukkit.compatibility.skript.effect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffPlaceFurniture extends Effect {

    public static void register() {
        Skript.registerEffect(EffPlaceFurniture.class, "place [(custom|ce|craft-engine)] furniture[s] %strings% [at] [%directions% %locations%]");
    }

    private Expression<String> furniture;
    private Expression<Location> locations;

    @Override
    protected void execute(Event e) {
        String[] os = furniture.getArray(e);
        for (Location l : locations.getArray(e)) {
            for (String o : os) {
                CraftEngineFurniture.place(l, Key.of(o));
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "place furniture " + furniture.toString(event, debug) + " " + locations.toString(event, debug);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        furniture = (Expression<String>) expressions[0];
        locations = Direction.combine((Expression<? extends Direction>) expressions[1], (Expression<? extends Location>) expressions[2]);
        return true;
    }
}
