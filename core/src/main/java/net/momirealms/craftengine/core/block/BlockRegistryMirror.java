package net.momirealms.craftengine.core.block;

public class BlockRegistryMirror {
    private static PackedBlockState[] customBlockStates;
    private static PackedBlockState stoneState;

    public static void init(PackedBlockState[] states, PackedBlockState state) {
        customBlockStates = states;
        stoneState = state;
    }

    public static PackedBlockState stateByRegistryId(int vanillaId) {
        if (vanillaId < 0) return stoneState;
        return customBlockStates[vanillaId];
    }

    public static int size() {
        return customBlockStates.length;
    }
}
