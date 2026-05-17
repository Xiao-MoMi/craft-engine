package net.momirealms.craftengine.bukkit.compatibility.skript.event;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unchecked"})
@Name("On Custom Furniture Place And Break")
@Description({"Fires when a Custom furniture gets place and broken"})
@Since("1.0")
public final class EvtCustomFurniture extends SkriptEvent {

    public static void register(SkriptAddon addon) {
        SyntaxRegistry syntaxRegistry = addon.registry(SyntaxRegistry.class);
        EventValueRegistry valueRegistry = addon.registry(EventValueRegistry.class);

        BukkitSyntaxInfos.Event<EvtCustomFurniture> breakEvent = BukkitSyntaxInfos.Event.builder(EvtCustomFurniture.class, "Break Furniture")
                .addPattern("(break[ing]) of [(custom|ce|craft-engine)] furniture[s] [[of] %-strings%]")
                .addDescription("Called when a furniture is broken by a player.")
                .addEvent(FurnitureBreakEvent.class)
                .build();
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, breakEvent);

        valueRegistry.register(EventValue.builder(FurnitureBreakEvent.class, Location.class).getter(FurnitureBreakEvent::location).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurnitureBreakEvent.class, Player.class).getter(FurnitureBreakEvent::player).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurnitureBreakEvent.class, Entity.class).getter(e -> e.furniture().bukkitEntity()).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurnitureBreakEvent.class, World.class).getter(e -> e.location().getWorld()).time(EventValue.Time.NOW).build());

        BukkitSyntaxInfos.Event<EvtCustomFurniture> placeEvent = BukkitSyntaxInfos.Event.builder(EvtCustomFurniture.class, "Place Furniture")
                .addPattern("(plac(e|ing)|build[ing]) of [(custom|ce|craft-engine)] furniture[s] [[of] %-strings%]")
                .addDescription("Called when a player places a furniture.")
                .addEvent(FurniturePlaceEvent.class)
                .build();
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, placeEvent);

        valueRegistry.register(EventValue.builder(FurniturePlaceEvent.class, Location.class).getter(FurniturePlaceEvent::location).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurniturePlaceEvent.class, Player.class).getter(FurniturePlaceEvent::player).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurniturePlaceEvent.class, Entity.class).getter(e -> e.furniture().bukkitEntity()).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurniturePlaceEvent.class, World.class).getter(e -> e.location().getWorld()).time(EventValue.Time.NOW).build());
    }

    @Nullable
    private Literal<String> ids;
    private List<String> idList;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        if (args[0] != null) {
            ids = ((Literal<String>) args[0]);
            idList = Arrays.stream(ids.getAll()).toList();
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (ids == null) return true;

        String id;
        if (event instanceof FurnitureBreakEvent e) {
            id = e.furniture().id().toString();
        } else if (event instanceof FurniturePlaceEvent e) {
            id = e.furniture().id().toString();
        } else {
            return false;
        }

        return idList.contains(id);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "break/place" + (ids != null ? " of " + ids.toString(event, debug) : "");
    }
}
