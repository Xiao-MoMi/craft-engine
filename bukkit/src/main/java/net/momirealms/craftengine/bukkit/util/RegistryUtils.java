package net.momirealms.craftengine.bukkit.util;

public class RegistryUtils {

    private RegistryUtils() {}

    public static int currentBlockRegistrySize() {
        try {
            return (int) Reflections.method$IdMapper$size.invoke(Reflections.instance$BLOCK_STATE_REGISTRY);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int currentBiomeRegistrySize() {
        try {
            Object idMap = Reflections.method$Registry$asHolderIdMap.invoke(Reflections.instance$BuiltInRegistries$BIOME);
            return (int) Reflections.method$IdMap$size.invoke(idMap);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
