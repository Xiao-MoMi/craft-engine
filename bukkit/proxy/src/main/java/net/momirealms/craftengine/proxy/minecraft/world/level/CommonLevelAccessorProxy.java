package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.CommonLevelAccessor")
public interface CommonLevelAccessorProxy extends LevelReaderProxy, LevelWriterProxy, EntityGetterProxy {
}
