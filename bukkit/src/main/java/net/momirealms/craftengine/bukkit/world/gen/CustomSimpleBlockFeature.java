package net.momirealms.craftengine.bukkit.world.gen;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.DoublePlantBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.MossyCarpetBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy.BlockStateBaseProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.FeaturePlaceContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.SimpleBlockFeatureProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderProxy;

import java.util.Objects;

final class CustomSimpleBlockFeature {
    private CustomSimpleBlockFeature() {
    }

    static boolean placeLegacy(Object context) {
        FeaturePlaceContextProxy access = FeaturePlaceContextProxy.INSTANCE;
        Object level = access.level(context);
        Object pos = access.origin(context);
        Object configuration = access.config(context);
        Object provider = SimpleBlockFeatureProxy.ConfigurationProxy.INSTANCE.toPlace(configuration);
        Object state = CustomStateProvider.sample(provider, level, access.random(context), pos);
        return place(configuration, level, pos, state);
    }

    static boolean placeModern(Object configuration, Object level, Object random, Object pos) {
        Object holder = SimpleBlockFeatureProxy.INSTANCE.toPlace(configuration);
        Object provider = HolderProxy.INSTANCE.value(holder);
        Object state = BlockStateProviderProxy.INSTANCE.getOptionalState(provider, level, random, pos);
        return state != null && place(configuration, level, pos, state);
    }

    private static boolean place(Object configuration, Object level, Object pos, Object state) {
        if (!BlockStateBaseProxy.INSTANCE.canSurvive(state, level, pos)) {
            return false;
        }

        Object block = BlockStateBaseProxy.INSTANCE.getBlock(state);
        ImmutableBlockState customState = BlockStateUtils.getNullableCustomBlockState(state);
        if (customState != null) {
            BlockBehavior behavior = customState.behavior();
            if (behavior.hasMultiState(customState)) {
                return placeMultiState(behavior, customState, level, pos, state, block);
            }
            LevelWriterProxy.INSTANCE.setBlock(level, pos, state, 2);
        } else if (DoublePlantBlockProxy.CLASS.isInstance(block)) {
            Object above = BlockPosProxy.INSTANCE.offset(pos, 0, 1, 0);
            if (!canPlaceUpperHalf(level, state, above)) {
                return false;
            }
            DoublePlantBlockProxy.INSTANCE.placeAt(level, state, pos, 2);
        } else if (VersionHelper.isOrAbove1_21_2 && MossyCarpetBlockProxy.CLASS.isInstance(block)) {
            MossyCarpetBlockProxy.INSTANCE.placeAt(level, pos, LevelAccessorProxy.INSTANCE.getRandom(level), 2);
        } else {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, state, 2);
        }

        boolean scheduleTick = VersionHelper.isOrAbove26_3
                ? SimpleBlockFeatureProxy.INSTANCE.scheduleTick(configuration)
                : VersionHelper.isOrAbove1_21_4 && SimpleBlockFeatureProxy.ConfigurationProxy.INSTANCE.scheduleTick(configuration);
        if (scheduleTick) {
            Object placedState = BlockGetterProxy.INSTANCE.getBlockState(level, pos);
            ScheduledTickAccessProxy.INSTANCE.scheduleTick$0(level, pos, BlockStateBaseProxy.INSTANCE.getBlock(placedState), 1);
        }
        return true;
    }

    private static boolean canPlaceUpperHalf(Object level, Object state, Object above) {
        if (!VersionHelper.isOrAbove26_3) {
            return LevelReaderProxy.INSTANCE.isEmptyBlock(level, above);
        }
        Object aboveState = BlockGetterProxy.INSTANCE.getBlockState(level, above);
        return BlockStateBaseProxy.INSTANCE.isAir(aboveState) || (BlockStateBaseProxy.INSTANCE.canBeReplaced(aboveState)
                && Objects.equals(BlockStateBaseProxy.INSTANCE.getFluidState(state), BlockStateBaseProxy.INSTANCE.getFluidState(aboveState)));
    }

    private static boolean placeMultiState(BlockBehavior behavior,
                                           ImmutableBlockState customState,
                                           Object level,
                                           Object pos,
                                           Object state,
                                           Object block) {
        if (!behavior.canPlaceMultiState(new WorldGenAccess(level), LocationUtils.fromBlockPos(pos), customState)) {
            return false;
        }
        try {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, state, 2);
            behavior.placeMultiState(block, new Object[]{level, pos, state, null, ItemStackProxy.EMPTY});
            return true;
        } catch (Throwable t) {
            CraftEngine.instance().logger().warn("Failed to run placeMultiState", t);
            return false;
        }
    }
}
