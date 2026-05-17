package net.momirealms.craftengine.bukkit.compatibility.skript.effect;

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
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class EffPlaceFurniture extends Effect {

    public static void register(SkriptAddon addon) {
        SyntaxInfo<EffPlaceFurniture> syntaxInfo = SyntaxInfo.builder(EffPlaceFurniture.class)
                .addPattern("place [(custom|ce|craft-engine)] furniture[s] %strings% [at] [%directions% %locations%]")
                .build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.EFFECT, syntaxInfo);
    }

    private Expression<String> furniture;
    private Expression<Location> locations;

    @Override
    protected void execute(Event e) {
        String[] ids = furniture.getArray(e);
        for (Location location : locations.getArray(e)) {
            for (String id : ids) {
                CraftEngineFurniture.place(location, Key.of(id));
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
