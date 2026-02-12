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
import ch.njol.skript.registrations.EventValues;
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

import java.util.function.Predicate;

@Name("On Click on Custom Block and Furniture")
@Description({"Fires when click on custom block and furniture"})
@Since("1.0")
public class EvtCustomClick extends SkriptEvent {

    private final static int RIGHT = 1, LEFT = 2, ANY = RIGHT | LEFT;
    public final static ClickEventTracker interactTracker = new ClickEventTracker(Skript.getInstance());

    @SuppressWarnings("unchecked")
    public static void register() {
        Skript.registerEvent("Interact Custom Block Furniture", EvtCustomClick.class, new Class[]{CustomBlockInteractEvent.class, FurnitureInteractEvent.class},
                        "[(" + RIGHT + ":right|" + LEFT + ":left)(| |-)][mouse(| |-)]click[ing] of (ce|craft-engine) [on %-unsafeblockstatematchers/strings%] [(with|using|holding) %-itemtype%]",
                        "[(" + RIGHT + ":right|" + LEFT + ":left)(| |-)][mouse(| |-)]click[ing] of (ce|craft-engine) (with|using|holding) %itemtype% on %unsafeblockstatematchers/strings%");

        EventValues.registerEventValue(CustomBlockInteractEvent.class, Location.class, CustomBlockInteractEvent::location, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockInteractEvent.class, Player.class, CustomBlockInteractEvent::player, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockInteractEvent.class, Block.class, CustomBlockInteractEvent::bukkitBlock, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockInteractEvent.class, Entity.class, event -> null, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockInteractEvent.class, World.class, event -> event.location().getWorld(), EventValues.TIME_NOW);

        EventValues.registerEventValue(FurnitureInteractEvent.class, Location.class, FurnitureInteractEvent::location, EventValues.TIME_NOW);
        EventValues.registerEventValue(FurnitureInteractEvent.class, Player.class, FurnitureInteractEvent::player, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockInteractEvent.class, Block.class, event -> null, EventValues.TIME_NOW);
        EventValues.registerEventValue(FurnitureInteractEvent.class, Entity.class, event -> event.furniture().bukkitEntity(), EventValues.TIME_NOW);
        EventValues.registerEventValue(FurnitureInteractEvent.class, World.class, event -> event.location().getWorld(), EventValues.TIME_NOW);
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
            if (!interactTracker.checkEvent(interactEvent.getPlayer(), interactEvent, hand)) {
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
            } else if (event instanceof FurnitureInteractEvent event1) {
                return itemType.isOfType(event1.player().getInventory().getItem(event1.hand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND));
            } else {
                return false;
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
