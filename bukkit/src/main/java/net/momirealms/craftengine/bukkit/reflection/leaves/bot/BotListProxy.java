package net.momirealms.craftengine.bukkit.reflection.leaves.bot;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "org.leavesmc.leaves.bot.BotList", version = "server:leaves")
public interface BotListProxy {
    BotListProxy INSTANCE = ReflectionHelper.getProxy(BotListProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BotListProxy.class);

    @FieldGetter(name = "INSTANCE", isStatic = true)
    Object INSTANCE();

    @FieldSetter(name = "INSTANCE", isStatic = true, strategy = Strategy.MH)
    void INSTANCE(Object instance);

    @FieldGetter(name = "bots")
    List<Object> bots(Object instance);

    @FieldSetter(name = "bots", strategy = Strategy.MH)
    void bots(Object instance, List<?> bots);
}
