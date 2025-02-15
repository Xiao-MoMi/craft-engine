package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.MCUtils;
import org.bukkit.Particle;

public class ParticleUtils {
    private static Particle BLOCK_CRACK;

    static {
        try {
            BLOCK_CRACK = Particle.valueOf("BLOCK_CRACK");
        } catch (IllegalArgumentException e) {
            BLOCK_CRACK = Particle.BLOCK;
        }
    }

    private ParticleUtils() {}

    public static void addBlockBreakParticles(org.bukkit.World bukkitWorld, Object pos, Object state) {
        try {
            Object world = Reflections.field$CraftWorld$ServerLevel.get(bukkitWorld);
            Object shape = Reflections.method$BlockState$getShape.invoke(state, world, pos, Reflections.method$CollisionContext$empty.invoke(null));
            boolean isEmpty = (boolean) Reflections.method$VoxelShape$isEmpty.invoke(shape);
            if (isEmpty) {
                return;
            }

            Object aabb = Reflections.method$VoxelShape$bounds.invoke(shape);
            double minX = Reflections.field$AABB$minX.getDouble(aabb);
            double minY = Reflections.field$AABB$minY.getDouble(aabb);
            double minZ = Reflections.field$AABB$minZ.getDouble(aabb);
            double maxX = Reflections.field$AABB$maxX.getDouble(aabb);
            double maxY = Reflections.field$AABB$maxY.getDouble(aabb);
            double maxZ = Reflections.field$AABB$maxZ.getDouble(aabb);

            double d = Math.min(1.0, maxX - minX);
            double e = Math.min(1.0, maxY - minY);
            double f = Math.min(1.0, maxZ - minZ);
            int i = Math.max(2, MCUtils.ceil(d / 0.25));
            int j = Math.max(2, MCUtils.ceil(e / 0.25));
            int k = Math.max(2, MCUtils.ceil(f / 0.25));

            Object data = Reflections.method$CraftBlockData$createData.invoke(null, state);
            for (int l = 0; l < i; ++l) {
                for (int m = 0; m < j; ++m) {
                    for (int n = 0; n < k; ++n) {
                        double g = ((double) l + 0.5) / (double) i;
                        double h = ((double) m + 0.5) / (double) j;
                        double o = ((double) n + 0.5) / (double) k;
                        double p = g * d + minX;
                        double q = h * e + minY;
                        double r = o * f + minZ;
                        bukkitWorld.spawnParticle(BLOCK_CRACK,
                                (double) Reflections.field$Vec3i$x.getInt(pos) + p,
                                (double) Reflections.field$Vec3i$y.getInt(pos) + q,
                                (double) Reflections.field$Vec3i$z.getInt(pos) + r,
                                1,
                                0,
                                0,
                                0,
                                0,
                                data
                        );
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to add block break particles", e);
        }
    }
}
