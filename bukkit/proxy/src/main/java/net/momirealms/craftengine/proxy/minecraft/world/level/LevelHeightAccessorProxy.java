package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.LevelHeightAccessor")
public interface LevelHeightAccessorProxy {
    LevelHeightAccessorProxy INSTANCE = ASMProxyFactory.create(LevelHeightAccessorProxy.class);

    @MethodInvoker(name = "isOutsideBuildHeight")
    boolean isOutsideBuildHeight(Object target, @Type(clazz = BlockPosProxy.class) Object pos);

    @MethodInvoker(name = "isOutsideBuildHeight")
    boolean isOutsideBuildHeight(Object target, int posY);

    @MethodInvoker(name = {"getMinY", "getMinBuildHeight"})
    int getMinY(Object target);

    @MethodInvoker(name = {"getMaxY", "getMaxBuildHeight"})
    int getMaxY(Object target);

    @MethodInvoker(name = "getHeight")
    int getHeight(Object target);

    @MethodInvoker(name = "getMinSection", activeIf = "max_version=1.21.1")
    int getMinSection(Object target);

    @MethodInvoker(name = "getMinSectionY", activeIf = "min_version=1.21.2")
    int getMinSectionY(Object target);
}
