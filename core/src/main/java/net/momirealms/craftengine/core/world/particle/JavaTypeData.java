package net.momirealms.craftengine.core.world.particle;

public final class JavaTypeData implements ParticleData {
    private final Object data;

    public JavaTypeData(Object data) {
        this.data = data;
    }

    public Object data() {
        return data;
    }
}
