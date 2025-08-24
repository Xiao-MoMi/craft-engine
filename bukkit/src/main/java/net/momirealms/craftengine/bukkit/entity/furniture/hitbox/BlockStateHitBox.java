package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.entity.furniture.AbstractHitBox;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.HitBox;
import net.momirealms.craftengine.core.entity.furniture.HitBoxFactory;
import net.momirealms.craftengine.core.entity.furniture.HitBoxTypes;
import net.momirealms.craftengine.core.entity.furniture.Seat;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;

/**
 * A HitBox that places actual blocks in the world for visual purposes.
 * 
 * This hitbox is designed to be primarily client-side visual, avoiding server-side
 * collision detection and piston interactions. For actual collision detection,
 * use other hitbox types like AABBHitBox or ColliderHitBox.
 * 
 * Key features:
 * - Places blocks with minimal server-side impact
 * - Prevents piston movement of placed blocks
 * - Does not generate movement collision by default
 * - Supports container dropping when removed
 */
public class BlockStateHitBox extends AbstractHitBox {
    public static final Factory FACTORY = new Factory();
    
    private final LazyReference<BlockStateWrapper> lazyBlockState;
    private final boolean dropContainer;
    private final boolean invertRotation;
    private WorldPosition placedPosition;
    private BlockStateWrapper originalBlockState;
    private BukkitFurniture parentFurniture;
    private boolean actuallyPlacedBlock = false; // Track if we actually placed the block or reused existing existing

    public BlockStateHitBox(Seat[] seats, Vector3f position, LazyReference<BlockStateWrapper> lazyBlockState, 
                           boolean canUseOn, boolean blocksBuilding, boolean canBeHitByProjectile, boolean dropContainer,
                           boolean invertRotation) {
        super(seats, position, canUseOn, blocksBuilding, canBeHitByProjectile);
        this.lazyBlockState = lazyBlockState;
        this.dropContainer = dropContainer;
        this.invertRotation = invertRotation;
    }

    public LazyReference<BlockStateWrapper> blockState() {
        return lazyBlockState;
    }

    public boolean dropContainer() {
        return dropContainer;
    }

    public boolean invertRotation() {
        return invertRotation;
    }

    public BukkitFurniture getParentFurniture() {
        return parentFurniture;
    }

    public void setParentFurniture(BukkitFurniture parentFurniture) {
        this.parentFurniture = parentFurniture;
    }

    @Override
    public Key type() {
        return HitBoxTypes.BLOCKSTATE;
    }

