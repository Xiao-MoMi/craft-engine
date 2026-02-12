package net.momirealms.craftengine.proxy.bukkit.craftbukkit;

import net.momirealms.craftengine.proxy.minecraft.core.particles.ParticleTypeProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.Particle;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftParticle")
public interface CraftParticleProxy {
    CraftParticleProxy INSTANCE = ASMProxyFactory.create(CraftParticleProxy.class);

    @MethodInvoker(name = {"minecraftToBukkit", "toBukkit"}, isStatic = true)
    Particle minecraftToBukkit(@Type(clazz = ParticleTypeProxy.class) Object minecraft);
}
