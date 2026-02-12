package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.IsPathFindableBlockBehavior;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class StemBlockBehavior extends BukkitBlockBehavior implements IsPathFindableBlockBehavior {
    public static final BlockBehaviorFactory<StemBlockBehavior> FACTORY = new Factory();
    private final IntegerProperty ageProperty;
    private final Key fruit;
    private final Key attachedStem;
    private final int minGrowLight;
    private final Object tagMayPlaceFruit;
    private final Object blockMayPlaceFruit;

    public StemBlockBehavior(CustomBlock customBlock,
                             IntegerProperty ageProperty,
                             Key fruit,
                             Key attachedStem,
                             int minGrowLight,
                             Object tagMayPlaceFruit,
                             Object blockMayPlaceFruit) {
        super(customBlock);
        this.ageProperty = ageProperty;
        this.fruit = fruit;
        this.attachedStem = attachedStem;
        this.minGrowLight = minGrowLight;
        this.tagMayPlaceFruit = tagMayPlaceFruit;
        this.blockMayPlaceFruit = blockMayPlaceFruit;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return (VersionHelper.isOrAbove1_20_5() ? args[1] : args[3]).equals(CoreReflections.instance$PathComputationType$AIR)
                && !FastNMS.INSTANCE.field$BlockBehavior$hasCollision(thisBlock) || (boolean) superMethod.call();
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        if (CropBlockBehavior.getRawBrightness(level, pos) < this.minGrowLight) return;
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (customState == null || customState.isEmpty()) return;
        int age = customState.get(ageProperty);
        if (age < ageProperty.max) {
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, customState.with(ageProperty, age + 1).customBlockState().literalObject(), 2);
            return;
        }
        Object randomDirection = CoreReflections.instance$Direction$values[RandomUtils.generateRandomInt(2, 6)];
        Object blockPos = FastNMS.INSTANCE.method$BlockPos$relative(pos, randomDirection);
        if (!FastNMS.INSTANCE.method$BlockStateBase$isAir(FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, blockPos)))
            return;
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, FastNMS.INSTANCE.method$BlockPos$relative(blockPos, CoreReflections.instance$Direction$DOWN));
        if (mayPlaceFruit(blockState)) {
            Optional<CustomBlock> optionalFruit = BukkitBlockManager.instance().blockById(this.fruit);
            Object fruitState = null;
            if (optionalFruit.isPresent()) {
                fruitState = optionalFruit.get().defaultState().customBlockState().literalObject();
            } else if (fruit.namespace().equals("minecraft")) {
                fruitState = FastNMS.INSTANCE.method$Block$defaultState(FastNMS.INSTANCE.method$Registry$getValue(
                        MBuiltInRegistries.BLOCK,
                        FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", fruit.value())
                ));
            }
            Optional<CustomBlock> optionalAttachedStem = BukkitBlockManager.instance().blockById(this.attachedStem);
            if (fruitState == null || optionalAttachedStem.isEmpty()) return;
            CustomBlock attachedStem = optionalAttachedStem.get();
            @SuppressWarnings("unchecked")
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) attachedStem.getProperty("facing");
            if (facing == null) return;
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, fruitState, UpdateOption.UPDATE_ALL.flags());
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, attachedStem.defaultState().with(facing, DirectionUtils.fromNMSDirection(randomDirection).toHorizontalDirection()).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
        }
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[2]).orElse(null);
        if (state == null || state.isEmpty()) return false;
        return state.get(ageProperty) != ageProperty.max;
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[3]).orElse(null);
        if (state == null || state.isEmpty()) return;
        int min = Math.min(7, state.get(ageProperty) + RandomUtils.generateRandomInt(Math.min(ageProperty.min + 2, ageProperty.max), Math.min(ageProperty.max - 2, ageProperty.max)));
        Object blockState = state.with(ageProperty, min).customBlockState().literalObject();
        FastNMS.INSTANCE.method$LevelWriter$setBlock(args[0], args[2], blockState, 2);
        if (min >= ageProperty.max) {
            FastNMS.INSTANCE.method$BlockBehaviour$BlockStateBase$randomTick(blockState, args[0], args[2]);
        }
    }

    private boolean mayPlaceFruit(Object blockState) {
        boolean flag1 = tagMayPlaceFruit != null && FastNMS.INSTANCE.method$BlockStateBase$is(blockState, tagMayPlaceFruit);
        boolean flag2 = blockMayPlaceFruit != null && FastNMS.INSTANCE.method$BlockStateBase$isBlock(blockState, blockMayPlaceFruit);
        if (tagMayPlaceFruit == null && blockMayPlaceFruit == null) return true;
        return flag1 || flag2;
    }

    private static class Factory implements BlockBehaviorFactory<StemBlockBehavior> {

        @Override
        public StemBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            IntegerProperty ageProperty = (IntegerProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("age"), "warning.config.block.behavior.stem.missing_age");
            Key fruit = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("fruit"), "warning.config.block.behavior.stem.missing_fruit"));
            Key attachedStem = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("attached-stem"), "warning.config.block.behavior.stem.missing_attached_stem"));
            int minGrowLight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("light-requirement", 9), "light-requirement");
            Object tagMayPlaceFruit = FastNMS.INSTANCE.method$TagKey$create(MRegistries.BLOCK, KeyUtils.toResourceLocation(Key.of(arguments.getOrDefault("may-place-fruit", "minecraft:dirt").toString())));
            Object blockMayPlaceFruit = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK, KeyUtils.toResourceLocation(Key.of(arguments.getOrDefault("may-place-fruit", "minecraft:farmland").toString())));
            return new StemBlockBehavior(block, ageProperty, fruit, attachedStem, minGrowLight, tagMayPlaceFruit, blockMayPlaceFruit);
        }
    }
}
