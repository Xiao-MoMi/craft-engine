package net.momirealms.craftengine.bukkit.loot;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.AbstractLootManager;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.loot.LootTableReference;
import net.momirealms.craftengine.core.loot.VanillaLoot;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.compatibility.EntityProvider;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// note: block listeners are in BlockEventListener to reduce performance cost
public final class BukkitLootManager extends AbstractLootManager implements Listener {
    private static BukkitLootManager instance;
    private final BukkitCraftEngine plugin;
    private final VanillaLootParser vanillaLootParser;
    private EntityProvider[] entitySources;

    public BukkitLootManager(BukkitCraftEngine plugin) {
        if (instance != null) {
            throw new IllegalStateException();
        }
        instance = this;
        this.plugin = plugin;
        this.vanillaLootParser = new VanillaLootParser();
    }

    public static BukkitLootManager instance() {
        return instance;
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
        BukkitEntity bukkitEntity = BukkitAdaptor.adapt(entity);
        Key key = getEntityId(bukkitEntity);
        Optional.ofNullable(this.entityLoots.get(key)).ifPresent(vanillaLoot -> {
            if (vanillaLoot.override()) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
            Location location = entity.getLocation();
            net.momirealms.craftengine.core.world.World world = BukkitAdaptor.adapt(entity.getWorld());
            WorldPosition position = new WorldPosition(world, location.getX(), location.getY(), location.getZ());
            ContextHolder.Builder builder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.ENTITY, bukkitEntity)
                    .withParameter(DirectContextParameters.POSITION, position);
            BukkitServerPlayer optionalPlayer = null;
            float luck = 1.0f;
            if (VersionHelper.isOrAbove1_20_5) {
                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                    optionalPlayer = BukkitAdaptor.adapt(player);
                    builder.withOptionalParameter(DirectContextParameters.PLAYER, optionalPlayer);
                    if (optionalPlayer != null) {
                        luck = (float) optionalPlayer.luck();
                        Item itemInHand = optionalPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                        builder.withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, ItemUtils.isEmpty(itemInHand) ? null : itemInHand);
                    }
                }
            }
            ContextHolder contextHolder = builder.build();
            EntityLootContext entityLootContext = new EntityLootContext(world, optionalPlayer, luck, contextHolder, entity);
            for (Loot loot : vanillaLoot.loots()) {
                for (Item item : loot.getRandomItems(entityLootContext)) {
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
    public ConfigParser[] parsers() {
        return new ConfigParser[] {this.vanillaLootParser, super.lootParser};
    }

    @Override
    public LootTableReference createReference(Key key) {
        LazyReference<Loot> lazyReference = LazyReference.lazyReference(() -> {
            Optional<Loot> lootTable = BukkitLootManager.instance().getLoot(key);
            return lootTable.orElseGet(() -> new DatapackLootTable(key));
        });
        return new LootTableReference(lazyReference);
    }

    private final class VanillaLootParser extends IdSectionConfigParser {
        private static final String[] CONFIG_SECTION_NAME = new String[] {"vanilla-loots", "vanilla-loot"};
        private static final String[] LOOT_SECTION = new String[] {"loot", "loots"};
        private int count;

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

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
            return LoadingStages.VANILLA_LOOT;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE, LoadingStages.LOOT_TABLE);
        }

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            VanillaLoot.Type typeEnum = section.getNonNullEnum("type", VanillaLoot.Type.class);
            boolean override = section.getBoolean("override");
            Loot loot = section.getValue(LOOT_SECTION, ConfigValue::getAsLoot);
            switch (typeEnum) {
                case BLOCK -> {
                    List<String> targets = section.getStringList("target");
                    for (String target : targets) {
                        if (target.endsWith("]") && target.contains("[")) {
                            java.lang.Object blockState = BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData(target));
                            if (blockState == BlocksProxy.AIR$defaultState) {
                                throw new KnownResourceException("resource.vanilla_loot.block.invalid_target", target);
                            }
                            VanillaLoot vanillaLoot = BukkitLootManager.this.blockLoots.computeIfAbsent(BlockStateUtils.blockStateToId(blockState), k -> new VanillaLoot(VanillaLoot.Type.BLOCK));
                            vanillaLoot.addLootTable(loot);
                        } else {
                            for (Object blockState : BlockStateUtils.getPossibleBlockStates(Key.of(target))) {
                                if (blockState == BlocksProxy.AIR$defaultState) {
                                    throw new KnownResourceException("resource.vanilla_loot.block.invalid_target", target);
                                }
                                VanillaLoot vanillaLoot = BukkitLootManager.this.blockLoots.computeIfAbsent(BlockStateUtils.blockStateToId(blockState), k -> new VanillaLoot(VanillaLoot.Type.BLOCK));
                                if (override) vanillaLoot.override(true);
                                vanillaLoot.addLootTable(loot);
                            }
                        }
                    }
                }
                case ENTITY -> {
                    List<Key> entityTypes = section.getList("target", ConfigValue::getAsIdentifier);
                    for (Key key : entityTypes) {
                        VanillaLoot vanillaLoot = BukkitLootManager.this.entityLoots.computeIfAbsent(key, k -> new VanillaLoot(VanillaLoot.Type.ENTITY));
                        vanillaLoot.addLootTable(loot);
                        if (override) vanillaLoot.override(true);
                    }
                }
            }
            this.count++;
        }
    }
}
