package net.momirealms.craftengine.proxy.minecraft.world.entity.player;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.player.Abilities")
public interface AbilitiesProxy {
    AbilitiesProxy INSTANCE = ASMProxyFactory.create(AbilitiesProxy.class);

    @FieldGetter(name = "instabuild")
    boolean isInstantBuild(Object target);

    @FieldSetter(name = "instabuild")
    void setInstantBuild(Object target, boolean instantBuild);

    @FieldGetter(name = "mayBuild")
    boolean isMayBuild(Object target);

    @FieldSetter(name = "mayBuild")
    void setMayBuild(Object target, boolean mayBuild);
}
