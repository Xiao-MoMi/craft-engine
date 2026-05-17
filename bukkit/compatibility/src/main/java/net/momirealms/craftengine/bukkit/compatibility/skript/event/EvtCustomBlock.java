package net.momirealms.craftengine.bukkit.compatibility.skript.event;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
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
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Arrays;

@SuppressWarnings({"unchecked"})
@Name("On Custom Block Place And Break")
@Description({"Fires when a Custom block gets place and broken"})
@Since("1.0")
public final class EvtCustomBlock extends SkriptEvent {

    public static void register(SkriptAddon addon) {
        SyntaxRegistry syntaxRegistry = addon.registry(SyntaxRegistry.class);
        EventValueRegistry valueRegistry = addon.registry(EventValueRegistry.class);

        BukkitSyntaxInfos.Event<EvtCustomBlock> breakEvent = BukkitSyntaxInfos.Event.builder(EvtCustomBlock.class, "Break Custom Block")
                .addPattern("(break[ing]|1¦min(e|ing)) of (custom|ce|craft-engine) block[s] [[of] %-unsafeblockstatematchers%]")
                .addDescription("Called when a custom block is broken by a player. If you use 'on mine', only events where the broken block dropped something will call the trigger.")
                .addEvent(CustomBlockBreakEvent.class)
                .build();
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, breakEvent);

        valueRegistry.register(EventValue.builder(CustomBlockBreakEvent.class, Location.class).getter(CustomBlockBreakEvent::location).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockBreakEvent.class, Player.class).getter(CustomBlockBreakEvent::getPlayer).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockBreakEvent.class, Block.class).getter(CustomBlockBreakEvent::bukkitBlock).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockBreakEvent.class, World.class).getter(e -> e.location().getWorld()).time(EventValue.Time.NOW).build());

        BukkitSyntaxInfos.Event<EvtCustomBlock> placeEvent = BukkitSyntaxInfos.Event.builder(EvtCustomBlock.class, "Place Custom Block")
                .addPattern("(plac(e|ing)|build[ing]) of (custom|ce|craft-engine) block[s] [[of] %-unsafeblockstatematchers%]")
                .addDescription("Called when a player places a custom block.")
                .addEvent(CustomBlockPlaceEvent.class)
                .build();
        syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, placeEvent);
        valueRegistry.register(EventValue.builder(CustomBlockPlaceEvent.class, Location.class).getter(CustomBlockPlaceEvent::location).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockPlaceEvent.class, Player.class).getter(CustomBlockPlaceEvent::player).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockPlaceEvent.class, Block.class).getter(CustomBlockPlaceEvent::bukkitBlock).time(EventValue.Time.NOW).build());
        valueRegistry.register(EventValue.builder(CustomBlockPlaceEvent.class, World.class).getter(e -> e.location().getWorld()).time(EventValue.Time.NOW).build());
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
