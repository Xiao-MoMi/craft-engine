package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.SimpleContext;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.Vec3i;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@SuppressWarnings("DuplicatedCode")
public class CropBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<CropBlockBehavior> FACTORY = new Factory();
    private final IntegerProperty ageProperty;
    private final float growSpeed;
    private final int minGrowLight;
    private final boolean isBoneMealTarget;
    private final NumberProvider boneMealBonus;

    public CropBlockBehavior(CustomBlock block, Property<Integer> ageProperty, float growSpeed, int minGrowLight, boolean isBoneMealTarget, NumberProvider boneMealBonus) {
        super(block);
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

    public static int getRawBrightness(Object level, Object pos) {
        return FastNMS.INSTANCE.method$BlockAndTintGetter$getRawBrightness(level, pos, 0);
    }

    private boolean hasSufficientLight(Object level, Object pos) {
        return getRawBrightness(level, pos) >= this.minGrowLight - 1;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        if (getRawBrightness(level, pos) >= this.minGrowLight) {
            BlockStateUtils.getOptionalCustomBlockState(state).ifPresent(customState -> {
                int age = this.getAge(customState);
                if (age < this.ageProperty.max && RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
                    FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, customState.with(this.ageProperty, age + 1).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
                }
            });
        }
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object world = args[1];
        Object pos = args[2];
        return hasSufficientLight(world, pos);
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) {
        if (!this.isBoneMealTarget) return false;
        Object state = args[2];
        Optional<ImmutableBlockState> optionalState = BlockStateUtils.getOptionalCustomBlockState(state);
        return optionalState.filter(immutableBlockState -> getAge(immutableBlockState) != this.ageProperty.max).isPresent();
    }

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) {
        this.performBoneMeal(args[0], args[2], args[3]);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Item<?> item = context.getItem();
        Player player = context.getPlayer();
        if (ItemUtils.isEmpty(item) || !item.vanillaId().equals(ItemKeys.BONE_MEAL) || player == null || player.isAdventureMode())
            return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        net.momirealms.craftengine.core.world.World world = context.getLevel();
        Location location = new Location((World) world.platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.INTERACT, location)) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        if (isMaxAge(state))
            return InteractionResult.PASS;
        boolean sendSwing = false;
        Object visualState = state.visualBlockState().literalObject();
        Object visualStateBlock = BlockStateUtils.getBlockOwner(visualState);
        if (CoreReflections.clazz$BonemealableBlock.isInstance(visualStateBlock)) {
            boolean is = FastNMS.INSTANCE.method$BonemealableBlock$isValidBonemealTarget(visualStateBlock, world.serverWorld(), LocationUtils.toBlockPos(pos), visualState);
            if (!is) {
                sendSwing = true;
            }
        } else {
            sendSwing = true;
        }
        if (sendSwing) {
            player.swingHand(context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    private void performBoneMeal(Object level, Object pos, Object state) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) {
            return;
        }
        ImmutableBlockState customState = optionalCustomState.get();
        boolean sendParticles = false;
        Object visualState = customState.visualBlockState().literalObject();
        Object visualStateBlock = BlockStateUtils.getBlockOwner(visualState);
        if (CoreReflections.clazz$BonemealableBlock.isInstance(visualStateBlock)) {
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
        int i = this.getAge(customState) + this.boneMealBonus.getInt(
                SimpleContext.of(ContextHolder.builder()
                            .withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, customState)
                            .withParameter(DirectContextParameters.POSITION, new WorldPosition(BukkitAdaptors.adapt(world), Vec3d.atCenterOf(new Vec3i(x, y, z))))
                            .build())
        );
        int maxAge = this.ageProperty.max;
        if (i > maxAge) {
            i = maxAge;
        }
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, customState.with(this.ageProperty, i).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
        if (sendParticles) {
            world.spawnParticle(ParticleUtils.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 15, 0.25, 0.25, 0.25);
        }
    }

    private static class Factory implements BlockBehaviorFactory<CropBlockBehavior> {

        @SuppressWarnings("unchecked")
        @Override
        public CropBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Integer> ageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("age"), "warning.config.block.behavior.crop.missing_age");
            int minGrowLight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("light-requirement", 9), "light-requirement");
            float growSpeed = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 0.125f), "grow-speed");
            boolean isBoneMealTarget = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("is-bone-meal-target", true), "is-bone-meal-target");
            NumberProvider boneMealAgeBonus = NumberProviders.fromObject(arguments.getOrDefault("bone-meal-age-bonus", 1));
            return new CropBlockBehavior(block, ageProperty, growSpeed, minGrowLight, isBoneMealTarget, boneMealAgeBonus);
        }
    }
}
