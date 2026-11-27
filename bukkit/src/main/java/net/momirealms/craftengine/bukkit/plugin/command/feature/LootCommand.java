package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.block.entity.SimpleStorageBlockEntityController;
import net.momirealms.craftengine.bukkit.loot.DatapackLootTable;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftInventoryProxy;
import net.momirealms.craftengine.proxy.minecraft.server.commands.LootCommandProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootTableProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamSetsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.bukkit.parser.location.LocationParser;
import org.incendo.cloud.context.CommandContext;

import java.util.List;

public final class LootCommand extends BukkitCommandFeature<CommandSender> {

    public LootCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder.literal("insert")
                .required("location", LocationParser.locationParser())
                .literal("loot")
                .required("table", NamespacedKeyParser.namespacedKeyParser())
                .flag(FlagKeys.SILENT_FLAG)
                .handler(this::execute);
    }

    private void execute(CommandContext<CommandSender> context) {
        Location target = context.get("location");
        Key id = KeyUtils.namespacedKeyToKey(context.get("table"));
        Object table = DatapackLootTable.resolve(id);
        if (table == LootTableProxy.EMPTY) {
            this.handleFeedback(context, MessageConstants.COMMAND_LOOT_UNKNOWN_TABLE, Component.text(id.asString()));
            return;
        }
        World world = target.getWorld();
        Object level = CraftWorldProxy.INSTANCE.getWorld(world);
        Object builder = LootParamsProxy.BuilderProxy.INSTANCE.newInstance(level);
        CommandSender sender = context.sender();
        Location origin;
        if (sender instanceof Entity entity) {
            origin = entity.getLocation();
            LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.THIS_ENTITY, CraftEntityProxy.INSTANCE.getEntity(entity));
        } else if (sender instanceof BlockCommandSender blockSender) {
            origin = blockSender.getBlock().getLocation().add(0.5, 0.5, 0.5);
        } else {
            origin = world.getSpawnLocation();
        }
        LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.ORIGIN, Vec3Proxy.INSTANCE.newInstance(origin.getX(), origin.getY(), origin.getZ()));
        Object params = LootParamsProxy.BuilderProxy.INSTANCE.create(builder, LootContextParamSetsProxy.CHEST);
        List<Object> drops = LootTableProxy.INSTANCE.getRandomItems(table, params);
        BlockPos pos = LocationUtils.toBlockPos(target);
        this.plugin().scheduler().platform().run(() -> this.insert(context, world, pos, drops), world, pos.x >> 4, pos.z >> 4);
    }

    private void insert(CommandContext<CommandSender> context, World world, BlockPos pos, List<Object> drops) {
        if (!world.isChunkLoaded(pos.x >> 4, pos.z >> 4)) {
            this.handleFeedback(context, MessageConstants.COMMAND_CHUNK_NOT_LOADED);
            return;
        }
        CEWorld storageWorld = BukkitWorldManager.instance().getStorageWorld(world);
        BlockEntity blockEntity = storageWorld == null ? null : storageWorld.getBlockEntityAtIfLoaded(pos);
        Object container;
        if (blockEntity != null) {
            SimpleStorageBlockEntityController storage = blockEntity.controller.get(SimpleStorageBlockEntityController.class, 0);
            Inventory inventory = storage == null ? null : storage.inventory();
            if (inventory == null) {
                this.handleFeedback(context, MessageConstants.COMMAND_LOOT_NOT_CONTAINER);
                return;
            }
            if (!storage.allowsInput()) {
                this.handleFeedback(context, MessageConstants.COMMAND_LOOT_INPUT_DISABLED);
                return;
            }
            container = CraftInventoryProxy.INSTANCE.getInventory(inventory);
        } else {
            container = BlockGetterProxy.INSTANCE.getBlockEntity(CraftWorldProxy.INSTANCE.getWorld(world), LocationUtils.toBlockPos(pos));
            if (!ContainerProxy.CLASS.isInstance(container)) {
                this.handleFeedback(context, MessageConstants.COMMAND_LOOT_NOT_CONTAINER);
                return;
            }
        }
        int insertedStacks = 0;
        for (Object drop : drops) {
            if (LootCommandProxy.INSTANCE.distributeToContainer(container, drop)) {
                ContainerProxy.INSTANCE.setChanged(container);
                insertedStacks++;
            }
        }
        this.handleFeedback(context, MessageConstants.COMMAND_LOOT_INSERTED, Component.text(insertedStacks));
    }

    @Override
    public String getFeatureID() {
        return "loot";
    }
}
