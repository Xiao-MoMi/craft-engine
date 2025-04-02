package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.Tuple;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import org.bukkit.World;

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
    private final Pair<Integer, Integer> boneMealGrowthStageRange;
    private final boolean canUseBoneMeal;

    public CropBlockBehavior(List<Object> tagsCanSurviveOn, Set<Object> blocksCansSurviveOn, Set<String> customBlocksCansSurviveOn,
                             Property<Integer> ageProperty, float growSpeed, int minGrowLight, Pair<Integer, Integer> boneMealGrowthStageRange,
                             boolean canUseBoneMeal) {
        super(tagsCanSurviveOn, blocksCansSurviveOn, customBlocksCansSurviveOn);
        this.ageProperty = (IntegerProperty) ageProperty;
        this.growSpeed = growSpeed;
        this.minGrowLight = minGrowLight;
        this.boneMealGrowthStageRange = boneMealGrowthStageRange;
        this.canUseBoneMeal = canUseBoneMeal;
    }

    public final int getAge(ImmutableBlockState state) {
        return state.get(ageProperty);
    }

    public boolean isMaxAge(ImmutableBlockState state) {
        return state.get(ageProperty) == ageProperty.max;
    }

    private static int getRawBrightness(Object level, Object pos) throws InvocationTargetException, IllegalAccessException {
        return (int) Reflections.method$BlockAndTintGetter$getRawBrightness.invoke(level, pos, 0);
    }

    private boolean hasSufficientLight(Object level, Object pos) throws InvocationTargetException, IllegalAccessException {
        return getRawBrightness(level, pos) >= this.minGrowLight - 1;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        if (getRawBrightness(level, pos) >= this.minGrowLight) {
            ImmutableBlockState currentState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(state));
            if (currentState != null && !currentState.isEmpty()) {
                int age = this.getAge(currentState);
                if (age < this.ageProperty.max && RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    Reflections.method$Level$setBlock.invoke(level, pos, currentState.with(this.ageProperty, age + 1).customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
                }
            }
        }
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws ReflectiveOperationException {
        return hasSufficientLight(world, blockPos) && super.canSurvive(thisBlock, state, world, blockPos);
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) {
        return this.canUseBoneMeal;
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) {
        if (!this.canUseBoneMeal) return false;
        Object state = args[2];
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(state));
        if (immutableBlockState != null && !immutableBlockState.isEmpty()) {
            return getAge(immutableBlockState) != this.ageProperty.max;
        } else {
            return false;
        }
    }

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {
        if (!this.canUseBoneMeal) return;
        this.performBoneMeal(args[0], args[2], args[3]);
    }

    private void performBoneMeal(Object level, Object pos, Object state) throws InvocationTargetException, IllegalAccessException {
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(state));
        if (immutableBlockState == null || immutableBlockState.isEmpty()) {
            return;
        }
        boolean sendParticles = false;
        Object visualState = immutableBlockState.vanillaBlockState().handle();
        Object visualStateBlock = Reflections.method$BlockStateBase$getBlock.invoke(visualState);
        if (Reflections.clazz$BonemealableBlock.isInstance(visualStateBlock)) {
            boolean is = (boolean) Reflections.method$BonemealableBlock$isValidBonemealTarget.invoke(visualStateBlock, level, pos, visualState);
            if (!is) {
                sendParticles = true;
            }
        } else {
            sendParticles = true;
        }

        int i = this.getAge(immutableBlockState) + RandomUtils.generateRandomInt(boneMealGrowthStageRange.left(), boneMealGrowthStageRange.right());
        int maxAge = this.ageProperty.max;
        if (i > maxAge) {
            i = maxAge;
        }
        Reflections.method$Level$setBlock.invoke(level, pos, immutableBlockState.with(this.ageProperty, i).customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
        if (sendParticles) {
            World world = FastNMS.INSTANCE.method$Level$getCraftWorld(level);
            int x = FastNMS.INSTANCE.field$Vec3i$x(pos);
            int y = FastNMS.INSTANCE.field$Vec3i$y(pos);
            int z = FastNMS.INSTANCE.field$Vec3i$z(pos);
            world.spawnParticle(ParticleUtils.getParticle("HAPPY_VILLAGER"), x + 0.5, y + 0.5, z + 0.5, 12, 0.25, 0.25, 0.25);
        }
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
            int minGrowLight = MiscUtils.getAsInt(arguments.getOrDefault("light-requirement", 9));
            float growSpeed = MiscUtils.getAsFloat(arguments.getOrDefault("grow-speed", 0.25f));
            Pair<Integer, Integer> boneMealGrowthStageRange = MiscUtils.getIntPair(arguments.getOrDefault("bone-meal-growth-stage-range", "2~5"));
            boolean canUseBoneMeal = (boolean) arguments.getOrDefault("can-use-bone-meal", true);
            return new CropBlockBehavior(tuple.left(), tuple.mid(), tuple.right(), ageProperty, growSpeed, minGrowLight, boneMealGrowthStageRange, canUseBoneMeal);
        }
    }
}
