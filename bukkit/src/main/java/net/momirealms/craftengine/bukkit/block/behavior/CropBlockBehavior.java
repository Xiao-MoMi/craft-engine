package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.BonemealableBlock;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.SimpleContext;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.Vec3i;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockAndLightGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BonemealableBlockProxy;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public final class CropBlockBehavior extends BukkitBlockBehavior implements BonemealableBlock, RandomTickBlock {
    public static final BlockBehaviorFactory<CropBlockBehavior> FACTORY = new Factory();
    public final IntegerProperty ageProperty;
    public final float growSpeed;
    public final int baseGrowth;
    public final float extraGrowChance;
    public final int minGrowLight;
    public final boolean isBoneMealTarget;
    public final NumberProvider boneMealBonus;

    private CropBlockBehavior(BlockDefinition block, Property<Integer> ageProperty, float growSpeed, int minGrowLight, boolean isBoneMealTarget, NumberProvider boneMealBonus) {
        super(block);
        this.ageProperty = (IntegerProperty) ageProperty;
        this.growSpeed = growSpeed;
        this.baseGrowth = (int) growSpeed;
        this.extraGrowChance = growSpeed - baseGrowth;
        this.minGrowLight = minGrowLight;
        this.isBoneMealTarget = isBoneMealTarget;
        this.boneMealBonus = boneMealBonus;
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return !isMaxAge(state);
    }

    public int getAge(ImmutableBlockState state) {
        return state.get(ageProperty);
    }

    public boolean isMaxAge(ImmutableBlockState state) {
        return state.get(ageProperty) == ageProperty.max;
    }

    public static int getRawBrightness(Object level, Object pos) {
        return BlockAndLightGetterProxy.INSTANCE.getRawBrightness(level, pos, 0);
    }

    private boolean hasSufficientLight(Object level, Object pos) {
        return getRawBrightness(level, pos) >= this.minGrowLight - 1;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        if (getRawBrightness(level, pos) >= this.minGrowLight) {
            BlockStateUtils.getOptionalCustomBlockState(state).ifPresent(customState -> {
                int before = this.getAge(customState);
                if (before < this.ageProperty.max) {
                    int after = before + baseGrowth;
                    if (after < this.ageProperty.max && this.extraGrowChance > 0 && RandomUtils.generateRandomFloat(0, 1) < this.extraGrowChance) {
                        after++;
                    }
                    if (after > before) {
                        if (VersionHelper.isOrAbove1_21_5) {
                            CraftEventFactoryProxy.INSTANCE.handleBlockGrowEvent(level, pos, customState.with(this.ageProperty, after).customBlockState().minecraftState(), UpdateFlags.UPDATE_CLIENTS);
                        } else {
                            CraftEventFactoryProxy.INSTANCE.handleBlockGrowEvent(level, pos, customState.with(this.ageProperty, after).customBlockState().minecraftState());
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args) {
        Object world = args[1];
        Object pos = args[2];
        return hasSufficientLight(world, pos);
    }

    @Override
    public boolean isBonemealSuccess(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public boolean isValidBonemealTarget(Object thisBlock, Object[] args) {
        if (!this.isBoneMealTarget) return false;
        Object state = args[2];
        Optional<ImmutableBlockState> optionalState = BlockStateUtils.getOptionalCustomBlockState(state);
        return optionalState.filter(immutableBlockState -> getAge(immutableBlockState) != this.ageProperty.max).isPresent();
    }

    @Override
    public void performBonemeal(Object thisBlock, Object[] args) {
        this.performBoneMeal(args[0], args[2], args[3]);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Item item = context.getItem();
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
        Object visualState = state.visualBlockState().minecraftState();
        Object visualStateBlock = BlockStateUtils.getBlockOwner(visualState);
        if (BonemealableBlockProxy.CLASS.isInstance(visualStateBlock)) {
            boolean is;
            if (VersionHelper.isOrAbove1_20_2) {
                is = BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(visualStateBlock, world.minecraftWorld(), LocationUtils.toBlockPos(pos), visualState);
            } else {
                is = BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(visualStateBlock, world.minecraftWorld(), LocationUtils.toBlockPos(pos), visualState, true);
            }
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
        Object visualState = customState.visualBlockState().minecraftState();
        Object visualStateBlock = BlockStateUtils.getBlockOwner(visualState);
        if (BonemealableBlockProxy.CLASS.isInstance(visualStateBlock)) {
            boolean is;
            if (VersionHelper.isOrAbove1_20_2) {
                is = BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(visualStateBlock, level, pos, visualState);
            } else {
                is = BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(visualStateBlock, level, pos, visualState, true);
            }
            if (!is) {
                sendParticles = true;
            }
        } else {
            sendParticles = true;
        }
        World world = LevelProxy.INSTANCE.getWorld(level);
        int x = Vec3iProxy.INSTANCE.getX(pos);
        int y = Vec3iProxy.INSTANCE.getY(pos);
        int z = Vec3iProxy.INSTANCE.getZ(pos);
        int before = this.getAge(customState);
        int after = before + this.boneMealBonus.getInt(
                SimpleContext.of(ContextHolder.builder()
                        .withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, customState)
                        .withParameter(DirectContextParameters.POSITION, new WorldPosition(BukkitAdaptor.adapt(world), Vec3d.atCenterOf(new Vec3i(x, y, z))))
                        .build())
        );
        int maxAge = this.ageProperty.max;
        if (after > maxAge) {
            after = maxAge;
        }
        if (after > before) {
            boolean success;
            if (VersionHelper.isOrAbove1_21_5) {
                success = CraftEventFactoryProxy.INSTANCE.handleBlockGrowEvent(level, pos, customState.with(this.ageProperty, after).customBlockState().minecraftState(), UpdateFlags.UPDATE_CLIENTS);
            } else {
                success = CraftEventFactoryProxy.INSTANCE.handleBlockGrowEvent(level, pos, customState.with(this.ageProperty, after).customBlockState().minecraftState());
            }
            if (sendParticles && success) {
                world.spawnParticle(ParticleUtils.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 15, 0.25, 0.25, 0.25);
            }
        }
    }

    private static class Factory implements BlockBehaviorFactory<CropBlockBehavior> {
        private static final String[] GROW_SPEED = new String[]{"grow_speed", "grow-speed"};
        private static final String[] LIGHT_REQUIREMENT = new String[]{"light_requirement", "light-requirement"};
        private static final String[] IS_BONE_MEAL_TARGET = new String[]{"is_bone_meal_target", "is-bone-meal-target"};
        private static final String[] AGE_BONUS = new String[]{"bone_meal_age_bonus", "bone-meal-age-bonus"};

        @Override
        public CropBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new CropBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "age", Integer.class),
                    section.getFloat(GROW_SPEED, 0.125f),
                    section.getInt(LIGHT_REQUIREMENT),
                    section.getBoolean(IS_BONE_MEAL_TARGET, true),
                    section.getNumber(AGE_BONUS, ConfigConstants.CONSTANT_ONE)
            );
        }
    }
}
