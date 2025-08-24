package net.momirealms.craftengine.core.world;

/**
 * Represents a block position in a world with integer coordinates.
 * This class is specifically designed to be used as a key in Maps
 * for block-related operations like BlockStateHitBox tracking.
 * 
 * Unlike WorldPosition, this class:
 * - Uses only integer coordinates (suitable for block positions)
 * - Excludes rotation data from equals/hashCode
 * - Has a simpler and more efficient hashCode implementation
 */
public class BlockPosition {
    private final World world;
    private final int x;
    private final int y;
    private final int z;

    public BlockPosition(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a BlockPosition from a WorldPosition by flooring the coordinates
     */
    public static BlockPosition fromWorldPosition(WorldPosition worldPosition) {
        return new BlockPosition(
            worldPosition.world(),
            (int) Math.floor(worldPosition.x()),
            (int) Math.floor(worldPosition.y()),
            (int) Math.floor(worldPosition.z())
        );
    }

    public World world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /**
     * Converts this BlockPosition to a WorldPosition at the center of the block
     */
    public WorldPosition toWorldPosition() {
        return new WorldPosition(world, x + 0.5, y + 0.5, z + 0.5);
    }

    /**
     * Converts this BlockPosition to a WorldPosition at the exact block coordinates
     */
    public WorldPosition toExactWorldPosition() {
        return new WorldPosition(world, x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockPosition other)) return false;
        return x == other.x &&
               y == other.y &&
               z == other.z &&
               world.name().equalsIgnoreCase(other.world.name());
    }

    @Override
    public int hashCode() {
        // Simple and efficient hash for integer coordinates
        int result = world.name().toLowerCase().hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return String.format("BlockPosition{world=%s, x=%d, y=%d, z=%d}", 
            world.name(), x, y, z);
    }
}
