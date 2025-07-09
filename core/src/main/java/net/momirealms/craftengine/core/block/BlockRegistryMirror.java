package net.momirealms.craftengine.core.block;

public final class BlockRegistryMirror {
    private static BlockStateWrapper[] blockStates;
    private static BlockStateWrapper stoneState;

    public static void init(BlockStateWrapper[] states, BlockStateWrapper state) {
        if (blockStates != null) throw new IllegalStateException("block states are already set");
        blockStates = states;
        stoneState = state;
    }

    public static BlockStateWrapper stateByRegistryId(int vanillaId) {
        if (vanillaId < 0) return stoneState;
        return blockStates[vanillaId];
    }

    public static int size() {
        return blockStates.length;
    }

    public static BlockStateWrapper stoneState() {
        return stoneState;
    }
}