    @Override
    public void initPacketsAndColliders(int[] entityId, WorldPosition position, Quaternionf conjugated, 
                                       BiConsumer<Object, Boolean> packets, Consumer<Collider> collider, 
                                       BiConsumer<Integer, AABB> aabb) {
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        World world = position.world();
        int blockX = (int) Math.floor(position.x() + offset.x);
        int blockY = (int) Math.floor(position.y() + offset.y);
        int blockZ = (int) Math.floor(position.z() - offset.z);
        
        // Store the placed position for later removal
        this.placedPosition = new WorldPosition(world, blockX, blockY, blockZ);
        
        // Check if this position is already registered to this furniture
        Furniture existingFurniture = CraftEngine.instance().furnitureManager().getFurnitureByBlockPosition(this.placedPosition);
        boolean blockAlreadyPlaced = false;
        
        // Also check if the existing block is the same type we want to place
        boolean sameBlockType = false;
        try {
            org.bukkit.World bukkitWorld = (org.bukkit.World) world.platformWorld();
            org.bukkit.block.data.BlockData existingBlockData = bukkitWorld.getBlockAt(blockX, blockY, blockZ).getBlockData();
            BlockStateWrapper targetBlockState = lazyBlockState.get();
            if (targetBlockState != null) {
                // Convert our target block state to bukkit block data for comparison
                String targetMaterial = getBlockMaterial(targetBlockState);
                String existingMaterial = existingBlockData.getMaterial().getKey().toString();
                sameBlockType = targetMaterial.equals(existingMaterial);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to compare block types", e);
        }
        
        if (existingFurniture != null && parentFurniture != null && existingFurniture.equals(parentFurniture)) {
            // Block is already placed by this same furniture, reuse it
            blockAlreadyPlaced = true;
            CraftEngine.instance().logger().info("Reusing existing BlockStateHitBox at position: " + this.placedPosition + " for furniture: " + parentFurniture.id());
        } else if (existingFurniture != null) {
            // Block belongs to different furniture, clean it up first
            CraftEngine.instance().logger().warn("Found existing BlockStateHitBox at position: " + this.placedPosition + 
                " belonging to different furniture. Cleaning up before placing new block.");
            CraftEngine.instance().furnitureManager().unregisterBlockStateHitBox(this.placedPosition);
        } else if (sameBlockType) {
            // Same block type exists but no furniture ownership, reuse it
            blockAlreadyPlaced = true;
            CraftEngine.instance().logger().info("Reusing existing block of same type at position: " + this.placedPosition);
        }
        
        if (!blockAlreadyPlaced) {
            // Store the original block state before placing our block
            try {
                // Get the bukkit block data from the world
                org.bukkit.World bukkitWorld = (org.bukkit.World) world.platformWorld();
                org.bukkit.block.data.BlockData blockData = bukkitWorld.getBlockAt(blockX, blockY, blockZ).getBlockData();
                this.originalBlockState = BlockStateUtils.toPackedBlockState(blockData);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to get original block state", e);
                // Fallback to air
                this.originalBlockState = CraftEngine.instance().blockManager().createPackedBlockState("minecraft:air");
            }
            
            // Place the block with client-side flags to avoid server collision and piston movement
            BlockStateWrapper blockStateWrapper = lazyBlockState.get();
            if (blockStateWrapper != null) {
                // Use UPDATE_CLIENTS only to make it visible but avoid server-side collision
                // Add SUPPRESS_DROPS flag to prevent interference, EXPLICITLY EXCLUDE UPDATE_MOVE_BY_PISTON to prevent piston movement
                int flags = UpdateOption.Flags.UPDATE_CLIENTS | 
                           UpdateOption.Flags.UPDATE_SUPPRESS_DROPS;
                           // NOTE: NOT including UPDATE_MOVE_BY_PISTON flag to prevent piston movement
                world.setBlockAt(blockX, blockY, blockZ, blockStateWrapper, flags);
                this.actuallyPlacedBlock = true; // Mark that we placed this block
                
                // Apply rotation to the placed block if supported
                applyRotationToBlock(world, blockX, blockY, blockZ, conjugated);
                
                // Register this block position with the FurnitureManager for tracking
                if (parentFurniture != null) {
                    // Register with the furniture entity's PersistentDataContainer with complete data
                    // This ensures persistence across server restarts and proper cleanup
                    parentFurniture.addBlockStateHitBoxData(
                        this.placedPosition, 
                        this.originalBlockState, 
                        this.dropContainer, 
                        this.actuallyPlacedBlock
                    );
                    
                    // Register with the manager using the simplified method
                    CraftEngine.instance().furnitureManager().registerBlockStateHitBox(this.placedPosition, parentFurniture);
                }
            }
        } else {
            // Block already exists, just ensure it's registered and apply rotation
            this.actuallyPlacedBlock = false; // We didn't place it, just reused it
            
            // Apply rotation to the existing block if supported
            applyRotationToBlock(world, blockX, blockY, blockZ, conjugated);
            
            if (parentFurniture != null) {
                // Register with the furniture entity's PersistentDataContainer with complete data
                // Even for reused blocks, we need to track them for proper cleanup
                parentFurniture.addBlockStateHitBoxData(
                    this.placedPosition, 
                    this.originalBlockState, // This will be null for reused blocks
                    this.dropContainer, 
                    this.actuallyPlacedBlock // false for reused blocks
                );
                
                // Register with the manager using the simplified method
                CraftEngine.instance().furnitureManager().registerBlockStateHitBox(this.placedPosition, parentFurniture);
                
            }
        }

        // Only add AABB for interaction if canUseItemOn is enabled
        // Do not add collision AABB since this is a visual-only hitbox
        if (canUseItemOn()) {
            aabb.accept(entityId[0], new AABB(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1));
        }
        
        // Note: No colliders are added since this hitbox is for visual purposes only
        // Use other hitbox types for actual collision detection
    }

    @Override
    public void initShapeForPlacement(double x, double y, double z, float yaw, Quaternionf conjugated, Consumer<AABB> aabbs) {
        // Only block building if explicitly configured to do so
        // By default, BlockStateHitBox should be visual-only and not interfere with building
        if (blocksBuilding()) {
            Vector3f offset = conjugated.transform(new Vector3f(position()));
            int blockX = (int) Math.floor(x + offset.x);
            int blockY = (int) Math.floor(y + offset.y);
            int blockZ = (int) Math.floor(z - offset.z);
            aabbs.accept(new AABB(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1));
        }
        // Note: Consider setting blocks-building to false in configuration for visual-only blocks
    }

    @Override
    public int[] acquireEntityIds(Supplier<Integer> entityIdSupplier) {
        return new int[] {entityIdSupplier.get()};
    }

    /**
     * Extract the material name from a BlockStateWrapper
     */
    private String getBlockMaterial(BlockStateWrapper blockState) {
        try {
            // Convert to bukkit block data to get material
            Object nmsBlockState = blockState.handle();
            org.bukkit.block.data.BlockData blockData = BlockStateUtils.fromBlockData(nmsBlockState);
            return blockData.getMaterial().getKey().toString();
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to get block material from BlockStateWrapper", e);
            return "minecraft:air";
        }
    }

    /**
     * Apply rotation from furniture to block properties like facing, axis, etc.
     */
    private void applyRotationToBlock(World world, int x, int y, int z, Quaternionf conjugated) {
        try {
            org.bukkit.World bukkitWorld = (org.bukkit.World) world.platformWorld();
            if (bukkitWorld == null) return;
            
            org.bukkit.block.Block block = bukkitWorld.getBlockAt(x, y, z);
            org.bukkit.block.data.BlockData blockData = block.getBlockData();
            
            // Apply inversion if needed
            Quaternionf rotationToApply = conjugated;
            if (invertRotation) {
                rotationToApply = new Quaternionf(conjugated).invert();
            }
            
            // Calculate facing direction from quaternion
            org.bukkit.block.BlockFace facing = calculateFacingFromQuaternion(rotationToApply);
            org.bukkit.Axis axis = calculateAxisFromQuaternion(rotationToApply);
            
            boolean modified = false;
            
            // Apply facing property if the block supports it
            if (blockData instanceof org.bukkit.block.data.Directional directional) {
                if (directional.getFaces().contains(facing)) {
                    directional.setFacing(facing);
                    modified = true;
                }
            }
            
            // Apply axis property if the block supports it
            if (blockData instanceof org.bukkit.block.data.Orientable orientable) {
                if (orientable.getAxes().contains(axis)) {
                    orientable.setAxis(axis);
                    modified = true;
                }
            }
            
            // Apply rotation property if the block supports it (like signs, banners)
            if (blockData instanceof org.bukkit.block.data.Rotatable rotatable) {
                org.bukkit.block.BlockFace rotation = calculateRotationFromQuaternion(rotationToApply);
                rotatable.setRotation(rotation);
                modified = true;
            }
            
            // Update the block if we modified any properties
            if (modified) {
                block.setBlockData(blockData, false); // Don't trigger physics updates
                CraftEngine.instance().logger().info("Applied rotation to block at " + x + "," + y + "," + z + 
                    " - facing: " + facing + ", axis: " + axis);
            }
            
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to apply rotation to block", e);
        }
    }

    /**
     * Calculate BlockFace direction from quaternion rotation
     */
    private org.bukkit.block.BlockFace calculateFacingFromQuaternion(Quaternionf quaternion) {
        // Convert quaternion to euler angles
        Vector3f euler = new Vector3f();
        quaternion.getEulerAnglesYXZ(euler);
        
        // Normalize the Y rotation (yaw) to determine facing
        float yaw = (float) Math.toDegrees(euler.y);
        yaw = ((yaw % 360) + 360) % 360; // Normalize to 0-360
        
        // Map yaw to BlockFace (minecraft uses different conventions)
        if (yaw >= 315 || yaw < 45) {
            return org.bukkit.block.BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return org.bukkit.block.BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return org.bukkit.block.BlockFace.NORTH;
        } else {
            return org.bukkit.block.BlockFace.EAST;
        }
    }

    /**
     * Calculate Axis from quaternion rotation
     */
    private org.bukkit.Axis calculateAxisFromQuaternion(Quaternionf quaternion) {
        Vector3f euler = new Vector3f();
        quaternion.getEulerAnglesYXZ(euler);
        
        // Determine the primary axis based on rotation
        float pitch = Math.abs((float) Math.toDegrees(euler.x));
        float roll = Math.abs((float) Math.toDegrees(euler.z));
        
        // Choose the axis with the most significant rotation
        if (pitch > 45 && pitch > roll) {
            return org.bukkit.Axis.Y;
        } else if (roll > 45) {
            return org.bukkit.Axis.Z;
        } else {
            return org.bukkit.Axis.X;
        }
    }

    /**
     * Calculate rotation for Rotatable blocks (like signs, banners)
     */
    private org.bukkit.block.BlockFace calculateRotationFromQuaternion(Quaternionf quaternion) {
        Vector3f euler = new Vector3f();
        quaternion.getEulerAnglesYXZ(euler);
        
        float yaw = (float) Math.toDegrees(euler.y);
        yaw = ((yaw % 360) + 360) % 360;
        
        // Map to 16 possible rotations (360 / 16 = 22.5 degrees per step)
        int rotationStep = Math.round(yaw / 22.5f) % 16;
        
        return switch (rotationStep) {
            case 0 -> org.bukkit.block.BlockFace.SOUTH;
            case 1 -> org.bukkit.block.BlockFace.SOUTH_SOUTH_WEST;
            case 2 -> org.bukkit.block.BlockFace.SOUTH_WEST;
            case 3 -> org.bukkit.block.BlockFace.WEST_SOUTH_WEST;
            case 4 -> org.bukkit.block.BlockFace.WEST;
            case 5 -> org.bukkit.block.BlockFace.WEST_NORTH_WEST;
            case 6 -> org.bukkit.block.BlockFace.NORTH_WEST;
            case 7 -> org.bukkit.block.BlockFace.NORTH_NORTH_WEST;
            case 8 -> org.bukkit.block.BlockFace.NORTH;
            case 9 -> org.bukkit.block.BlockFace.NORTH_NORTH_EAST;
            case 10 -> org.bukkit.block.BlockFace.NORTH_EAST;
            case 11 -> org.bukkit.block.BlockFace.EAST_NORTH_EAST;
            case 12 -> org.bukkit.block.BlockFace.EAST;
            case 13 -> org.bukkit.block.BlockFace.EAST_SOUTH_EAST;
            case 14 -> org.bukkit.block.BlockFace.SOUTH_EAST;
            case 15 -> org.bukkit.block.BlockFace.SOUTH_SOUTH_EAST;
            default -> org.bukkit.block.BlockFace.SOUTH;
        };
    }

    public static class Factory implements HitBoxFactory {

        @Override
        public HitBox create(Map<String, Object> arguments) {
            Vector3f position = net.momirealms.craftengine.core.util.MiscUtils.getAsVector3f(
                arguments.getOrDefault("position", "0"), "position");
            
            String blockStateString = ResourceConfigUtils.requireNonEmptyStringOrThrow(
                arguments.get("block-state"), "warning.config.furniture.hitbox.blockstate.missing_block_state");
            
            boolean canUseOn = ResourceConfigUtils.getAsBoolean(
                arguments.getOrDefault("can-use-item-on", false), "can-use-item-on");
            // Changed default to false since BlockStateHitBox should be visual-only by default
            boolean blocksBuilding = ResourceConfigUtils.getAsBoolean(
                arguments.getOrDefault("blocks-building", false), "blocks-building");
            boolean canBeHitByProjectile = ResourceConfigUtils.getAsBoolean(
                arguments.getOrDefault("can-be-hit-by-projectile", false), "can-be-hit-by-projectile");
            boolean dropContainer = ResourceConfigUtils.getAsBoolean(
                arguments.getOrDefault("drop-container", true), "drop-container");
            boolean invertRotation = ResourceConfigUtils.getAsBoolean(
                arguments.getOrDefault("invert-rotation", false), "invert-rotation");
            
            LazyReference<BlockStateWrapper> lazyBlockState = LazyReference.lazyReference(
                () -> CraftEngine.instance().blockManager().createPackedBlockState(blockStateString));
            
            return new BlockStateHitBox(
                HitBoxFactory.getSeats(arguments),
                position,
                lazyBlockState,
                canUseOn,
                blocksBuilding,
                canBeHitByProjectile,
                dropContainer,
                invertRotation
            );
        }
    }
}