package net.momirealms.craftengine.bukkit.plugin.command.feature;

import com.saicone.rtag.RtagMirror;
import com.saicone.rtag.item.ItemObject;
import com.saicone.rtag.tag.TagCompound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;

import java.util.*;

public class DebugItemDataCommand extends BukkitCommandFeature<CommandSender> {

    public DebugItemDataCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    ItemStack itemInHand = context.sender().getInventory().getItemInMainHand();
                    if (ItemUtils.isEmpty(itemInHand)) {
                        plugin().senderFactory().wrap(context.sender()).sendMessage(Component.text("Please hold an item").color(NamedTextColor.RED));
                        return;
                    }
                    Map<String, Object> readableMap = toReadableMap(itemInHand);
                    readableMap.remove("rtagDataVersion");
                    List<String> readableList = mapToList(readableMap);
                    StringJoiner joiner = new StringJoiner("<newline><reset>");
                    for (String text : readableList) {
                        joiner.add(text);
                    }
                    plugin().senderFactory().wrap(context.sender()).sendMessage(AdventureHelper.miniMessage(joiner.toString()));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_item_data";
    }


    public static Map<String, Object> toReadableMap(ItemStack item) {
        return toMap(item);
    }

    private static Map<String, Object> toMap(ItemStack object) {
        return TagCompound.getValue(RtagMirror.INSTANCE, toCompound(object));
    }

    private static Object toCompound(ItemStack object) {
        if (object == null) {
            return null;
        } else {
            Object compound = extract(object);
            return TagCompound.isTagCompound(compound) ? compound : null;
        }
    }

    private static Object extract(ItemStack object) {
        return ItemObject.save(ItemObject.asNMSCopy(object));
    }

    private List<String> mapToList(Map<String, Object> readableDataMap) {
        List<String> list = new ArrayList<>();
        mapToList(readableDataMap, list, 0, false);
        return list;
    }

    @SuppressWarnings("unchecked")
    private void mapToList(Map<String, Object> map, List<String> readableList, int loopTimes, boolean isMapList) {
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object nbt = entry.getValue();
            if (nbt instanceof List<?> list) {
                if (isMapList && first) {
                    first = false;
                    readableList.add("  ".repeat(loopTimes - 1) +
                            "<#F5F5F5>- <gradient:#FFD700:#FFFACD><hover:show_text:'<yellow>List'>" + entry.getKey() + "</hover></gradient>:</#F5F5F5>");
                } else {
                    readableList.add("  ".repeat(loopTimes) +
                            "<#F5F5F5><gradient:#FFD700:#FFFACD><hover:show_text:'<yellow>List'>" + entry.getKey() + "</hover></gradient>:</#F5F5F5>");
                }
                for (Object value : list) {
                    if (value instanceof Map<?,?> innerDataMap) {
                        mapToList((Map<String, Object>) innerDataMap, readableList, loopTimes + 2, true);
                    } else {
                        if (value instanceof String string) {
                            readableList.add("  ".repeat(loopTimes + 1)
                                    + "<#F5F5F5>- <hover:show_text:'<yellow>Copy'><click:suggest_command:'" + string.replace("'", "\\'") + "'>" + value + "</click></hover></#F5F5F5>");
                        } else {
                            readableList.add("  ".repeat(loopTimes + 1)
                                    + "<#F5F5F5>- <hover:show_text:'<yellow>Copy'><click:suggest_command:'" + value + "'>" + value + "</click></hover></#F5F5F5>");
                        }
                    }
                }
            } else if (nbt instanceof Map<?,?> innerMap) {
                if (isMapList && first) {
                    first = false;
                    readableList.add("  ".repeat(loopTimes - 1) +
                            "<#F5F5F5>- <gradient:#FFD700:#FFFACD><hover:show_text:'<yellow>Map'>" + entry.getKey() + "</hover></gradient>:</#F5F5F5>");
                } else {
                    readableList.add("  ".repeat(loopTimes) +
                            "<#F5F5F5><gradient:#FFD700:#FFFACD><hover:show_text:'<yellow>Map'>" + entry.getKey() + "</hover></gradient>:");
                }
                mapToList((Map<String, Object>) innerMap, readableList, loopTimes + 1, false);
            } else  {
                String value;
                if (nbt.getClass().isArray()) {
                    if (nbt instanceof Object[]) {
                        value = Arrays.deepToString((Object[]) nbt);
                    } else if (nbt instanceof int[]) {
                        value = Arrays.toString((int[]) nbt);
                    } else if (nbt instanceof long[]) {
                        value = Arrays.toString((long[]) nbt);
                    } else if (nbt instanceof double[]) {
                        value = Arrays.toString((double[]) nbt);
                    } else if (nbt instanceof float[]) {
                        value = Arrays.toString((float[]) nbt);
                    } else if (nbt instanceof boolean[]) {
                        value = Arrays.toString((boolean[]) nbt);
                    } else if (nbt instanceof byte[]) {
                        value = Arrays.toString((byte[]) nbt);
                    } else if (nbt instanceof char[]) {
                        value = Arrays.toString((char[]) nbt);
                    } else if (nbt instanceof short[]) {
                        value = Arrays.toString((short[]) nbt);
                    } else {
                        value = "Unknown array type";
                    }
                } else {
                    value = nbt.toString();
                }

                if (isMapList && first) {
                    first = false;
                    readableList.add("  ".repeat(loopTimes - 1) +
                            "<#F5F5F5>- <gradient:#FFD700:#FFFACD><hover:show_text:'<yellow>" + nbt.getClass().getSimpleName() + "'>" + entry.getKey() + "</hover></gradient>" +
                            ": " +
                            "<#F5F5F5><hover:show_text:'<yellow>Copy'><click:suggest_command:'" + value.replace("'", "\\'") + "'>" + value + "</click></hover></#F5F5F5>");
                } else {
                    readableList.add("  ".repeat(loopTimes) +
                            "<#F5F5F5><gradient:#FFD700:#FFFACD><hover:show_text:'<yellow>" + nbt.getClass().getSimpleName() + "'>" + entry.getKey() + "</hover></gradient>" +
                            ": " +
                            "<hover:show_text:'<yellow>Copy'><click:suggest_command:'" + value.replace("'", "\\'") + "'>" + value + "</click></hover></#F5F5F5>");
                }
            }
        }
    }
}
