package net.momirealms.craftengine.bukkit.compatibility.denizen.commands;

import com.denizenscript.denizen.objects.CuboidTag;
import com.denizenscript.denizen.objects.EllipsoidTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;

import java.util.List;

// ScriptEntry 的对象存取 API 在 Denizen 开发版快照中被标记 @Deprecated（软废弃），
// 但 Denizen 官方插件源码仍全面使用该 API，此处与官方写法保持一致
@SuppressWarnings("deprecation")
public class RemoveCEBlockCommand extends AbstractCommand implements Holdable {

    public RemoveCEBlockCommand() {
        setName("removeceblock");
        setSyntax("removeceblock [<location>|.../<ellipsoid>/<cuboid>]");
        setRequiredArguments(1, 1);
        isProcedural = false;
    }

    // <--[command]
    // @Name RemoveCEBlock
    // @Syntax removeceblock [<location>|.../<ellipsoid>/<cuboid>]
    // @Required 1
    // @Maximum 1
    // @Short Removes CraftEngine custom blocks.
    // @Group world
    // @Plugin CraftEngine
    //
    // @Description
    // Removes CraftEngine custom blocks from the world, without dropping loot or playing effects.
    //
    // Locations that don't contain a CraftEngine custom block are silently skipped.
    //
    // The removeceblock command is ~waitable. Refer to <@link language ~waitable>.
    //
    // @Tags
    // <LocationTag.is_ce_block>
    // <LocationTag.ce_id>
    //
    // @Usage
    // Use to remove the CraftEngine block the player is looking at.
    // - removeceblock <player.cursor_on>
    //
    // @Usage
    // Use to clear CraftEngine blocks in an entire cuboid.
    // - removeceblock <player.location.to_cuboid[<player.cursor_on>]>
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (arg.matchesArgumentType(CuboidTag.class)
                    && !scriptEntry.hasObject("locations")
                    && !scriptEntry.hasObject("location_list")
                    && (arg.startsWith("cu@") || !arg.getRawValue().contains("|"))) {
                scriptEntry.addObject("locations", arg.asType(CuboidTag.class).getBlockLocationsUnfiltered(false));
            }
            else if (arg.matchesArgumentType(EllipsoidTag.class)
                    && !scriptEntry.hasObject("locations")
                    && !scriptEntry.hasObject("location_list")
                    && (arg.startsWith("ellipsoid@") || !arg.getRawValue().contains("|"))) {
                scriptEntry.addObject("locations", arg.asType(EllipsoidTag.class).getBlockLocationsUnfiltered(false));
            }
            else if (arg.matchesArgumentList(LocationTag.class)
                    && !scriptEntry.hasObject("locations")
                    && !scriptEntry.hasObject("location_list")) {
                scriptEntry.addObject("location_list", arg.asType(ListTag.class));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("locations") && !scriptEntry.hasObject("location_list")) {
            throw new InvalidArgumentsException("Missing location argument!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(),
                    scriptEntry.hasObject("locations") ? db("locations", scriptEntry.getObject("locations")) : (Object) scriptEntry.getObjectTag("location_list"));
        }
        List<LocationTag> locations = PlaceCEBlockCommand.getLocations(scriptEntry);
        if (locations == null || locations.isEmpty()) {
            scriptEntry.setFinished(true);
            return;
        }
        for (LocationTag location : locations) {
            if (location == null || location.getWorld() == null) {
                Debug.echoError(scriptEntry, "Input is not a valid LocationTag");
                scriptEntry.setFinished(true);
                return;
            }
            CraftEngineBlocks.remove(location.getBlock());
        }
        scriptEntry.setFinished(true);
    }
}
