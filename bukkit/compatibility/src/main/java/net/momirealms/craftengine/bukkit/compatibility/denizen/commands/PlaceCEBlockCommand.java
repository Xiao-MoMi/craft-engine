package net.momirealms.craftengine.bukkit.compatibility.denizen.commands;

import com.denizenscript.denizen.objects.CuboidTag;
import com.denizenscript.denizen.objects.EllipsoidTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;

import java.util.ArrayList;
import java.util.List;

// ScriptEntry 的对象存取 API 在 Denizen 开发版快照中被标记 @Deprecated（软废弃），
// 但 Denizen 官方插件源码仍全面使用该 API，此处与官方写法保持一致
@SuppressWarnings("deprecation")
public class PlaceCEBlockCommand extends AbstractCommand implements Holdable {

    public PlaceCEBlockCommand() {
        setName("placeceblock");
        setSyntax("placeceblock [<location>|.../<ellipsoid>/<cuboid>] [<craftengine_block>|...] (no_physics) (<percent chance>|...) (silent)");
        setRequiredArguments(2, 5);
        isProcedural = false;
    }

    // <--[command]
    // @Name PlaceCEBlock
    // @Syntax placeceblock [<location>|.../<ellipsoid>/<cuboid>] [<craftengine_block>|...] (no_physics) (<percent chance>|...) (silent)
    // @Required 2
    // @Maximum 5
    // @Short Places CraftEngine custom blocks.
    // @Group world
    // @Plugin CraftEngine
    //
    // @Description
    // Places CraftEngine custom blocks in the world based on the criteria given.
    //
    // Blocks are specified as CraftEngine block ids, optionally with block state properties,
    // like "custom:sugar_block" or "custom:chinese_lantern[lit=true]".
    //
    // Use 'no_physics' to place the blocks without physics taking over the modified blocks.
    // Use 'silent' to skip the placement sound. Sounds play by default.
    //
    // Specify (<percent chance>|...) to give a chance of each type of blocks being placed (in any block at all).
    // Chances are rolled in order per location: each block type is rolled against its own percentage until one succeeds.
    // If no roll succeeds, the location is left unchanged. For example "50|50" means 50% the first block, 25% the second, and 25% unchanged.
    //
    // Note that specifying a list of locations will take more time in parsing than in the actual block modification.
    //
    // The placeceblock command is ~waitable. Refer to <@link language ~waitable>.
    //
    // @Tags
    // <LocationTag.is_ce_block>
    // <LocationTag.ce_id>
    //
    // @Usage
    // Use to place a CraftEngine block where the player is looking.
    // - placeceblock <player.cursor_on> custom:sugar_block
    //
    // @Usage
    // Use to fill an entire cuboid with two CraftEngine blocks at random.
    // - placeceblock <player.location.to_cuboid[<player.cursor_on>]> custom:sugar_block|custom:cloud_block 50|50
    //
    // @Usage
    // Use to place a CraftEngine block without physics, and wait for completion.
    // - ~placeceblock <player.cursor_on> custom:chinese_lantern[lit=true] no_physics
    //
    // @Usage
    // Use to place a CraftEngine block without playing its placement sound.
    // - placeceblock <player.cursor_on> custom:sugar_block silent
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
            else if (!scriptEntry.hasObject("physics")
                    && arg.matches("no_physics")) {
                scriptEntry.addObject("physics", new ElementTag(false));
            }
            else if (!scriptEntry.hasObject("silent")
                    && arg.matches("silent")) {
                scriptEntry.addObject("silent", new ElementTag(true));
            }
            else if (!scriptEntry.hasObject("blocks")) {
                scriptEntry.addObject("blocks", listArg(arg));
            }
            else if (!scriptEntry.hasObject("percents")) {
                scriptEntry.addObject("percents", listArg(arg));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("blocks")) {
            throw new InvalidArgumentsException("Missing CraftEngine block argument!");
        }
        if (!scriptEntry.hasObject("locations") && !scriptEntry.hasObject("location_list")) {
            throw new InvalidArgumentsException("Missing location argument!");
        }
        scriptEntry.defaultObject("physics", new ElementTag(true));
        scriptEntry.defaultObject("silent", new ElementTag(false));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        final ListTag blocks = scriptEntry.getObjectTag("blocks");
        ListTag percents = scriptEntry.getObjectTag("percents");
        final ElementTag physics = scriptEntry.getElement("physics");
        final ElementTag silent = scriptEntry.getElement("silent");
        if (percents != null && percents.size() != blocks.size()) {
            Debug.echoError(scriptEntry, "Percents length != blocks length");
            percents = null;
        }
        List<ImmutableBlockState> states = new ArrayList<>(blocks.size());
        for (String blockString : blocks) {
            ImmutableBlockState state;
            try {
                state = BlockStateParser.deserialize(blockString);
            }
            catch (IllegalArgumentException e) {
                Debug.echoError(scriptEntry, "Invalid CraftEngine block '" + blockString + "': " + e.getMessage());
                scriptEntry.setFinished(true);
                return;
            }
            if (state == null) {
                Debug.echoError(scriptEntry, "Invalid CraftEngine block '" + blockString + "'");
                scriptEntry.setFinished(true);
                return;
            }
            states.add(state);
        }
        if (states.isEmpty()) {
            Debug.echoError(scriptEntry, "Must specify a valid CraftEngine block!");
            scriptEntry.setFinished(true);
            return;
        }
        List<Float> percentages = null;
        if (percents != null) {
            percentages = new ArrayList<>(percents.size());
            for (String str : percents) {
                percentages.add(new ElementTag(str).asFloat());
            }
        }
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), blocks, physics, percents, silent,
                    scriptEntry.hasObject("locations") ? db("locations", scriptEntry.getObject("locations")) : scriptEntry.getObjectTag("location_list"));
        }
        List<LocationTag> locations = getLocations(scriptEntry);
        if (locations == null || locations.isEmpty()) {
            scriptEntry.setFinished(true);
            return;
        }
        int flags = physics.asBoolean() ? UpdateFlags.UPDATE_ALL : UpdateFlags.UPDATE_NO_PHYS;
        int index = 0;
        for (LocationTag location : locations) {
            if (location == null || location.getWorld() == null) {
                Debug.echoError(scriptEntry, "Input is not a valid LocationTag");
                scriptEntry.setFinished(true);
                return;
            }
            placeAt(location, index, states, percentages, flags, !silent.asBoolean());
            index++;
        }
        scriptEntry.setFinished(true);
    }

    /**
     * Denizen 的 {@link Argument} 会把 "namespace:id" 中第一个冒号前的部分视为参数前缀并剥离，
     * 命名空间 id 列表需要基于原始字符串解析
     */
    static ListTag listArg(Argument arg) {
        String raw = arg.getRawValue();
        if (raw.startsWith("li@")) {
            return arg.asType(ListTag.class);
        }
        return ListTag.valueOf(raw, CoreUtilities.noDebugContext);
    }

    static List<LocationTag> getLocations(ScriptEntry scriptEntry) {
        List<LocationTag> result = new ArrayList<>();
        if (scriptEntry.getObject("locations") instanceof List<?> cuboidOrEllipsoid) {
            for (Object obj : cuboidOrEllipsoid) {
                if (obj instanceof LocationTag locationTag) {
                    result.add(locationTag);
                }
            }
            return result;
        }
        ListTag locationList = scriptEntry.getObjectTag("location_list");
        if (locationList == null) {
            return null;
        }
        for (int i = 0; i < locationList.size(); i++) {
            if (locationList.getObject(i) instanceof LocationTag locationTag) {
                result.add(locationTag);
            }
            else {
                result.add(LocationTag.valueOf(locationList.getObject(i).toString(), scriptEntry.context));
            }
        }
        return result;
    }

    static void placeAt(LocationTag location, int index, List<ImmutableBlockState> states, List<Float> percents, int flags, boolean playSound) {
        ImmutableBlockState state;
        if (percents == null) {
            state = states.get(index % states.size());
        }
        else {
            state = null;
            for (int i = 0; i < states.size(); i++) {
                float percent = percents.get(i) / 100f;
                if (CoreUtilities.getRandom().nextDouble() <= percent) {
                    state = states.get(i);
                    break;
                }
            }
            if (state == null) {
                return;
            }
        }
        CraftEngineBlocks.place(location.getBlockLocation(), state, flags, playSound);
    }
}
