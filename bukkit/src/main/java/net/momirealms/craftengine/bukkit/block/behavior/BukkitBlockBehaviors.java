package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.block.behavior.EmptyBlockBehavior;
import net.momirealms.craftengine.core.util.Key;

public class BukkitBlockBehaviors extends BlockBehaviors {
    public static final Key BUSH_BLOCK = Key.from("craftengine:bush_block");
    public static final Key HANGING_BLOCK = Key.from("craftengine:hanging_block");
    public static final Key FALLING_BLOCK = Key.from("craftengine:falling_block");
    public static final Key LEAVES_BLOCK = Key.from("craftengine:leaves_block");
    public static final Key STRIPPABLE_BLOCK = Key.from("craftengine:strippable_block");
    public static final Key SAPLING_BLOCK = Key.from("craftengine:sapling_block");
    public static final Key ON_LIQUID_BLOCK = Key.from("craftengine:on_liquid_block");
    public static final Key NEAR_LIQUID_BLOCK = Key.from("craftengine:near_liquid_block");
    public static final Key CONCRETE_POWDER_BLOCK = Key.from("craftengine:concrete_powder_block");
    public static final Key VERTICAL_CROP_BLOCK = Key.from("craftengine:vertical_crop_block");
    public static final Key CROP_BLOCK = Key.from("craftengine:crop_block");
    public static final Key GRASS_BLOCK = Key.from("craftengine:grass_block");
    public static final Key LAMP_BLOCK = Key.from("craftengine:lamp_block");
    public static final Key TRAPDOOR_BLOCK = Key.from("craftengine:trapdoor_block");
    public static final Key DOOR_BLOCK = Key.from("craftengine:door_block");
    public static final Key STACKABLE_BLOCK = Key.from("craftengine:stackable_block");
    public static final Key STURDY_BASE_BLOCK = Key.from("craftengine:sturdy_base_block");
    public static final Key FENCE_GATE_BLOCK = Key.from("craftengine:fence_gate_block");
    public static final Key SLAB_BLOCK = Key.from("craftengine:slab_block");
    public static final Key STAIRS_BLOCK = Key.from("craftengine:stairs_block");
    public static final Key PRESSURE_PLATE_BLOCK = Key.from("craftengine:pressure_plate_block");
    public static final Key DOUBLE_HIGH_BLOCK = Key.from("craftengine:double_high_block");
    public static final Key CHANGE_OVER_TIME_BLOCK = Key.from("craftengine:change_over_time_block");
    public static final Key SIMPLE_STORAGE_BLOCK = Key.from("craftengine:simple_storage_block");
    public static final Key TOGGLEABLE_LAMP_BLOCK = Key.from("craftengine:toggleable_lamp_block");
    public static final Key SOFA_BLOCK = Key.from("craftengine:sofa_block");
    public static final Key BOUNCING_BLOCK = Key.from("craftengine:bouncing_block");
    public static final Key DIRECTIONAL_ATTACHED_BLOCK = Key.from("craftengine:directional_attached_block");
    public static final Key LIQUID_FLOWABLE_BLOCK = Key.from("craftengine:liquid_flowable_block");
    public static final Key SIMPLE_PARTICLE_BLOCK = Key.from("craftengine:simple_particle_block");
    public static final Key WALL_TORCH_PARTICLE_BLOCK = Key.from("craftengine:wall_torch_particle_block");

    public static void init() {
        register(EMPTY, (block, args) -> EmptyBlockBehavior.INSTANCE);
        register(FALLING_BLOCK, FallingBlockBehavior.FACTORY);
        register(BUSH_BLOCK, BushBlockBehavior.FACTORY);
        register(HANGING_BLOCK, HangingBlockBehavior.FACTORY);
        register(LEAVES_BLOCK, LeavesBlockBehavior.FACTORY);
        register(STRIPPABLE_BLOCK, StrippableBlockBehavior.FACTORY);
        register(SAPLING_BLOCK, SaplingBlockBehavior.FACTORY);
        register(ON_LIQUID_BLOCK, OnLiquidBlockBehavior.FACTORY);
        register(NEAR_LIQUID_BLOCK, NearLiquidBlockBehavior.FACTORY);
        register(CONCRETE_POWDER_BLOCK, ConcretePowderBlockBehavior.FACTORY);
        register(VERTICAL_CROP_BLOCK, VerticalCropBlockBehavior.FACTORY);
        register(CROP_BLOCK, CropBlockBehavior.FACTORY);
        register(GRASS_BLOCK, GrassBlockBehavior.FACTORY);
        register(LAMP_BLOCK, LampBlockBehavior.FACTORY);
        register(TRAPDOOR_BLOCK, TrapDoorBlockBehavior.FACTORY);
        register(DOOR_BLOCK, DoorBlockBehavior.FACTORY);
        register(STACKABLE_BLOCK, StackableBlockBehavior.FACTORY);
        register(STURDY_BASE_BLOCK, SturdyBaseBlockBehavior.FACTORY);
        register(FENCE_GATE_BLOCK, FenceGateBlockBehavior.FACTORY);
        register(SLAB_BLOCK, SlabBlockBehavior.FACTORY);
        register(STAIRS_BLOCK, StairsBlockBehavior.FACTORY);
        register(PRESSURE_PLATE_BLOCK, PressurePlateBlockBehavior.FACTORY);
        register(DOUBLE_HIGH_BLOCK, DoubleHighBlockBehavior.FACTORY);
        register(CHANGE_OVER_TIME_BLOCK, ChangeOverTimeBlockBehavior.FACTORY);
        register(SIMPLE_STORAGE_BLOCK, SimpleStorageBlockBehavior.FACTORY);
        register(TOGGLEABLE_LAMP_BLOCK, ToggleableLampBlockBehavior.FACTORY);
        register(SOFA_BLOCK, SofaBlockBehavior.FACTORY);
        register(BOUNCING_BLOCK, BouncingBlockBehavior.FACTORY);
        register(DIRECTIONAL_ATTACHED_BLOCK, DirectionalAttachedBlockBehavior.FACTORY);
        register(LIQUID_FLOWABLE_BLOCK, LiquidFlowableBlockBehavior.FACTORY);
        register(SIMPLE_PARTICLE_BLOCK, SimpleParticleBlockBehavior.FACTORY);
        register(WALL_TORCH_PARTICLE_BLOCK, WallTorchParticleBlockBehavior.FACTORY);
    }
}
