package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.behavior.BlockBehaviorType;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.util.Key;

public class BukkitBlockBehaviors extends BlockBehaviors {
    private BukkitBlockBehaviors() {}

    public static final BlockBehaviorType<BushBlockBehavior> BUSH_BLOCK = register(Key.ce("bush_block"), BushBlockBehavior.FACTORY);
    public static final BlockBehaviorType<HangingBlockBehavior> HANGING_BLOCK = register(Key.ce("hanging_block"), HangingBlockBehavior.FACTORY);
    public static final BlockBehaviorType<FallingBlockBehavior> FALLING_BLOCK = register(Key.ce("falling_block"), FallingBlockBehavior.FACTORY);
    public static final BlockBehaviorType<LeavesBlockBehavior> LEAVES_BLOCK = register(Key.ce("leaves_block"), LeavesBlockBehavior.FACTORY);
    public static final BlockBehaviorType<StrippableBlockBehavior> STRIPPABLE_BLOCK = register(Key.ce("strippable_block"), StrippableBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SaplingBlockBehavior> SAPLING_BLOCK = register(Key.ce("sapling_block"), SaplingBlockBehavior.FACTORY);
    public static final BlockBehaviorType<OnLiquidBlockBehavior> ON_LIQUID_BLOCK = register(Key.ce("on_liquid_block"), OnLiquidBlockBehavior.FACTORY);
    public static final BlockBehaviorType<NearLiquidBlockBehavior> NEAR_LIQUID_BLOCK = register(Key.ce("near_liquid_block"), NearLiquidBlockBehavior.FACTORY);
    public static final BlockBehaviorType<ConcretePowderBlockBehavior> CONCRETE_POWDER_BLOCK = register(Key.ce("concrete_powder_block"), ConcretePowderBlockBehavior.FACTORY);
    public static final BlockBehaviorType<VerticalCropBlockBehavior> VERTICAL_CROP_BLOCK = register(Key.ce("vertical_crop_block"), VerticalCropBlockBehavior.FACTORY);
    public static final BlockBehaviorType<CropBlockBehavior> CROP_BLOCK = register(Key.ce("crop_block"), CropBlockBehavior.FACTORY);
    public static final BlockBehaviorType<GrassBlockBehavior> GRASS_BLOCK = register(Key.ce("grass_block"), GrassBlockBehavior.FACTORY);
    public static final BlockBehaviorType<LampBlockBehavior> LAMP_BLOCK = register(Key.ce("lamp_block"), LampBlockBehavior.FACTORY);
    public static final BlockBehaviorType<TrapDoorBlockBehavior> TRAPDOOR_BLOCK = register(Key.ce("trapdoor_block"), TrapDoorBlockBehavior.FACTORY);
    public static final BlockBehaviorType<DoorBlockBehavior> DOOR_BLOCK = register(Key.ce("door_block"), DoorBlockBehavior.FACTORY);
    public static final BlockBehaviorType<StackableBlockBehavior> STACKABLE_BLOCK = register(Key.ce("stackable_block"), StackableBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SturdyBaseBlockBehavior> STURDY_BASE_BLOCK = register(Key.ce("sturdy_base_block"), SturdyBaseBlockBehavior.FACTORY);
    public static final BlockBehaviorType<FenceGateBlockBehavior> FENCE_GATE_BLOCK = register(Key.ce("fence_gate_block"), FenceGateBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SlabBlockBehavior> SLAB_BLOCK = register(Key.ce("slab_block"), SlabBlockBehavior.FACTORY);
    public static final BlockBehaviorType<StairsBlockBehavior> STAIRS_BLOCK = register(Key.ce("stairs_block"), StairsBlockBehavior.FACTORY);
    public static final BlockBehaviorType<PressurePlateBlockBehavior> PRESSURE_PLATE_BLOCK = register(Key.ce("pressure_plate_block"), PressurePlateBlockBehavior.FACTORY);
    public static final BlockBehaviorType<DoubleHighBlockBehavior> DOUBLE_HIGH_BLOCK = register(Key.ce("double_high_block"), DoubleHighBlockBehavior.FACTORY);
    public static final BlockBehaviorType<ChangeOverTimeBlockBehavior> CHANGE_OVER_TIME_BLOCK = register(Key.ce("change_over_time_block"), ChangeOverTimeBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SimpleStorageBlockBehavior> SIMPLE_STORAGE_BLOCK = register(Key.ce("simple_storage_block"), SimpleStorageBlockBehavior.FACTORY);
    public static final BlockBehaviorType<ToggleableLampBlockBehavior> TOGGLEABLE_LAMP_BLOCK = register(Key.ce("toggleable_lamp_block"), ToggleableLampBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SofaBlockBehavior> SOFA_BLOCK = register(Key.ce("sofa_block"), SofaBlockBehavior.FACTORY);
    public static final BlockBehaviorType<BouncingBlockBehavior> BOUNCING_BLOCK = register(Key.ce("bouncing_block"), BouncingBlockBehavior.FACTORY);
    public static final BlockBehaviorType<DirectionalAttachedBlockBehavior> DIRECTIONAL_ATTACHED_BLOCK = register(Key.ce("directional_attached_block"), DirectionalAttachedBlockBehavior.FACTORY);
    public static final BlockBehaviorType<LiquidFlowableBlockBehavior> LIQUID_FLOWABLE_BLOCK = register(Key.ce("liquid_flowable_block"), LiquidFlowableBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SimpleParticleBlockBehavior> SIMPLE_PARTICLE_BLOCK = register(Key.ce("simple_particle_block"), SimpleParticleBlockBehavior.FACTORY);
    public static final BlockBehaviorType<WallTorchParticleBlockBehavior> WALL_TORCH_PARTICLE_BLOCK = register(Key.ce("wall_torch_particle_block"), WallTorchParticleBlockBehavior.FACTORY);
    public static final BlockBehaviorType<FenceBlockBehavior> FENCE_BLOCK = register(Key.ce("fence_block"), FenceBlockBehavior.FACTORY);
    public static final BlockBehaviorType<ButtonBlockBehavior> BUTTON_BLOCK = register(Key.ce("button_block"), ButtonBlockBehavior.FACTORY);
    public static final BlockBehaviorType<FaceAttachedHorizontalDirectionalBlockBehavior> FACE_ATTACHED_HORIZONTAL_DIRECTIONAL_BLOCK = register(Key.ce("face_attached_horizontal_directional_block"), FaceAttachedHorizontalDirectionalBlockBehavior.FACTORY);
    public static final BlockBehaviorType<StemBlockBehavior> STEM_BLOCK = register(Key.ce("stem_block"), StemBlockBehavior.FACTORY);
    public static final BlockBehaviorType<AttachedStemBlockBehavior> ATTACHED_STEM_BLOCK = register(Key.ce("attached_stem_block"), AttachedStemBlockBehavior.FACTORY);
    public static final BlockBehaviorType<ChimeBlockBehavior> CHIME_BLOCK = register(Key.ce("chime_block"), ChimeBlockBehavior.FACTORY);
    public static final BlockBehaviorType<BuddingBlockBehavior> BUDDING_BLOCK = register(Key.ce("budding_block"), BuddingBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SeatBlockBehavior> SEAT_BLOCK = register(Key.ce("seat_block"), SeatBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SurfaceSpreadingBlockBehavior> SURFACE_SPREADING_BLOCK = register(Key.ce("surface_spreading_block"), SurfaceSpreadingBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SnowyBlockBehavior> SNOWY_BLOCK = register(Key.ce("snowy_block"), SnowyBlockBehavior.FACTORY);
    public static final BlockBehaviorType<HangableBlockBehavior> HANGABLE_BLOCK = register(Key.ce("hangable_block"), HangableBlockBehavior.FACTORY);
    public static final BlockBehaviorType<DropExperienceBlockBehavior> DROP_EXPERIENCE_BLOCK = register(Key.ce("drop_experience_block"), DropExperienceBlockBehavior.FACTORY);
    public static final BlockBehaviorType<DropExperienceBlockBehavior> DROP_EXP_BLOCK = register(Key.ce("drop_exp_block"), DropExperienceBlockBehavior.FACTORY);
    public static final BlockBehaviorType<MultiHighBlockBehavior> MULTI_HIGH_BLOCK = register(Key.ce("multi_high_block"), MultiHighBlockBehavior.FACTORY);
    public static final BlockBehaviorType<SpreadingBlockBehavior> SPREADING_BLOCK = register(Key.ce("spreading_block"), SpreadingBlockBehavior.FACTORY);
    public static final BlockBehaviorType<ItemFrameBlockBehavior> ITEM_FRAME_BLOCK = register(Key.ce("item_frame_block"), ItemFrameBlockBehavior.FACTORY);

    public static void init() {
    }
}
