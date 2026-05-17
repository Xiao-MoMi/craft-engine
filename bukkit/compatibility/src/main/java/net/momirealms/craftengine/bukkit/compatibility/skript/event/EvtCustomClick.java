package net.momirealms.craftengine.bukkit.compatibility.skript.event;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ClickEventTracker;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UnsafeBlockStateMatcher;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.function.Predicate;

@Name("On Click on Custom Block and Furniture")
@Description({"Fires when click on custom block and furniture"})
@Since("1.0")
public final class EvtCustomClick extends SkriptEvent {

    private final static int RIGHT = 1, LEFT = 2, ANY = RIGHT | LEFT;
    public final static ClickEventTracker INTERACT_TRACKER = new ClickEventTracker(Skript.getInstance());

    @SuppressWarnings("unchecked")
    public static void register(SkriptAddon addon) {
        SyntaxRegistry syntaxRegistry = addon.registry(SyntaxRegistry.class);
        EventValueRegistry valueRegistry = addon.registry(EventValueRegistry.class);

        BukkitSyntaxInfos.Event<EvtCustomClick> clickEvent = BukkitSyntaxInfos.Event.builder(EvtCustomClick.class, "Interact Custom Block Furniture")
                .addPattern("[(" + RIGHT + ":right|" + LEFT + ":left)(| |-)][mouse(| |-)]click[ing] of (ce|craft-engine) [on %-unsafeblockstatematchers/strings%] [(with|using|holding) %-itemtype%]")
                .addPattern("[(" + RIGHT + ":right|" + LEFT + ":left)(| |-)][mouse(| |-)]click[ing] of (ce|craft-engine) (with|using|holding) %itemtype% on %unsafeblockstatematchers/strings%")
                .addEvents(new Class[]{CustomBlockInteractEvent.class, FurnitureInteractEvent.class})
                .build();
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, clickEvent);

        valueRegistry.register(EventValue.builder(CustomBlockInteractEvent.class, Location.class).getter(CustomBlockInteractEvent::location).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockInteractEvent.class, Player.class).getter(CustomBlockInteractEvent::player).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockInteractEvent.class, Block.class).getter(CustomBlockInteractEvent::bukkitBlock).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockInteractEvent.class, Entity.class).getter(e -> null).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockInteractEvent.class, World.class).getter(e -> e.location().getWorld()).time(EventValue.Time.NOW).build());

        valueRegistry.register(EventValue.builder(FurnitureInteractEvent.class, Location.class).getter(FurnitureInteractEvent::location).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurnitureInteractEvent.class, Player.class).getter(FurnitureInteractEvent::player).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurnitureInteractEvent.class, Block.class).getter(e -> null).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurnitureInteractEvent.class, Entity.class).getter(e -> e.furniture().bukkitEntity()).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(FurnitureInteractEvent.class, World.class).getter(e -> e.location().getWorld()).time(EventValue.Time.NOW).build());
    }

    private @Nullable Literal<?> type;
    private @Nullable Literal<ItemType> tools;
    private int click = ANY;

    @Override
    public boolean check(Event event) {
        ImmutableBlockState block;
        String furnitureId;
        if (event instanceof CustomBlockInteractEvent interactEvent) {
            furnitureId = null;
            CustomBlockInteractEvent.Action action = interactEvent.action();
            int click;
            switch (action)  {
                case LEFT_CLICK -> click = LEFT;
                case RIGHT_CLICK -> click = RIGHT;
                default -> {
                    return false;
                }
            }
            if ((this.click & click) == 0)
                return false;
            EquipmentSlot hand = interactEvent.hand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
            if (!INTERACT_TRACKER.checkEvent(interactEvent.getPlayer(), interactEvent, hand)) {
                return false;
            }
            block = interactEvent.blockState();
        } else if (event instanceof FurnitureInteractEvent interactEvent) {
            furnitureId = interactEvent.furniture().id().toString();
            block = null;
            if ((this.click & RIGHT) == 0)
                return false;
        } else {
            return false;
        }

        Predicate<ItemType> checker = itemType -> {
            if (event instanceof CustomBlockInteractEvent event1) {
                return itemType.isOfType(event1.item());
            } else {
                FurnitureInteractEvent interactEvent = (FurnitureInteractEvent) event;
                return itemType.isOfType(interactEvent.player().getInventory().getItem(interactEvent.hand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND));
            }
        };

        if (tools != null && !tools.check(event, checker))
            return false;

        if (type != null) {
            return type.check(event, (Predicate<Object>) object -> {
                if (object instanceof String id && furnitureId != null) {
                    return id.equals(furnitureId);
                } else if (object instanceof UnsafeBlockStateMatcher matcher && block != null)  {
                    return matcher.matches(block);
                }
                return false;
            });
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        click = parseResult.mark == 0 ? ANY : parseResult.mark;
        type = args[matchedPattern];
        tools = (Literal<ItemType>) args[1 - matchedPattern];
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (click) {
            case LEFT -> "left";
            case RIGHT -> "right";
            default -> "";
        } + "click" + (type != null ? " on " + type.toString(event, debug) : "") +
                (tools != null ? " holding " + tools.toString(event, debug) : "");
    }
}
