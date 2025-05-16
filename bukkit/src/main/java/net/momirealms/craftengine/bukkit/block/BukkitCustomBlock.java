package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.util.SoundUtils;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.event.EventTrigger;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.core.util.Tristate;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.shared.ObjectHolder;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class BukkitCustomBlock extends CustomBlock {

    protected BukkitCustomBlock(
            Key id,
            Holder.Reference<CustomBlock> holder,
            Map<String, Property<?>> properties,
            Map<String, Integer> appearances,
            Map<String, VariantState> variantMapper,
            BlockSettings settings,
            @NotNull EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events,
            @Nullable Map<String, Object> behavior,
            @Nullable LootTable<?> lootTable
    ) {
        super(id, holder, properties, appearances, variantMapper, settings, events, behavior, lootTable);
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
            for (ImmutableBlockState state : variantProvider().states()) {
                if (state.vanillaBlockState() == null) {
                    CraftEngine.instance().logger().warn("Could not find vanilla block state for " + state + ". This might cause errors!");
                    continue;
                } else if (state.customBlockState() == null) {
                    CraftEngine.instance().logger().warn("Could not find custom block state for " + state + ". This might cause errors!");
                    continue;
                }
                Object mcBlockState = state.customBlockState().handle();
                BlockSettings settings = state.settings();
                // set block state properties
                BlockStateUtils.setInstrument(mcBlockState, settings.instrument());
                BlockStateUtils.setMapColor(mcBlockState, settings.mapColor());
                BlockStateUtils.setLightEmission(mcBlockState, settings.luminance());
                BlockStateUtils.setBurnable(mcBlockState, settings.burnable());
                BlockStateUtils.setHardness(mcBlockState, settings.hardness());
                BlockStateUtils.setPushReaction(mcBlockState, settings.pushReaction());
                BlockStateUtils.setReplaceable(mcBlockState, settings.replaceable());
                BlockStateUtils.setCanOcclude(mcBlockState, settings.canOcclude());
                if (settings.isRedstoneConductor() == Tristate.TRUE) {
                    BlockStateUtils.setIsRedstoneConductor(mcBlockState, StatePredicate.alwaysTrue());
                } else if (settings.isRedstoneConductor() == Tristate.FALSE) {
                    BlockStateUtils.setIsRedstoneConductor(mcBlockState, StatePredicate.alwaysFalse());
                }
                if (settings.isSuffocating() == Tristate.TRUE) {
                    BlockStateUtils.setIsSuffocating(mcBlockState, StatePredicate.alwaysTrue());
                } else if (settings.isSuffocating() == Tristate.FALSE) {
                    BlockStateUtils.setIsSuffocating(mcBlockState, StatePredicate.alwaysFalse());
                }
                if (settings.isViewBlocking() == Tristate.TRUE) {
                    BlockStateUtils.setIsViewBlocking(mcBlockState, StatePredicate.alwaysTrue());
                } else if (settings.isViewBlocking() == Tristate.FALSE) {
                    BlockStateUtils.setIsViewBlocking(mcBlockState, StatePredicate.alwaysFalse());
                } else {
                    if (settings.isSuffocating() == Tristate.TRUE) {
                        BlockStateUtils.setIsViewBlocking(mcBlockState, StatePredicate.alwaysTrue());
                    } else if (settings.isSuffocating() == Tristate.FALSE) {
                        BlockStateUtils.setIsViewBlocking(mcBlockState, StatePredicate.alwaysFalse());
                    }
                }
                // set parent block properties
                Object mcBlock = BlockStateUtils.getBlockOwner(mcBlockState);
                // bind shape
                Field shapeField = mcBlock.getClass().getField("shapeHolder");
                @SuppressWarnings("unchecked")
                ObjectHolder<BukkitBlockShape> shapeHolder = (ObjectHolder<BukkitBlockShape>) shapeField.get(mcBlock);
                shapeHolder.bindValue(new BukkitBlockShape(state.vanillaBlockState().handle()));
                // bind behavior
                Field behaviorField = mcBlock.getClass().getField("behaviorHolder");
                @SuppressWarnings("unchecked")
                ObjectHolder<BlockBehavior> behaviorHolder = (ObjectHolder<BlockBehavior>) behaviorField.get(mcBlock);
                behaviorHolder.bindValue(super.behavior);
                // set block side properties
                Reflections.field$BlockBehaviour$explosionResistance.set(mcBlock, settings.resistance());
                Reflections.field$BlockBehaviour$soundType.set(mcBlock, SoundUtils.toSoundType(settings.sounds()));
                // init cache
                Reflections.method$BlockStateBase$initCache.invoke(mcBlockState);
                // set block light
                if (settings.blockLight() != -1) {
                    if (VersionHelper.isOrAbove1_21_2()) {
                        Reflections.field$BlockStateBase$lightBlock.set(mcBlockState, settings.blockLight());
                    } else {
                        Object cache = Reflections.field$BlockStateBase$cache.get(mcBlockState);
                        Reflections.field$BlockStateBase$Cache$lightBlock.set(cache, settings.blockLight());
                    }
                }
                // set fluid later
                if (settings.fluidState()) {
                    Reflections.field$BlockStateBase$fluidState.set(mcBlockState, Reflections.method$FlowingFluid$getSource.invoke(Reflections.instance$Fluids$WATER, false));
                } else {
                    Reflections.field$BlockStateBase$fluidState.set(mcBlockState, Reflections.instance$Fluid$EMPTY$defaultState);
                }
                // set random tick later
                BlockStateUtils.setIsRandomlyTicking(mcBlockState, settings.isRandomlyTicking());
                // bind tags
                Object holder = BukkitCraftEngine.instance().blockManager().getMinecraftBlockHolder(state.customBlockState().registryId());
                Set<Object> tags = new HashSet<>();
                for (Key tag : settings.tags()) {
                    tags.add(Reflections.method$TagKey$create.invoke(null, Reflections.instance$Registries$BLOCK, KeyUtils.toResourceLocation(tag)));
                }
                Reflections.field$Holder$Reference$tags.set(holder, tags);
                // set burning properties
                if (settings.burnable()) {
                    Reflections.method$FireBlock$setFlammable.invoke(Reflections.instance$Blocks$FIRE, mcBlock, settings.burnChance(), settings.fireSpreadChance());
                }
                Reflections.field$BlockStateBase$requiresCorrectToolForDrops.set(mcBlockState, settings.requireCorrectTool());
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to init block settings", e);
        }
    }

    public static Builder builder(Key id) {
        return new Builder(id);
    }

    public static class Builder extends CustomBlock.Builder {

        protected Builder(Key id) {
            super(id);
        }

        @Override
        public CustomBlock build() {
            // create or get block holder
            Holder.Reference<CustomBlock> holder = BuiltInRegistries.BLOCK.get(id).orElseGet(() ->
                    ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).registerForHolder(new ResourceKey<>(BuiltInRegistries.BLOCK.key().location(), id)));
            return new BukkitCustomBlock(id, holder, properties, appearances, variantMapper, settings, events, behavior, lootTable);
        }
    }
}
