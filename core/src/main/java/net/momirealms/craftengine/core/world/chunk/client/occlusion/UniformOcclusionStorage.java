package net.momirealms.craftengine.core.world.chunk.client.occlusion;

public enum UniformOcclusionStorage implements ClientSectionOcclusionStorage {
    PERMEATION(false),
    OCCLUDING(true);

    public final boolean isOccluding;

    UniformOcclusionStorage(boolean isOccluding) {
        this.isOccluding = isOccluding;
    }

    public static UniformOcclusionStorage fromTest(boolean isOccluding) {
        return isOccluding ? OCCLUDING : PERMEATION;
    }

    @Override
    public boolean isOccluding(int index) {
        return this.isOccluding;
    }
}
