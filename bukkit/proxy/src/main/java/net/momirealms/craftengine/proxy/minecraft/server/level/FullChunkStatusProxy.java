package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.FullChunkStatus")
public interface FullChunkStatusProxy {
    FullChunkStatusProxy INSTANCE = ASMProxyFactory.create(FullChunkStatusProxy.class);
    Object BLOCK_TICKING = INSTANCE.getBlockTicking();
    Object ENTITY_TICKING = INSTANCE.getEntityTicking();

    @FieldGetter(name = "BLOCK_TICKING", isStatic = true)
    Object getBlockTicking();

    @FieldGetter(name = "ENTITY_TICKING", isStatic = true)
    Object getEntityTicking();
}
