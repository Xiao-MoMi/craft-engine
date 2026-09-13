package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.LevelChunkSection")
public interface LevelChunkSectionProxy {
    LevelChunkSectionProxy INSTANCE = ASMProxyFactory.create(LevelChunkSectionProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.chunk.LevelChunkSection");

    @MethodInvoker(name = "setBlockState")
    Object setBlockState(Object target, int x, int y, int z, @Type(clazz = BlockStateProxy.class) Object blockState);

    @MethodInvoker(name = "setBlockState")
    Object setBlockState(Object target, int x, int y, int z, @Type(clazz = BlockStateProxy.class) Object blockState, boolean useLocks);

    @FieldGetter(name = "states")
    Object getStates(Object target);

    @FieldSetter(name = "states")
    void setStates(Object target, Object states);

    @MethodInvoker(name = "hasOnlyAir")
    boolean hasOnlyAir(Object target);

    @FieldGetter(name = "biomes")
    Object getBiomes(Object target);

    @FieldSetter(name = "biomes")
    void setBiomes(Object target, Object biomes);

    @MethodInvoker(name = "getBlockState")
    Object getBlockState(Object target, int x, int y, int z);

    @FieldGetter(name = "nonEmptyBlockCount")
    short getNonEmptyBlockCount(Object target);

    @FieldSetter(name = "nonEmptyBlockCount")
    void setNonEmptyBlockCount(Object target, short nonEmptyBlockCount);

    @FieldGetter(name = "tickingBlockCount")
    short getTickingBlockCount(Object target);

    @FieldSetter(name = "tickingBlockCount")
    void setTickingBlockCount(Object target, short tickingBlockCount);

    @FieldGetter(name = "tickingFluidCount")
    short getTickingFluidCount(Object target);

    @FieldSetter(name = "tickingFluidCount")
    void setTickingFluidCount(Object target, short tickingFluidCount);

    @FieldGetter(name = "specialCollidingBlocks", activeIf = "min_version=1.21.2 && has_patch=paper")
    default short getSpecialCollidingBlocks(Object target) {
        return 0;
    }

    @FieldSetter(name = "specialCollidingBlocks", activeIf = "min_version=1.21.2 && has_patch=paper")
    default void setSpecialCollidingBlocks(Object target, short specialCollidingBlocks) {
    }

    @FieldGetter(name = "specialCollidingBlocks", activeIf = "max_version=1.21.1")
    int getSpecialCollidingBlocks$legacy(Object target);

    @FieldSetter(name = "specialCollidingBlocks", activeIf = "max_version=1.21.1")
    void setSpecialCollidingBlocks$legacy(Object target, int specialCollidingBlocks);

    @FieldGetter(name = {"tickingBlocks", "tickingList"}, optional = true)
    default Object getTickingBlocks(Object target) {
        return null;
    }

    @FieldSetter(name = {"tickingBlocks", "tickingList"}, optional = true)
    default void setTickingBlocks(Object target, Object tickingBlocks) {
    }

    @FieldGetter(name = "knownBlockCollisionData", activeIf = "max_version=1.20.1")
    long[] getKnownBlockCollisionData(Object target);

    @FieldSetter(name = "knownBlockCollisionData", activeIf = "max_version=1.20.1")
    void setKnownBlockCollisionData(Object target, long[] knownBlockCollisionData);

    @FieldGetter(name = "fluidCount", activeIf = "min_version=26.1 || (has_patch=leaf && min_version=1.21.11)", optional = true)
    default short getFluidCount(Object target) {
        return 0;
    }

    @FieldSetter(name = "fluidCount", activeIf = "min_version=26.1 || (has_patch=leaf && min_version=1.21.11)", optional = true)
    default void setFluidCount(Object target, short fluidCount) {
    }

    @FieldGetter(name = "isRandomlyTickingBlocksStatus", activeIf = "has_patch=universespigot", optional = true)
    default boolean isRandomlyTickingBlocksStatus(Object target) {
        return false;
    }

    @FieldSetter(name = "isRandomlyTickingBlocksStatus", activeIf = "has_patch=universespigot", optional = true)
    default void setIsRandomlyTickingBlocksStatus(Object target, boolean status) {
    }

    @FieldSetter(name = "hasOnlyAir", activeIf = "has_patch=universespigot", optional = true)
    default void setHasOnlyAir(Object target, boolean hasOnlyAir) {
    }
}
