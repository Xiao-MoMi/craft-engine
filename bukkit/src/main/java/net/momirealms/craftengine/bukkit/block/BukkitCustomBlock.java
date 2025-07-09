package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.block.behavior.UnsafeCompositeBlockBehavior;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.SoundUtils;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.block.behavior.EmptyBlockBehavior;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class BukkitCustomBlock extends AbstractCustomBlock {
    private static final Object ALWAYS_FALSE = FastNMS.INSTANCE.method$StatePredicate$always(false);
    private static final Object ALWAYS_TRUE = FastNMS.INSTANCE.method$StatePredicate$always(true);

    private BukkitCustomBlock(
            @NotNull Key id,
            @NotNull Holder.Reference<CustomBlock> holder,
            @NotNull Map<String, Property<?>> properties,
            @NotNull Map<String, Integer> appearances,
            @NotNull Map<String, BlockStateVariant> variantMapper,
            @NotNull BlockSettings settings,
            @NotNull Map<EventTrigger, List<Function<PlayerOptionalContext>>> events,
            @Nullable List<Map<String, Object>> behavior,
            @Nullable LootTable<?> lootTable
    ) {
        super(id, holder, properties, appearances, variantMapper, settings, events, behavior, lootTable);
    }

    @Override
    protected BlockBehavior setupBehavior(List<Map<String, Object>> behaviorConfig) {
        if (behaviorConfig == null || behaviorConfig.isEmpty()) {
            return new EmptyBlockBehavior();
        } else if (behaviorConfig.size() == 1) {
            return BlockBehaviors.fromMap(this, behaviorConfig.getFirst());
        } else {
            List<AbstractBlockBehavior> behaviors = new ArrayList<>();
            for (Map<String, Object> config : behaviorConfig) {
                behaviors.add((AbstractBlockBehavior) BlockBehaviors.fromMap(this, config));
            }
            return new UnsafeCompositeBlockBehavior(this, behaviors);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public LootTable<ItemStack> lootTable() {
        return (LootTable<ItemStack>) super.lootTable();
    }

    @Override
    protected void applyPlatformSettings() {
        try {
            for (ImmutableBlockState immutableBlockState : variantProvider().states()) {
                if (immutableBlockState.vanillaBlockState() == null) {
                    CraftEngine.instance().logger().warn("Could not find vanilla block immutableBlockState for " + immutableBlockState + ". This might cause errors!");
                    continue;
                } else if (immutableBlockState.customBlockState() == null) {
                    CraftEngine.instance().logger().warn("Could not find custom block immutableBlockState for " + immutableBlockState + ". This might cause errors!");
                    continue;
                }
                DelegatingBlockState nmsState = (DelegatingBlockState) immutableBlockState.customBlockState().handle();
                nmsState.setBlockState(immutableBlockState);
                BlockSettings settings = immutableBlockState.settings();

                // set block properties
                CoreReflections.field$BlockStateBase$lightEmission.set(nmsState, settings.luminance());
                CoreReflections.field$BlockStateBase$burnable.set(nmsState, settings.burnable());
                CoreReflections.field$BlockStateBase$hardness.set(nmsState, settings.hardness());
                CoreReflections.field$BlockStateBase$replaceable.set(nmsState, settings.replaceable());
                Object mcMapColor = CoreReflections.method$MapColor$byId.invoke(null, settings.mapColor().id);
                CoreReflections.field$BlockStateBase$mapColor.set(nmsState, mcMapColor);
                Object mcInstrument = ((Object[]) CoreReflections.method$NoteBlockInstrument$values.invoke(null))[settings.instrument().ordinal()];
                CoreReflections.field$BlockStateBase$instrument.set(nmsState, mcInstrument);
                Object pushReaction = ((Object[]) CoreReflections.method$PushReaction$values.invoke(null))[settings.pushReaction().ordinal()];
                CoreReflections.field$BlockStateBase$pushReaction.set(nmsState, pushReaction);

                boolean canOcclude = settings.canOcclude() == Tristate.UNDEFINED ? BlockStateUtils.isOcclude(immutableBlockState.vanillaBlockState().handle()) : settings.canOcclude().asBoolean();
                CoreReflections.field$BlockStateBase$canOcclude.set(nmsState, canOcclude);

                boolean useShapeForLightOcclusion = settings.useShapeForLightOcclusion() == Tristate.UNDEFINED ? CoreReflections.field$BlockStateBase$useShapeForLightOcclusion.getBoolean(immutableBlockState.vanillaBlockState().handle()) : settings.useShapeForLightOcclusion().asBoolean();
                CoreReflections.field$BlockStateBase$useShapeForLightOcclusion.set(nmsState, useShapeForLightOcclusion);

                CoreReflections.field$BlockStateBase$isRedstoneConductor.set(nmsState, settings.isRedstoneConductor().asBoolean() ? ALWAYS_TRUE : ALWAYS_FALSE);
                CoreReflections.field$BlockStateBase$isSuffocating.set(nmsState, settings.isSuffocating().asBoolean() ? ALWAYS_TRUE : ALWAYS_FALSE);
                CoreReflections.field$BlockStateBase$isViewBlocking.set(nmsState, settings.isViewBlocking() == Tristate.UNDEFINED ? settings.isSuffocating().asBoolean() ? ALWAYS_TRUE : ALWAYS_FALSE : (settings.isViewBlocking().asBoolean() ? ALWAYS_TRUE : ALWAYS_FALSE));

                // set parent block properties
                DelegatingBlock nmsBlock = (DelegatingBlock) BlockStateUtils.getBlockOwner(nmsState);
                ObjectHolder<BlockShape> shapeHolder = nmsBlock.shapeDelegate();
                shapeHolder.bindValue(new BukkitBlockShape(immutableBlockState.vanillaBlockState().handle(), Optional.ofNullable(immutableBlockState.settings().supportShapeBlockState()).map(it -> {
                    try {
                        Object blockState = BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData(it));
                        if (!BlockStateUtils.isVanillaBlock(blockState)) {
                            throw new IllegalArgumentException("BlockState is not a Vanilla block");
                        }
                        return blockState;
                    } catch (IllegalArgumentException e) {
                        CraftEngine.instance().logger().warn("Illegal shape block state: " + it, e);
                        return null;
                    }
                }).orElse(null)));
                // bind behavior
                ObjectHolder<BlockBehavior> behaviorHolder = nmsBlock.behaviorDelegate();
                behaviorHolder.bindValue(super.behavior);
                // set block side properties
                CoreReflections.field$BlockBehaviour$explosionResistance.set(nmsBlock, settings.resistance());
                CoreReflections.field$BlockBehaviour$soundType.set(nmsBlock, SoundUtils.toSoundType(settings.sounds()));
                // 1.21.2以前要在init cache之前设定 isConditionallyFullOpaque
                boolean isConditionallyFullOpaque = canOcclude & useShapeForLightOcclusion;
                if (!VersionHelper.isOrAbove1_21_2()) {
                    CoreReflections.field$BlockStateBase$isConditionallyFullOpaque.set(nmsState, isConditionallyFullOpaque);
                }
                // init cache
                CoreReflections.method$BlockStateBase$initCache.invoke(nmsState);
                // modify cache
                if (VersionHelper.isOrAbove1_21_2()) {
                    int blockLight = settings.blockLight() != -1 ? settings.blockLight() : CoreReflections.field$BlockStateBase$lightBlock.getInt(immutableBlockState.vanillaBlockState().handle());
                    // set block light
                    CoreReflections.field$BlockStateBase$lightBlock.set(nmsState, blockLight);
                    // set propagates skylight
                    if (settings.propagatesSkylightDown() == Tristate.TRUE) {
                        CoreReflections.field$BlockStateBase$propagatesSkylightDown.set(nmsState, true);
                    } else if (settings.propagatesSkylightDown() == Tristate.FALSE) {
                        CoreReflections.field$BlockStateBase$propagatesSkylightDown.set(nmsState, false);
                    } else {
                        CoreReflections.field$BlockStateBase$propagatesSkylightDown.set(nmsState, CoreReflections.field$BlockStateBase$propagatesSkylightDown.getBoolean(immutableBlockState.vanillaBlockState().handle()));
                    }
                } else {
                    Object cache = CoreReflections.field$BlockStateBase$cache.get(nmsState);
                    int blockLight = settings.blockLight() != -1 ? settings.blockLight() : CoreReflections.field$BlockStateBase$Cache$lightBlock.getInt(CoreReflections.field$BlockStateBase$cache.get(immutableBlockState.vanillaBlockState().handle()));
                    // set block light
                    CoreReflections.field$BlockStateBase$Cache$lightBlock.set(cache, blockLight);
                    // set propagates skylight
                    if (settings.propagatesSkylightDown() == Tristate.TRUE) {
                        CoreReflections.field$BlockStateBase$Cache$propagatesSkylightDown.set(cache, true);
                    } else if (settings.propagatesSkylightDown() == Tristate.FALSE) {
                        CoreReflections.field$BlockStateBase$Cache$propagatesSkylightDown.set(cache, false);
                    } else {
                        CoreReflections.field$BlockStateBase$Cache$propagatesSkylightDown.set(cache, CoreReflections.field$BlockStateBase$Cache$propagatesSkylightDown.getBoolean(CoreReflections.field$BlockStateBase$cache.get(immutableBlockState.vanillaBlockState().handle())));
                    }
                    if (!isConditionallyFullOpaque) {
                        CoreReflections.field$BlockStateBase$opacityIfCached.set(nmsState, blockLight);
                    }
                }
                // set fluid later
                if (settings.fluidState()) {
                    CoreReflections.field$BlockStateBase$fluidState.set(nmsState, CoreReflections.method$FlowingFluid$getSource.invoke(MFluids.WATER, false));
                } else {
                    CoreReflections.field$BlockStateBase$fluidState.set(nmsState, MFluids.EMPTY$defaultState);
                }
                // set random tick later
                CoreReflections.field$BlockStateBase$isRandomlyTicking.set(nmsState, settings.isRandomlyTicking());
                // bind tags
                Object holder = BukkitCraftEngine.instance().blockManager().getMinecraftBlockHolder(immutableBlockState.customBlockState().registryId());
                Set<Object> tags = new HashSet<>();
                for (Key tag : settings.tags()) {
                    tags.add(CoreReflections.method$TagKey$create.invoke(null, MRegistries.BLOCK, KeyUtils.toResourceLocation(tag)));
                }
                CoreReflections.field$Holder$Reference$tags.set(holder, tags);
                // set burning properties
                if (settings.burnable()) {
                    CoreReflections.method$FireBlock$setFlammable.invoke(MBlocks.FIRE, nmsBlock, settings.burnChance(), settings.fireSpreadChance());
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to init block settings", e);
        }
    }

    public static Builder builder(Key id) {
        return new BuilderImpl(id);
    }

    public static class BuilderImpl implements Builder {
        protected final Key id;
        protected Map<String, Property<?>> properties;
        protected Map<String, Integer> appearances;
        protected Map<String, BlockStateVariant> variantMapper;
        protected BlockSettings settings;
        protected List<Map<String, Object>> behavior;
        protected LootTable<?> lootTable;
        protected Map<EventTrigger, List<Function<PlayerOptionalContext>>> events;

        public BuilderImpl(Key id) {
            this.id = id;
        }

        @Override
        public Builder events(Map<EventTrigger, List<Function<PlayerOptionalContext>>> events) {
            this.events = events;
            return this;
        }

        @Override
        public Builder appearances(Map<String, Integer> appearances) {
            this.appearances = appearances;
            return this;
        }

        @Override
        public Builder behavior(List<Map<String, Object>> behavior) {
            this.behavior = behavior;
            return this;
        }

        @Override
        public Builder lootTable(LootTable<?> lootTable) {
            this.lootTable = lootTable;
            return this;
        }

        @Override
        public Builder properties(Map<String, Property<?>> properties) {
            this.properties = properties;
            return this;
        }

        @Override
        public Builder settings(BlockSettings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public Builder variantMapper(Map<String, BlockStateVariant> variantMapper) {
            this.variantMapper = variantMapper;
            return this;
        }

        @Override
        public @NotNull CustomBlock build() {
            // create or get block holder
            Holder.Reference<CustomBlock> holder = BuiltInRegistries.BLOCK.get(id).orElseGet(() ->
                    ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).registerForHolder(new ResourceKey<>(BuiltInRegistries.BLOCK.key().location(), id)));
            return new BukkitCustomBlock(id, holder, properties, appearances, variantMapper, settings, events, behavior, lootTable);
        }
    }
}
