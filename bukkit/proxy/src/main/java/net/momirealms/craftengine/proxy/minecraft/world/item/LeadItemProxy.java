package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.LeadItem")
public interface LeadItemProxy {
    LeadItemProxy INSTANCE = ASMProxyFactory.create(LeadItemProxy.class);

    @MethodInvoker(name = "bindPlayerMobs", isStatic = true)
    Object bindPlayerMobs(@Type(clazz = PlayerProxy.class) Object player, @Type(clazz = LevelProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object pos);
}
