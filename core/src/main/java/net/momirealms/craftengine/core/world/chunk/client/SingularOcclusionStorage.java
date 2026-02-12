package net.momirealms.craftengine.core.world.chunk.client;

public final class SingularOcclusionStorage implements ClientSectionOcclusionStorage {
    private final boolean isOccluding;

    public SingularOcclusionStorage(boolean isOccluding) {
        this.isOccluding = isOccluding;
    }

    @Override
    public boolean isOccluding(int index) {
        return this.isOccluding;
    }
}
