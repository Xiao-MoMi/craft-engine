package net.momirealms.craftengine.bukkit.reflection.leaves.bot;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "org.leavesmc.leaves.bot.BotList")
public interface BotListProxy {
    BotListProxy INSTANCE = MiscUtils.ifGet(() -> ReflectionHelper.getProxy(BotListProxy.class), VersionHelper.isLeaves());

    @FieldGetter(name = "INSTANCE", isStatic = true)
    Object INSTANCE();

    @FieldSetter(name = "INSTANCE", isStatic = true, strategy = Strategy.MH)
    void INSTANCE(Object instance);

    @FieldGetter(name = "bots")
    List<?> bots(Object instance);

    @FieldSetter(name = "bots", strategy = Strategy.MH)
    void bots(Object instance, List<?> bots);
}
