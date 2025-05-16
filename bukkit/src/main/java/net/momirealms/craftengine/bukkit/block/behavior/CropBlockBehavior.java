package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.SimpleContext;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.Tuple;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.Vec3i;
import net.momirealms.craftengine.core.world.WorldPosition;
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
    private final boolean isBoneMealTarget;
    private final NumberProvider boneMealBonus;

    public CropBlockBehavior(CustomBlock block, List<Object> tagsCanSurviveOn, Set<Object> blocksCansSurviveOn, Set<String> customBlocksCansSurviveOn,
                             Property<Integer> ageProperty, float growSpeed, int minGrowLight, boolean isBoneMealTarget, NumberProvider boneMealBonus) {
        super(block, tagsCanSurviveOn, blocksCansSurviveOn, customBlocksCansSurviveOn);
        this.ageProperty = (IntegerProperty) ageProperty;
        this.growSpeed = growSpeed;
        this.minGrowLight = minGrowLight;
        this.isBoneMealTarget = isBoneMealTarget;
        this.boneMealBonus = boneMealBonus;
    }

    public final int getAge(ImmutableBlockState state) {
        return state.get(ageProperty);
    }

    public boolean isMaxAge(ImmutableBlockState state) {
        return state.get(ageProperty) == ageProperty.max;
    }

    public float growSpeed() {
        return growSpeed;
    }

    public boolean isBoneMealTarget() {
        return isBoneMealTarget;
    }

    public NumberProvider boneMealBonus() {
        return boneMealBonus;
    }

    public int minGrowLight() {
        return minGrowLight;
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
                    FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, currentState.with(this.ageProperty, age + 1).customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
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
        return true;
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) {
        if (!this.isBoneMealTarget) return false;
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
            boolean is = FastNMS.INSTANCE.method$BonemealableBlock$isValidBonemealTarget(visualStateBlock, level, pos, visualState);
            if (!is) {
                sendParticles = true;
            }
        } else {
            sendParticles = true;
        }

        World world = FastNMS.INSTANCE.method$Level$getCraftWorld(level);
        int x = FastNMS.INSTANCE.field$Vec3i$x(pos);
        int y = FastNMS.INSTANCE.field$Vec3i$y(pos);
        int z = FastNMS.INSTANCE.field$Vec3i$z(pos);
        int i = this.getAge(immutableBlockState) + this.boneMealBonus.getInt(
                SimpleContext.of(
                        ContextHolder.builder()
                            .withParameter(DirectContextParameters.BLOCK_STATE, immutableBlockState)
                            .withParameter(DirectContextParameters.POSITION, new WorldPosition(new BukkitWorld(world), Vec3d.atCenterOf(new Vec3i(x, y, z))))
                            .build()
                )
        );
        int maxAge = this.ageProperty.max;
        if (i > maxAge) {
            i = maxAge;
        }
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, immutableBlockState.with(this.ageProperty, i).customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
        if (sendParticles) {
            world.spawnParticle(ParticleUtils.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 15, 0.25, 0.25, 0.25);
        }
    }

    public static class Factory implements BlockBehaviorFactory {

        @SuppressWarnings("unchecked")
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments, false);
            Property<Integer> ageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("age"), "warning.config.block.behavior.crop.missing_age");
            int minGrowLight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("light-requirement", 9), "light-requirement");
            float growSpeed = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 0.125f), "grow-speed");
            boolean isBoneMealTarget = (boolean) arguments.getOrDefault("is-bone-meal-target", true);
            NumberProvider boneMealAgeBonus = NumberProviders.fromObject(arguments.getOrDefault("bone-meal-age-bonus", 1));
            return new CropBlockBehavior(block, tuple.left(), tuple.mid(), tuple.right(), ageProperty, growSpeed, minGrowLight, isBoneMealTarget, boneMealAgeBonus);
        }
    }
}
