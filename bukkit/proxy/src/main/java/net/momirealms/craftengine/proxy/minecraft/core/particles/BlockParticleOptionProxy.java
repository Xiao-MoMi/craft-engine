package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.particles.BlockParticleOption")
public interface BlockParticleOptionProxy {
    BlockParticleOptionProxy INSTANCE = ASMProxyFactory.create(BlockParticleOptionProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.particles.BlockParticleOption");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ParticleTypeProxy.class) Object type, @Type(clazz = BlockStateProxy.class) Object blockState);

    @FieldGetter(name = "type")
    Object getType(Object target);

    @FieldGetter(name = "state")
    Object getState(Object target);
}
