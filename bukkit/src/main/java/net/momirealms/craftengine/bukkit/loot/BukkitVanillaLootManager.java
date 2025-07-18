package net.momirealms.craftengine.bukkit.loot;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.AbstractVanillaLootManager;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.loot.VanillaLoot;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// note: block listeners are in BlockEventListener to reduce performance cost
public class BukkitVanillaLootManager extends AbstractVanillaLootManager implements Listener {
    private final BukkitCraftEngine plugin;
    private final VanillaLootParser vanillaLootParser;

    public BukkitVanillaLootManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.vanillaLootParser = new VanillaLootParser();
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Key key = KeyUtils.namespacedKey2Key(entity.getType().getKey());
        Optional.ofNullable(this.entityLoots.get(key)).ifPresent(loot -> {
            if (loot.override()) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
            Location location = entity.getLocation();
            net.momirealms.craftengine.core.world.World world = new BukkitWorld(entity.getWorld());
            WorldPosition position = new WorldPosition(world, location.getX(), location.getY(), location.getZ());
            ContextHolder.Builder builder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.POSITION, position);
            BukkitServerPlayer optionalPlayer = null;
            if (VersionHelper.isOrAbove1_20_5()) {
                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                    optionalPlayer = this.plugin.adapt(player);
                    builder.withParameter(DirectContextParameters.PLAYER, optionalPlayer);
                }
            }
            ContextHolder contextHolder = builder.build();
            for (LootTable<?> lootTable : loot.lootTables()) {
                for (Item<?> item : lootTable.getRandomItems(contextHolder, world, optionalPlayer)) {
                    world.dropItemNaturally(position, item);
                }
            }
        });
    }

    @Override
    public ConfigParser parser() {
        return this.vanillaLootParser;
    }

    public class VanillaLootParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"vanilla-loots", "vanilla-loot", "vanilla_loots", "vanilla_loot"};

        @Override
        public int loadingSequence() {
            return LoadingSequence.VANILLA_LOOTS;
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(section.get("type"), "warning.config.vanilla_loot.missing_type");
            VanillaLoot.Type typeEnum;
            try {
                typeEnum = VanillaLoot.Type.valueOf(type.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                throw new LocalizedResourceConfigException("warning.config.vanilla_loot.invalid_type", type, EnumUtils.toString(VanillaLoot.Type.values()));
            }
            boolean override = ResourceConfigUtils.getAsBoolean(section.getOrDefault("override", false), "override");
            List<String> targets = MiscUtils.getAsStringList(section.getOrDefault("target", List.of()));
            LootTable<?> lootTable = LootTable.fromMap(MiscUtils.castToMap(section.get("loot"), false));
            switch (typeEnum) {
                case BLOCK -> {
                    for (String target : targets) {
                        if (target.endsWith("]") && target.contains("[")) {
                            java.lang.Object blockState = BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData(target));
                            if (blockState == MBlocks.AIR$defaultState) {
                                throw new LocalizedResourceConfigException("warning.config.vanilla_loot.block.invalid_target", target);
                            }
                            VanillaLoot vanillaLoot = blockLoots.computeIfAbsent(BlockStateUtils.blockStateToId(blockState), k -> new VanillaLoot(VanillaLoot.Type.BLOCK));
                            vanillaLoot.addLootTable(lootTable);
                        } else {
                            for (Object blockState : BlockStateUtils.getAllVanillaBlockStates(Key.of(target))) {
                                if (blockState == MBlocks.AIR$defaultState) {
                                    throw new LocalizedResourceConfigException("warning.config.vanilla_loot.block.invalid_target", target);
                                }
                                VanillaLoot vanillaLoot = blockLoots.computeIfAbsent(BlockStateUtils.blockStateToId(blockState), k -> new VanillaLoot(VanillaLoot.Type.BLOCK));
                                if (override) vanillaLoot.override(true);
                                vanillaLoot.addLootTable(lootTable);
                            }
                        }
                    }
                }
                case ENTITY -> {
                    for (String target : targets) {
                        Key key = Key.of(target);
                        VanillaLoot vanillaLoot = entityLoots.computeIfAbsent(key, k -> new VanillaLoot(VanillaLoot.Type.ENTITY));
                        vanillaLoot.addLootTable(lootTable);
                        if (override) vanillaLoot.override(true);
                    }
                }
            }
        }
    }
}
