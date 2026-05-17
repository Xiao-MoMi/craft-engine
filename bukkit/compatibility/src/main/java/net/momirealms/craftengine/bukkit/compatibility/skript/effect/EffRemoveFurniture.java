package net.momirealms.craftengine.bukkit.compatibility.skript.effect;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class EffRemoveFurniture extends Effect {

    public static void register(SkriptAddon addon) {
        SyntaxInfo<EffRemoveFurniture> syntaxInfo = SyntaxInfo.builder(EffRemoveFurniture.class)
                .addPattern("remove [(custom|ce|craft-engine)] furniture %entities%")
                .build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.EFFECT, syntaxInfo);
    }

    private Expression<Entity> entities;

    @Override
    protected void execute(Event e) {
        for (Entity entity : entities.getArray(e)) {
            Furniture bukkitFurniture = CraftEngineFurniture.getLoadedFurnitureByMetaEntity(entity);
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
