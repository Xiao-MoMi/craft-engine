package net.momirealms.craftengine.bukkit.loot;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.AbstractLootManager;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.loot.VanillaLoot;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.compatibility.EntityProvider;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.*;

// note: block listeners are in BlockEventListener to reduce performance cost
public class BukkitLootManager extends AbstractLootManager implements Listener {
    private final BukkitCraftEngine plugin;
    private final LootParser vanillaLootParser;
    private EntityProvider[] entitySources;

    public BukkitLootManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.vanillaLootParser = new LootParser();
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void delayedLoad() {
        List<EntityProvider> entityProviders = new ArrayList<>();
        for (String source : Config.lootEntitySources()) {
            Optional.ofNullable(this.plugin.compatibilityManager().getEntityProvider(source)).ifPresent(entityProviders::add);
        }
        this.entitySources = entityProviders.toArray(new EntityProvider[0]);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        BukkitEntity bukkitEntity = BukkitAdaptors.adapt(entity);
        Key key = getEntityId(bukkitEntity);
        Optional.ofNullable(this.entityLoots.get(key)).ifPresent(loot -> {
            if (loot.override()) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
            Location location = entity.getLocation();
            net.momirealms.craftengine.core.world.World world = BukkitAdaptors.adapt(entity.getWorld());
            WorldPosition position = new WorldPosition(world, location.getX(), location.getY(), location.getZ());
            ContextHolder.Builder builder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.ENTITY, bukkitEntity)
                    .withParameter(DirectContextParameters.POSITION, position);
            BukkitServerPlayer optionalPlayer = null;
            if (VersionHelper.isOrAbove1_20_5()) {
                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                    optionalPlayer = BukkitAdaptors.adapt(player);
                    builder.withOptionalParameter(DirectContextParameters.PLAYER, optionalPlayer);
                    if (optionalPlayer != null) {
                        Item<ItemStack> itemInHand = optionalPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                        builder.withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, ItemUtils.isEmpty(itemInHand) ? null : itemInHand);
                    }
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

    @ApiStatus.Experimental
    private Key getEntityId(BukkitEntity bukkitEntity) {
        if (this.entitySources != null && this.entitySources.length > 0) {
            for (EntityProvider entityProvider : this.entitySources) {
                String entityId = entityProvider.getEntityId(bukkitEntity);
                if (entityId != null) {
                    return Key.of(entityProvider.plugin(), StringUtils.normalizeString(entityId));
                }
            }
        }
        return bukkitEntity.type();
    }

    @Override
    public ConfigParser parser() {
        return this.vanillaLootParser;
    }

    public class LootParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"loots", "loot", "vanilla-loots", "vanilla-loot"};
        private int count;

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return this.count;
        }

        @Override
        public void preProcess() {
            this.count = 0;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.LOOT;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(section.get("type"), "warning.config.loot.missing_type");
            VanillaLoot.Type typeEnum;
            try {
                typeEnum = VanillaLoot.Type.valueOf(type.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                throw new LocalizedResourceConfigException("warning.config.loot.invalid_type", type, EnumUtils.toString(VanillaLoot.Type.values()));
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
                                throw new LocalizedResourceConfigException("warning.config.loot.block.invalid_target", target);
                            }
                            VanillaLoot vanillaLoot = blockLoots.computeIfAbsent(BlockStateUtils.blockStateToId(blockState), k -> new VanillaLoot(VanillaLoot.Type.BLOCK));
                            vanillaLoot.addLootTable(lootTable);
                        } else {
                            for (Object blockState : BlockStateUtils.getPossibleBlockStates(Key.of(target))) {
                                if (blockState == MBlocks.AIR$defaultState) {
                                    throw new LocalizedResourceConfigException("warning.config.loot.block.invalid_target", target);
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
            this.count++;
        }
    }
}
