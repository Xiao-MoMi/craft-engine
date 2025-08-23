package net.momirealms.craftengine.bukkit.compatibility.skript.event;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UnsafeBlockStateMatcher;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings({"unchecked"})
@Name("On Custom Block Place And Break")
@Description({"Fires when a Custom block gets place and broken"})
@Since("1.0")
public class EvtCustomBlock extends SkriptEvent {

    public static void register() {
        Skript.registerEvent("Break Custom Block", EvtCustomBlock.class, CustomBlockBreakEvent.class, "(break[ing]|1¦min(e|ing)) of (custom|ce|craft-engine) block[s] [[of] %-unsafeblockstatematchers%]")
                .description("Called when a custom block is broken by a player. If you use 'on mine', only events where the broken block dropped something will call the trigger.");
        EventValues.registerEventValue(CustomBlockBreakEvent.class, Location.class, CustomBlockBreakEvent::location, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockBreakEvent.class, Player.class, CustomBlockBreakEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockBreakEvent.class, Block.class, CustomBlockBreakEvent::bukkitBlock, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockBreakEvent.class, World.class, event -> event.location().getWorld(), EventValues.TIME_NOW);

        Skript.registerEvent("Place Custom Block", EvtCustomBlock.class, CustomBlockPlaceEvent.class, "(plac(e|ing)|build[ing]) of (custom|ce|craft-engine) block[s] [[of] %-unsafeblockstatematchers%]")
                .description("Called when a player places a custom block.");
        EventValues.registerEventValue(CustomBlockPlaceEvent.class, Location.class, CustomBlockPlaceEvent::location, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockPlaceEvent.class, Player.class, CustomBlockPlaceEvent::player, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockPlaceEvent.class, Block.class, CustomBlockPlaceEvent::bukkitBlock, EventValues.TIME_NOW);
        EventValues.registerEventValue(CustomBlockPlaceEvent.class, World.class, event -> event.location().getWorld(), EventValues.TIME_NOW);
    }

    @Nullable
    private Literal<UnsafeBlockStateMatcher> blocks;
    private UnsafeBlockStateMatcher[] blockArray;
    private boolean mine = false;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        if (args[0] != null) {
            blocks = ((Literal<UnsafeBlockStateMatcher>) args[0]);
            blockArray = blocks.getAll();
        }
        mine = parser.mark == 1;
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (mine && event instanceof CustomBlockBreakEvent customBlockBreakEvent) {
            if (!BlockStateUtils.isCorrectTool(customBlockBreakEvent.blockState(), customBlockBreakEvent.player().getItemInHand(InteractionHand.MAIN_HAND))) {
                return false;
            }
        }
        if (blocks == null)
            return true;

        ImmutableBlockState state;
        if (event instanceof CustomBlockBreakEvent customBlockBreakEvent) {
            state = customBlockBreakEvent.blockState();
        } else if (event instanceof CustomBlockPlaceEvent customBlockPlaceEvent) {
            state = customBlockPlaceEvent.blockState();
        } else {
            return false;
        }

        return Arrays.stream(blockArray).anyMatch(block -> block.matches(state));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "break/place" + (blocks != null ? " of " + blocks.toString(event, debug) : "");
    }
}
