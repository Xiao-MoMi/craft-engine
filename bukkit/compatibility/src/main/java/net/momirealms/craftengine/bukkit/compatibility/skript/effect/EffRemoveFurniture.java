package net.momirealms.craftengine.bukkit.compatibility.skript.effect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffRemoveFurniture extends Effect {

    public static void register() {
        Skript.registerEffect(EffRemoveFurniture.class, "remove [(custom|ce|craft-engine)] furniture %entities%");
    }

    private Expression<Entity> entities;

    @Override
    protected void execute(Event e) {
        for (Entity entity : entities.getArray(e)) {
            Furniture bukkitFurniture = CraftEngineFurniture.getLoadedFurnitureByBaseEntity(entity);
            if (bukkitFurniture != null) {
                bukkitFurniture.destroy();
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "remove furniture " + entities.toString(event, debug);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = (Expression<Entity>) expressions[0];
        return true;
    }
}
