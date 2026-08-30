package net.momirealms.craftengine.bukkit.loot;

import net.momirealms.craftengine.bukkit.loot.source.*;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.AbstractLootManager;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootTableReference;
import net.momirealms.craftengine.core.loot.source.LootSourceType;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.world.damagesource.DamageSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.function.Supplier;

public final class BukkitLootManager extends AbstractLootManager {
    private static final Map<ContextKey<?>, MinecraftLootParamMapper> MINECRAFT_LOOT_PARAM_MAPPERS = Map.of(
            DirectContextParameters.POSITION, (builder, value, contexts) -> {
                WorldPosition position = (WorldPosition) value;
                LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.ORIGIN, Vec3Proxy.INSTANCE.newInstance(position.x(), position.y(), position.z()));
            },
            DirectContextParameters.BLOCK, (builder, value, contexts) -> {
                ExistingBlock block = (ExistingBlock) value;
                LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.BLOCK_STATE, block.blockState().minecraftState());
            },
            DirectContextParameters.ITEM_IN_HAND, (builder, value, contexts) ->
                    LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.TOOL, ((Item) value).minecraftItem()),
            DirectContextParameters.ENTITY, (builder, value, contexts) ->
                    LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.THIS_ENTITY, ((Entity) value).minecraftEntity()),
            // 伤害源是死亡类上下文的标志, 原版仅实体死亡参数集包含伤害相关参数
            BukkitLootContextParameters.DAMAGE_SOURCE, (builder, value, contexts) -> {
                LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.DAMAGE_SOURCE, value);
                if (VersionHelper.isOrAbove1_21_9) {
                    LootParamsProxy.BuilderProxy.INSTANCE.withOptionalParameter(builder, LootContextParamsProxy.ATTACKING_ENTITY, DamageSourceProxy.INSTANCE.getCausingEntity(value));
                    LootParamsProxy.BuilderProxy.INSTANCE.withOptionalParameter(builder, LootContextParamsProxy.DIRECT_ATTACKING_ENTITY, DamageSourceProxy.INSTANCE.getDirectEntity(value));
                }
                // 显式传入的玩家(击杀者)优先, 否则从主体实体推导最近伤害玩家, 与 Map 遍历顺序无关
                if (!contexts.has(DirectContextParameters.PLAYER)) {
                    Entity entity = contexts.getOrNull(DirectContextParameters.ENTITY);
                    if (entity != null) {
                        Object serverEntity = entity.minecraftEntity();
                        if (LivingEntityProxy.CLASS.isInstance(serverEntity)) {
                            Object lastHurtByPlayer = VersionHelper.isOrAbove1_21_5 ?
                                    LivingEntityProxy.INSTANCE.getLastHurtByPlayer(serverEntity) :
                                    LivingEntityProxy.INSTANCE.getLastHurtByPlayerField(serverEntity);
                            LootParamsProxy.BuilderProxy.INSTANCE.withOptionalParameter(builder, LootContextParamsProxy.LAST_DAMAGE_PLAYER, lastHurtByPlayer);
                        }
                    }
                }
            },
            // 仅死亡类上下文(存在伤害源)中的玩家才是击杀者, 对应原版 LAST_DAMAGE_PLAYER
            DirectContextParameters.PLAYER, (builder, value, contexts) -> {
                if (contexts.has(BukkitLootContextParameters.DAMAGE_SOURCE)) {
                    LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.LAST_DAMAGE_PLAYER, ((Player) value).minecraftPlayer());
                }
            },
            DirectContextParameters.EXPLOSION_RADIUS, (builder, value, contexts) ->
                    LootParamsProxy.BuilderProxy.INSTANCE.withOptionalParameter(builder, LootContextParamsProxy.EXPLOSION_RADIUS, value)
    );
    private static BukkitLootManager instance;
    private final BukkitCraftEngine plugin;
    private final Map<LootSourceType<?>, List<Supplier<Listener>>> listenerFactories = new HashMap<>();
    private final Map<LootSourceType<?>, List<Listener>> activeListeners = new HashMap<>();

    public BukkitLootManager(BukkitCraftEngine plugin) {
        if (instance != null) {
            throw new IllegalStateException();
        }
        instance = this;
        this.plugin = plugin;
        this.registerSourceListener(LootSources.BLOCK_BREAK, BlockBreakLootListener::new);
        if (VersionHelper.hasPaperPatch) {
            this.registerSourceListener(LootSources.BLOCK_BREAK, PaperBlockBreakLootListener::new);
            this.registerSourceListener(LootSources.BLOCK_SHEAR, ShearBlockLootListener::new);
            this.registerSourceListener(LootSources.VAULT, VaultLootListener::new);
            this.registerSourceListener(LootSources.ENTITY_SHEAR, () -> new EntityShearLootListener(this.plugin.entityManager()));
        }
        this.registerSourceListener(LootSources.ENTITY_DEATH, () -> new EntityDeathLootListener(this.plugin.entityManager()));
        this.registerSourceListener(LootSources.FISHING, FishingLootListener::new);
        this.registerSourceListener(LootSources.CONTAINER, ContainerLootListener::new);
        this.registerSourceListener(LootSources.PIGLIN_BARTER, PiglinBarterLootListener::new);
        this.registerSourceListener(LootSources.ARCHAEOLOGY, ArchaeologyLootListener::new);
        this.registerSourceListener(LootSources.ENTITY_DROP, () -> new EntityDropLootListener(this.plugin.entityManager()));
        this.registerSourceListener(LootSources.HARVEST, HarvestBlockLootListener::new);
        this.registerSourceListener(LootSources.ADVANCEMENT, AdvancementLootListener::new);
    }

    public static BukkitLootManager instance() {
        return instance;
    }

    public void registerSourceListener(LootSourceType<?> lootSourceType, Supplier<Listener> factory) {
        this.listenerFactories.computeIfAbsent(lootSourceType, k -> new ArrayList<>()).add(factory);
    }

    @Override
    public void runDelayedSyncTasks() {
        for (Map.Entry<LootSourceType<?>, List<Supplier<Listener>>> entry : this.listenerFactories.entrySet()) {
            LootSourceType<?> type = entry.getKey();
            boolean hasSources = type.hasSources();
            List<Listener> active = this.activeListeners.get(type);
            if (hasSources && active == null) {
                List<Listener> listeners = new ArrayList<>(entry.getValue().size());
                for (Supplier<Listener> factory : entry.getValue()) {
                    Listener listener = factory.get();
                    Bukkit.getPluginManager().registerEvents(listener, this.plugin.javaPlugin());
                    listeners.add(listener);
                }
                this.activeListeners.put(type, listeners);
            } else if (!hasSources && active != null) {
                for (Listener listener : active) {
                    HandlerList.unregisterAll(listener);
                }
                this.activeListeners.remove(type);
            }
        }
    }

    @Override
    public void disable() {
        for (List<Listener> listeners : this.activeListeners.values()) {
            for (Listener listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
        }
        this.activeListeners.clear();
    }

    // 遍历上下文元素, 将可识别的参数映射为原版战利品参数
    public Object createMinecraftLootParamsBuilder(LootContext context) {
        ContextHolder contexts = context.contexts();
        Object lootParamsBuilder = LootParamsProxy.BuilderProxy.INSTANCE.newInstance(context.world().minecraftWorld());
        contexts.params().forEach((key, value) -> {
            MinecraftLootParamMapper mapper = MINECRAFT_LOOT_PARAM_MAPPERS.get(key);
            if (mapper == null || value == null) return;
            mapper.accept(lootParamsBuilder, value, contexts);
        });
        LootParamsProxy.BuilderProxy.INSTANCE.withLuck(lootParamsBuilder, context.luck());
        return lootParamsBuilder;
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[]{this.lootParser, this.lootSourceParser};
    }

    @Override
    public LootTableReference createReference(Key key) {
        LazyReference<Loot> lazyReference = LazyReference.untilNotNull(() -> {
            Optional<Loot> lootTable = BukkitLootManager.instance().getLoot(key);
            return lootTable.orElseGet(() -> new DatapackLootTable(key));
        });
        return new LootTableReference(lazyReference);
    }

    @FunctionalInterface
    private interface MinecraftLootParamMapper {
        void accept(Object lootParamsBuilder, Object value, ContextHolder contexts);
    }
}
