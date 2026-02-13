package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.CollisionGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.EntityGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.BooleanOpProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.ShapesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.VoxelShapeProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class CollisionUtils {
    private CollisionUtils() {}

    /**
     * Checks whether the given collection of axis-aligned bounding boxes (AABBs) collides with
     * any block or entity in the world that matches the specified filter.
     *
     * @param level        The world
     * @param aabbs        List of AABB objects representing collision shapes to check
     * @param entityFilter Predicate to test which entities should be considered for collision
     * @return {@code true} if no collision occurs with either blocks or filtered entities,
     *         {@code false} if a collision is detected
     */
    public static boolean test(Object level, List<Object> aabbs, Predicate<Object> entityFilter) {
        if (aabbs.isEmpty()) return true;
        Object combinedShape;
        if (aabbs.size() == 1) {
            combinedShape = ShapesProxy.INSTANCE.create(aabbs.getFirst());
        } else if (aabbs.size() == 2) {
            combinedShape = ShapesProxy.INSTANCE.or(ShapesProxy.INSTANCE.create(aabbs.getFirst()), ShapesProxy.INSTANCE.create(aabbs.getLast()));
        } else {
            List<Object> shapes = new ArrayList<>(aabbs.size() - 1);
            for (int i = 1; i < aabbs.size(); i++) {
                shapes.add(ShapesProxy.INSTANCE.create(aabbs.get(i)));
            }
            Object firstShape = ShapesProxy.INSTANCE.create(aabbs.getFirst());
            combinedShape = shapes.stream().reduce(firstShape, ShapesProxy.INSTANCE::or);
        }
        if (VoxelShapeProxy.INSTANCE.isEmpty(combinedShape)) {
            return true;
        }
        Object bounds = VoxelShapeProxy.INSTANCE.bounds(combinedShape);
        if (CollisionGetterProxy.INSTANCE.getBlockCollisions(level, null, bounds).iterator().hasNext()) {
            return false;
        }
        List<Object> entities = EntityGetterProxy.INSTANCE.getEntities(level, null, bounds);
        for (Object entity : entities) {
            if (entityFilter.test(entity)) {
                if (!EntityProxy.INSTANCE.isRemoved(entity)
                        && EntityProxy.INSTANCE.getBlocksBuilding(entity)
                        && ShapesProxy.INSTANCE.joinIsNotEmpty(combinedShape, ShapesProxy.INSTANCE.create(EntityProxy.INSTANCE.getBoundingBox(entity)), BooleanOpProxy.AND)) {
                    return false;
                }
            }
        }
        return true;
    }
}
