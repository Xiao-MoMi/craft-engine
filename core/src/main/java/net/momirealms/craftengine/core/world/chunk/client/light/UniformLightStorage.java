package net.momirealms.craftengine.core.world.chunk.client.light;

public enum UniformLightStorage implements ClientSectionLightStorage {
    SOLID(0),
    AIR(1),
    WATER(2);

    public static final UniformLightStorage[] VALUES = new UniformLightStorage[]{SOLID, AIR, WATER};
    private final int type;

    UniformLightStorage(int type) {
        this.type = type;
    }

    public static UniformLightStorage fromLightPredicate(int testResult) {
        return VALUES[testResult];
    }

    @Override
    public int blockType(int index) {
        return this.type;
    }

    @Override
    public boolean isSolid(int index) {
        return this == SOLID;
    }

    @Override
    public boolean isWater(int index) {
        return this == WATER;
    }

    @Override
    public boolean isAir(int index) {
        return this == AIR;
    }
}
