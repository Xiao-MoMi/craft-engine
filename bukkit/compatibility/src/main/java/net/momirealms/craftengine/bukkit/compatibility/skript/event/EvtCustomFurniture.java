package net.momirealms.craftengine.bukkit.compatibility.skript.event;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings({"unchecked"})
@Name("On Custom Furniture Place And Break")
@Description({"Fires when a Custom furniture gets place and broken"})
@Since("1.0")
public class EvtCustomFurniture extends SkriptEvent {

    public static void register() {
        Skript.registerEvent("Break Furniture", EvtCustomFurniture.class, FurnitureBreakEvent.class, "(break[ing]) of [(custom|ce|craft-engine)] furniture[s] [[of] %-strings%]")
                .description("Called when a furniture is broken by a player.");
        EventValues.registerEventValue(FurnitureBreakEvent.class, Location.class, FurnitureBreakEvent::location, EventValues.TIME_NOW);
        EventValues.registerEventValue(FurnitureBreakEvent.class, Player.class, FurnitureBreakEvent::player, EventValues.TIME_NOW);
        EventValues.registerEventValue(FurnitureBreakEvent.class, Entity.class, event -> event.furniture().bukkitEntity(), EventValues.TIME_NOW);
        EventValues.registerEventValue(FurnitureBreakEvent.class, World.class, event -> event.location().getWorld(), EventValues.TIME_NOW);

        Skript.registerEvent("Place Furniture", EvtCustomFurniture.class, FurniturePlaceEvent.class, "(plac(e|ing)|build[ing]) of [(custom|ce|craft-engine)] furniture[s] [[of] %-strings%]")
                .description("Called when a player places a furniture.");
        EventValues.registerEventValue(FurniturePlaceEvent.class, Location.class, FurniturePlaceEvent::location, EventValues.TIME_NOW);
        EventValues.registerEventValue(FurniturePlaceEvent.class, Player.class, FurniturePlaceEvent::player, EventValues.TIME_NOW);
        EventValues.registerEventValue(FurniturePlaceEvent.class, Entity.class, event -> event.furniture().bukkitEntity(), EventValues.TIME_NOW);
        EventValues.registerEventValue(FurniturePlaceEvent.class, World.class, event -> event.location().getWorld(), EventValues.TIME_NOW);
    }

    @Nullable
    private Literal<String> ids;
    private String[] idArray;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        if (args[0] != null) {
            ids = ((Literal<String>) args[0]);
            idArray = ids.getAll();
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (ids == null)
            return true;

        String id;
        if (event instanceof FurnitureBreakEvent e) {
            id = e.furniture().id().toString();
        } else if (event instanceof FurniturePlaceEvent e) {
            id = e.furniture().id().toString();
        } else {
            return false;
        }

        return Arrays.asList(idArray).contains(id);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "break/place" + (ids != null ? " of " + ids.toString(event, debug) : "");
    }
}
