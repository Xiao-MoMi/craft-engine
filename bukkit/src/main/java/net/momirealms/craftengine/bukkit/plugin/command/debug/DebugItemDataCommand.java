package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.encrypt.ItemCrypto;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.item.network.NetworkItemHandler.NETWORK_ITEM_TAG;

public final class DebugItemDataCommand extends BukkitCommandFeature<CommandSender> {
    private static final TextColor COLOR_TEXT = TextColor.color(0xF5F5F5);

    public DebugItemDataCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .flag(FlagKeys.CLIENT_SIDE_FLAG)
                .handler(context -> {
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(context.sender());
                    if (serverPlayer == null) return;
                    Item itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND).copyWithCount(1);
                    if (itemInHand.isEmpty()) {
                        return;
                    }
                    boolean toClientSide = context.flags().hasFlag(FlagKeys.CLIENT_SIDE_FLAG);
                    if (toClientSide) {
                        itemInHand = BukkitItemManager.instance().s2c(itemInHand, serverPlayer).orElse(itemInHand);
                        if (VersionHelper.COMPONENT_RELEASE) {
                            Tag customData = itemInHand.getComponentAsSparrowTag(DataComponentTypes.CUSTOM_DATA);
                            if (customData instanceof CompoundTag compoundTag) {
                                Tag networkTag = compoundTag.get(NETWORK_ITEM_TAG);
                                if (networkTag != null) {
                                    compoundTag.put(NETWORK_ITEM_TAG, ItemCrypto.decrypt(networkTag));
                                    itemInHand.setSparrowTagComponent(DataComponentTypes.CUSTOM_DATA, compoundTag);
                                }
                            }
                        } else {
                            Tag networkTag = itemInHand.getSparrowTag(NETWORK_ITEM_TAG);
                            if (networkTag != null) {
                                itemInHand.setSparrowTag(ItemCrypto.decrypt(networkTag), NETWORK_ITEM_TAG);
                            }
                        }
                    }

                    Map<String, Object> readableMap = toMap(itemInHand);
                    List<Component> readableComponents = mapToComponentList(readableMap);

                    Component finalMessage = Component.join(JoinConfiguration.separator(Component.newline()), readableComponents);
                    plugin().senderFactory().wrap(context.sender()).sendMessage(finalMessage);
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_item_data";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Item item) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return (Map<String, Object>) ItemStackProxy.INSTANCE.getCodec().encodeStart(RegistryOps.JAVA, item.minecraftItem())
                    .resultOrPartial(error -> CraftEngine.instance().logger().error("Error while saving item: " + error))
                    .orElse(null);
        } else {
            Object nmsTag = ItemStackProxy.INSTANCE.save(item.minecraftItem(), CompoundTagProxy.INSTANCE.newInstance());
            return (Map<String, Object>) RegistryOps.NBT.convertTo(RegistryOps.JAVA, nmsTag);
        }
    }

    private List<Component> mapToComponentList(Map<String, Object> readableDataMap) {
        List<Component> list = new ArrayList<>();
        mapToComponentList(readableDataMap, list, 0, false);
        return list;
    }

    @SuppressWarnings("unchecked")
    static void mapToComponentList(Map<String, Object> map, List<Component> readableList, int loopTimes, boolean isMapList) {
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object nbt = entry.getValue();

            String indent = "  ".repeat(isMapList && first ? Math.max(0, loopTimes - 1) : loopTimes);
            String prefixString = (isMapList && first) ? indent + "- " : indent;
            Component prefix = Component.text(prefixString, COLOR_TEXT);

            Component gradientKey = AdventureHelper.miniMessage().deserialize("<gradient:#FFD700:#FFFACD>" + entry.getKey() + "</gradient>");

            if (nbt instanceof List<?> list) {
                readableList.add(prefix
                        .append(gradientKey.hoverEvent(HoverEvent.showText(Component.text("List", NamedTextColor.YELLOW))))
                        .append(Component.text(":", COLOR_TEXT))
                );

                for (Object value : list) {
                    if (value instanceof Map<?,?> innerDataMap) {
                        mapToComponentList((Map<String, Object>) innerDataMap, readableList, loopTimes + 2, true);
                    } else {
                        String strValue = String.valueOf(value);
                        readableList.add(Component.text("  ".repeat(loopTimes + 1) + "- ", COLOR_TEXT)
                                .append(Component.text(strValue, COLOR_TEXT)
                                        .hoverEvent(HoverEvent.showText(Component.text("Copy", NamedTextColor.YELLOW)))
                                        .clickEvent(ClickEvent.suggestCommand(strValue)))
                        );
                    }
                }
            } else if (nbt instanceof Map<?,?> innerMap) {
                readableList.add(prefix
                        .append(gradientKey.hoverEvent(HoverEvent.showText(Component.text("Map", NamedTextColor.YELLOW))))
                        .append(Component.text(":", COLOR_TEXT))
                );
                mapToComponentList((Map<String, Object>) innerMap, readableList, loopTimes + 1, false);
            } else {
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

                readableList.add(prefix
                        .append(gradientKey.hoverEvent(HoverEvent.showText(Component.text(nbt.getClass().getSimpleName(), NamedTextColor.YELLOW))))
                        .append(Component.text(": ", COLOR_TEXT))
                        .append(Component.text(value, COLOR_TEXT)
                                .hoverEvent(HoverEvent.showText(Component.text("Copy", NamedTextColor.YELLOW)))
                                .clickEvent(ClickEvent.suggestCommand(value)))
                );
            }
            if (isMapList) {
                first = false;
            }
        }
    }
}