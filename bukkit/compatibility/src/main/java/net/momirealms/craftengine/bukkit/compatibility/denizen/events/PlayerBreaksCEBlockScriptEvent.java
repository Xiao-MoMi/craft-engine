package net.momirealms.craftengine.bukkit.compatibility.denizen.events;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerBreaksCEBlockScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // player breaks ce block <'block'>
    //
    // @Group Player
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Switch with:<item> to only process the event when the player is breaking the block with a specified item.
    //
    // @Triggers when a player breaks a CraftEngine custom block.
    // The word "ce" in the event line can also be written as "craftengine".
    //
    // @Context
    // <context.location> returns the LocationTag the block was broken at.
    // <context.id> returns the CraftEngine block id of the block that was broken, like "default:togglable_light_block".
    // <context.should_drop_items> returns whether the event will drop items.
    //
    // @Determine
    // "NOTHING" to make the block drop no items.
    // ListTag(ItemTag) to make the block drop a specified list of items.
    //
    // @Player Always.
    //
    // @Example
    // # Narrate the CraftEngine block's id when a player breaks any CraftEngine block.
    // on player breaks ce block block:
    // - narrate "You just broke a <context.id>!"
    //
    // @Example
    // # Only fire when a specific CraftEngine block is broken with a specific tool.
    // on player breaks ce block default:togglable_light_block with:diamond_pickaxe:
    // - narrate "You just broke a togglable light block with a diamond pickaxe!"
    // -->

    public PlayerBreaksCEBlockScriptEvent() {
        registerCouldMatcher("player breaks ce block <'block'>");
        registerCouldMatcher("player breaks craftengine block <'block'>");
        registerSwitches("with");
        this.<PlayerBreaksCEBlockScriptEvent>registerTextDetermination("nothing", (evt) -> {
            evt.event.setDropItems(false);
        });
        this.<PlayerBreaksCEBlockScriptEvent, ListTag>registerDetermination(null, ListTag.class, (evt, context, list) -> {
            evt.event.setDropItems(false);
            Block block = evt.event.bukkitBlock();
            BukkitWorld world = BukkitAdaptor.adapt(block.getWorld());
            Vec3d position = new Vec3d(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
            for (ItemTag newItem : list.filter(ItemTag.class, context)) {
                world.dropItemNaturally(position, BukkitAdaptor.adapt(newItem.getItemStack()));
            }
        });
    }

    public LocationTag location;
    public String id;
    public CustomBlockBreakEvent event;

    @Override
    public boolean matches(ScriptPath path) {
        String matcher = path.eventArgLowerAt(4);
        if (!matcher.equals("block") && !createMatcher(matcher).doesMatch(this.id)) {
            return false;
        }
        if (!runWithCheck(path, new ItemTag(this.event.getPlayer().getInventory().getItemInMainHand()))) {
            return false;
        }
        if (!runInCheck(path, this.location)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(this.event.getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "location" -> this.location;
            case "id" -> new ElementTag(this.id);
            case "should_drop_items" -> new ElementTag(this.event.dropItems());
            default -> super.getContext(name);
        };
    }

    @EventHandler
    public void onPlayerBreaksCEBlock(CustomBlockBreakEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        this.location = new LocationTag(event.location());
        this.id = event.customBlock().id().toString();
        this.event = event;
        fire(event);
    }
}
