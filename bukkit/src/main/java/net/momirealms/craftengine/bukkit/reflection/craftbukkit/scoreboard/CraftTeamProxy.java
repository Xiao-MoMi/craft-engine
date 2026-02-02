package net.momirealms.craftengine.bukkit.reflection.craftbukkit.scoreboard;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.scoreboard.Team;

@ReflectionProxy(name = "org.bukkit.craftbukkit.scoreboard.CraftTeam")
public interface CraftTeamProxy {
    CraftTeamProxy INSTANCE = ReflectionHelper.getProxy(CraftTeamProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftTeamProxy.class);

    @FieldGetter(name = "team")
    Object team(Team instance);

    @FieldSetter(name = "team", strategy = Strategy.MH)
    void team(Team instance, Object team);
}
