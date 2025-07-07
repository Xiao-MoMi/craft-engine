package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.entity.data.HappyGhastData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.location.LocationParser;
import org.incendo.cloud.parser.standard.IntegerParser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestCommand extends BukkitCommandFeature<CommandSender> {

    public TestCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .required("location", LocationParser.locationParser())
                .required("remove", IntegerParser.integerParser())
                .handler(context -> {
                    Player player = context.sender();
                    int removeEntityId = context.get("remove");
                    if (removeEntityId >= 0) {
                        try {
                            Object packet = NetworkReflections.constructor$ClientboundRemoveEntitiesPacket.newInstance((Object) new int[]{removeEntityId});
                            plugin().adapt(player).sendPacket(packet, true);
                            player.sendMessage("发送成功");
                        } catch (ReflectiveOperationException e) {
                            player.sendMessage("发送失败");
                        }
                        return;
                    }
                    Location location = context.get("location");
                    int entityId = CoreReflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
                    List<Object> packets = new ArrayList<>();
                    List<Object> cachedShulkerValues = new ArrayList<>();
                    HappyGhastData.MobFlags.addEntityDataIfNotDefaultValue((byte) 0x01, cachedShulkerValues); // NO AI
                    // HappyGhastData.SharedFlags.addEntityDataIfNotDefaultValue((byte) 0x20, cachedShulkerValues); // Invisible
                    HappyGhastData.StaysStill.addEntityDataIfNotDefaultValue(true, cachedShulkerValues);
                    packets.add(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                            entityId, UUID.randomUUID(), location.x(), Integer.MAX_VALUE, location.z(), 0, location.getYaw(),
                            MEntityTypes.HAPPY_GHAST, 0, CoreReflections.instance$Vec3$Zero, 0
                    ));
                    packets.add(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityId, List.copyOf(cachedShulkerValues)));
                    packets.add(EntityUtils.buildTeleportPacket(entityId, false, location.x(), location.y(), location.z(), 0, 0));
                    plugin().adapt(player).sendPackets(packets, true);
                    player.sendMessage("发送成功 id: " + entityId);
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_test";
    }
}
