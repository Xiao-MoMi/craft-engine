package net.momirealms.craftengine.core.entity.furniture;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ColliderMergePlan {
    private static final int[][] AXIS_ORDERS = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
    public static final ColliderMergePlan EMPTY = new ColliderMergePlan(List.of());
    public final boolean hasMerges;
    private final List<Merge> merges;

    private ColliderMergePlan(List<Merge> merges) {
        this.merges = List.copyOf(merges);
        this.hasMerges = !this.merges.isEmpty();
    }

    public static ColliderMergePlan compile(List<? extends FurnitureHitBoxConfig<?>> configs) {
        if (configs.size() < 2) return EMPTY;
        LinkedHashSet<Merge> candidates = new LinkedHashSet<>();
        for (float yaw : new float[]{180, 0, 90, 270}) {
            List<ColliderConfig> boxes = new ArrayList<>(configs.size());
            WorldPosition origin = new WorldPosition(null, 0, 0, 0, 0, yaw);
            for (int configIndex = 0, configCount = configs.size(); configIndex < configCount; configIndex++) {
                FurnitureHitBoxConfig<?> config = configs.get(configIndex);
                ColliderProperties properties = config.colliderProperties();
                List<AABB> bounds = new ArrayList<>(1);
                if (properties != null)
                    config.prepareBoundingBox(origin, bounds::add, true);
                boxes.add(bounds.size() == 1 ? new ColliderConfig(bounds.getFirst(), properties) : null);
            }
            List<Merge> merges = compileBoxes(boxes).merges;
            for (int mergeIndex = 0, mergeCount = merges.size(); mergeIndex < mergeCount; mergeIndex++) {
                Merge merge = merges.get(mergeIndex);
                candidates.add(merge);
            }
        }
        return candidates.isEmpty() ? EMPTY : new ColliderMergePlan(List.copyOf(candidates));
    }

    public static ColliderMergePlan compileBoxes(List<ColliderConfig> input) {
        if (input.size() < 2) return EMPTY;
        ColliderConfig[] candidates = input.toArray(ColliderConfig[]::new);
        for (int i = 0; i < candidates.length; i++) {
            ColliderConfig config = candidates[i];
            if (config != null && !finiteVolume(config.bounds)) candidates[i] = null;
        }
        List<Merge> best = List.of();
        for (int order = 0; order < AXIS_ORDERS.length; order++) {
            int[] axes = AXIS_ORDERS[order];
            // The last pass can consume the validated buffer directly.
            ColliderConfig[] boxes = order == AXIS_ORDERS.length - 1 ? candidates : candidates.clone();
            List<Merge> merges = new ArrayList<>();
            boolean changed;
            do {
                changed = false;
                for (int axis : axes) {
                    for (int i = 0; i < boxes.length; i++) {
                        if (boxes[i] == null) continue;
                        for (int j = i + 1; j < boxes.length; j++) {
                            if (boxes[j] == null || boxes[i].properties != boxes[j].properties) continue;
                            AABB union = union(boxes[i].bounds, boxes[j].bounds, axis);
                            if (union == null) continue;
                            boxes[i] = new ColliderConfig(union, boxes[i].properties);
                            boxes[j] = null;
                            merges.add(new Merge(i, j));
                            changed = true;
                        }
                    }
                }
            } while (changed);
            if (merges.size() > best.size()) best = merges;
        }
        return best.isEmpty() ? EMPTY : new ColliderMergePlan(best);
    }

    private static int root(int[] parents, int index) {
        while (parents[index] != index) {
            parents[index] = parents[parents[index]];
            index = parents[index];
        }
        return index;
    }

    public static AABB exactUnion(AABB a, AABB b) {
        if (!finiteVolume(a) || !finiteVolume(b)) return null;
        return unionValidated(a, b);
    }

    private static AABB unionValidated(AABB a, AABB b) {
        for (int axis = 0; axis < 3; axis++) {
            AABB union = union(a, b, axis);
            if (union != null) return union;
        }
        return null;
    }

    private static AABB union(AABB a, AABB b, int axis) {
        if (contains(a, b)) return a;
        if (contains(b, a)) return b;
        boolean aligned = switch (axis) {
            case 0 -> a.minY == b.minY && a.maxY == b.maxY && a.minZ == b.minZ && a.maxZ == b.maxZ && a.minX <= b.maxX && b.minX <= a.maxX;
            case 1 -> a.minX == b.minX && a.maxX == b.maxX && a.minZ == b.minZ && a.maxZ == b.maxZ && a.minY <= b.maxY && b.minY <= a.maxY;
            default -> a.minX == b.minX && a.maxX == b.maxX && a.minY == b.minY && a.maxY == b.maxY && a.minZ <= b.maxZ && b.minZ <= a.maxZ;
        };
        return aligned ? new AABB(Math.min(a.minX, b.minX), Math.min(a.minY, b.minY), Math.min(a.minZ, b.minZ),
                Math.max(a.maxX, b.maxX), Math.max(a.maxY, b.maxY), Math.max(a.maxZ, b.maxZ)) : null;
    }

    private static boolean finiteVolume(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
                && box.minX < box.maxX && box.minY < box.maxY && box.minZ < box.maxZ;
    }

    private static boolean contains(AABB a, AABB b) {
        return a.minX <= b.minX && a.minY <= b.minY && a.minZ <= b.minZ
                && a.maxX >= b.maxX && a.maxY >= b.maxY && a.maxZ >= b.maxZ;
    }

    /**
     * Compacts a build-local buffer in place. The source hitboxes and their configs are never mutated.
     */
    public void optimize(ObjectArrayList<ColliderConfig> input, int[] configuredIndices) {
        int size = input.size();
        if (size == 0) return;
        if (size == 1) {
            ColliderConfig config = input.getFirst();
            if (config == null) input.clear();
            return;
        }
        Object[] configs = input.elements();
        if (this.hasMerges) {
            int[] parents = new int[size];
            for (int i = 0; i < size; i++) {
                ColliderConfig config = (ColliderConfig) configs[i];
                // Invalid boxes remain in the output but cannot participate in merges.
                parents[i] = config != null && finiteVolume(config.bounds) ? i : -1;
            }
            for (int i = 0; i < this.merges.size(); i++) {
                Merge merge = this.merges.get(i);
                int first = configuredIndices[merge.first], second = configuredIndices[merge.second];
                if (first < 0 || second < 0) continue;
                if (parents[first] < 0 || parents[second] < 0) continue;
                first = root(parents, first);
                second = root(parents, second);
                if (first == second) continue;
                ColliderConfig a = (ColliderConfig) configs[first], b = (ColliderConfig) configs[second];
                if (a.properties != b.properties) continue;
                AABB union = unionValidated(a.bounds, b.bounds);
                if (union == null) continue;
                configs[first] = new ColliderConfig(union, a.properties);
                configs[second] = null;
                parents[second] = first;
            }
        }
        int kept = 0;
        for (int i = 0; i < size; i++) {
            ColliderConfig config = (ColliderConfig) configs[i];
            if (config != null) configs[kept++] = config;
        }
        input.size(kept);
    }

    private record Merge(int first, int second) {
    }
}
