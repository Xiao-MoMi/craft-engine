package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.Tuple;
import net.momirealms.craftengine.shared.block.BlockBehavior;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class CropBlockBehavior extends BushBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final IntegerProperty ageProperty;
    private final float growSpeed;
    private final int minGrowLight;

    public CropBlockBehavior(List<Object> tagsCanSurviveOn, Set<Object> blocksCansSurviveOn, Set<String> customBlocksCansSurviveOn,
                             Property<Integer> ageProperty, float growSpeed, int minGrowLight) {
        super(tagsCanSurviveOn, blocksCansSurviveOn, customBlocksCansSurviveOn);
        this.ageProperty = (IntegerProperty) ageProperty;
        this.growSpeed = growSpeed;
        this.minGrowLight = minGrowLight;
    }

    public final boolean isMaxAge(Object state) {
        return this.getAge(state) >= this.ageProperty.max;
    }

    public static ImmutableBlockState getCEBlockState(Object nmsState) {
        return BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(nmsState));
    }

    public final int getAge(Object state) {
        return getCEBlockState(state).get(ageProperty);
    }

    public Object getStateForAge(Object state, int age) {
        ImmutableBlockState afterState = getCEBlockState(state).owner().value().defaultState().with(ageProperty, age);
        return afterState.customBlockState().handle();
    }

    public void growCrops(Object level, Object pos, Object state) throws InvocationTargetException, IllegalAccessException {
        int i = this.getAge(state) + RandomUtils.generateRandomInt(2, 5);
        int maxAge = this.ageProperty.max;
        if (i > maxAge) {
            i = maxAge;
        }
        Reflections.method$ServerLevel$sendBlockUpdated.invoke(level, pos, state, getStateForAge(state, i), 3);
    }

    private static int getRawBrightness(Object level, Object pos) throws InvocationTargetException, IllegalAccessException {
        return (int) Reflections.method$BlockAndTintGetter$getRawBrightness.invoke(level, pos, 0);
    }

    private boolean hasSufficientLight(Object level, Object pos) throws InvocationTargetException, IllegalAccessException {
        return getRawBrightness(level, pos) >= minGrowLight - 1;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        if (getRawBrightness(level, pos) >= minGrowLight) {
            int age = this.getAge(state);
            if (age < this.ageProperty.max && RandomUtils.generateRandomFloat(0, 1) >= this.growSpeed) {
                Reflections.method$ServerLevel$sendBlockUpdated.invoke(level, pos, state, getStateForAge(state, age + 1), 3);
            }
        }
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws ReflectiveOperationException {
        return hasSufficientLight(world, blockPos) && super.canSurvive(thisBlock, state, world, blockPos);
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) {
        Object state = args[2];
        return !this.isMaxAge(state);
    }

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {
        this.growCrops(args[0], args[2], args[3]);
    }

    public static class Factory implements BlockBehaviorFactory {

        @SuppressWarnings("unchecked")
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments);
            Property<Integer> ageProperty = (Property<Integer>) block.getProperty("age");
            if (ageProperty == null) {
                throw new IllegalArgumentException("age property not set for crop");
            }
            // 存活条件是最小生长亮度-1
            int minGrowLight = MiscUtils.getAsInt(arguments.getOrDefault("min-grow-light", 9));
            float growSpeed = MiscUtils.getAsFloat(arguments.getOrDefault("grow-speed", 1));
            return new CropBlockBehavior(tuple.left(), tuple.mid(), tuple.right(), ageProperty, growSpeed, minGrowLight);
        }
    }
}
