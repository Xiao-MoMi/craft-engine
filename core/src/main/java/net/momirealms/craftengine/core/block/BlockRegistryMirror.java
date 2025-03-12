package net.momirealms.craftengine.core.block;

public class BlockRegistryMirror {
    private static PackedBlockState[] customBlockStates;

    public static void init(PackedBlockState[] states) {
        customBlockStates = states;
    }

    public static PackedBlockState stateByRegistryId(int vanillaId) {
        if (vanillaId < 0) return null;
        return customBlockStates[vanillaId];
    }

    public static int size() {
        return customBlockStates.length;
    }
}
