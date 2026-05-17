package net.momirealms.craftengine.core.world.chunk.client.light;

public interface ClientSectionLightStorage {

    // 0 -> Solid
    // 1 -> Air
    // 2 -> Water
    int blockType(int index);

    boolean isSolid(int index);

    boolean isWater(int index);

    boolean isAir(int index);

}
