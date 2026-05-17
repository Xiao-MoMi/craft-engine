package net.momirealms.craftengine.proxy.leaves.bot;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "org.leavesmc.leaves.bot.BotList", activeIf = "has_patch=leaves")
public interface BotListProxy {
    BotListProxy INSTANCE = ASMProxyFactory.create(BotListProxy.class);

    @FieldGetter(name = "INSTANCE", isStatic = true)
    Object getInstance();

    @FieldGetter(name = "bots")
    List<Object> getBots(Object target);

    @FieldSetter(name = "bots")
    void setBots(Object target, List<Object> bots);
}
