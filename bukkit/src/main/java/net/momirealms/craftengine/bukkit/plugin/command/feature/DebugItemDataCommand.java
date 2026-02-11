package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
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
                .flag(FlagKeys.CLIENT_SIDE_FLAG)
                .handler(context -> {
                    ItemStack itemInHand = context.sender().getInventory().getItemInMainHand().clone();
                    if (ItemStackUtils.isEmpty(itemInHand)) {
                        return;
                    }
                    boolean toClientSide = context.flags().hasFlag(FlagKeys.CLIENT_SIDE_FLAG);
                    if (toClientSide) {
                        itemInHand = BukkitItemManager.instance().s2c(itemInHand, BukkitAdaptors.adapt(context.sender())).orElse(itemInHand);
                    }
                    Map<String, Object> readableMap = toMap(itemInHand);
                    List<String> readableList = mapToList(readableMap);
                    StringJoiner joiner = new StringJoiner("<newline><reset>");
                    for (String text : readableList) {
                        joiner.add(text);
                    }
                    plugin().senderFactory().wrap(context.sender()).sendMessage(AdventureHelper.miniMessage().deserialize(joiner.toString()));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_item_data";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(ItemStack nmsStack) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return (Map<String, Object>) ItemStackProxy.INSTANCE.getCodec().encodeStart(MRegistryOps.JAVA, nmsStack)
                    .resultOrPartial(error -> CraftEngine.instance().logger().severe("Error while saving item: " + error))
                    .orElse(null);
        } else {
            Object nmsTag = ItemStackProxy.INSTANCE.save(nmsStack, CompoundTagProxy.INSTANCE.newInstance());
            return (Map<String, Object>) MRegistryOps.NBT.convertTo(MRegistryOps.JAVA, nmsTag);
        }
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
                    value = switch (nbt) {
                        case Object[] objects -> Arrays.deepToString(objects);
                        case int[] ints -> Arrays.toString(ints);
                        case long[] longs -> Arrays.toString(longs);
                        case double[] doubles -> Arrays.toString(doubles);
                        case float[] floats -> Arrays.toString(floats);
                        case boolean[] booleans -> Arrays.toString(booleans);
                        case byte[] bytes -> Arrays.toString(bytes);
                        case char[] chars -> Arrays.toString(chars);
                        case short[] shorts -> Arrays.toString(shorts);
                        default -> "Unknown array type";
                    };
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
