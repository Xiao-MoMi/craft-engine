package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.world.item.ItemCooldowns")
public interface ItemCooldownsProxy {
    ItemCooldownsProxy INSTANCE = ASMProxyFactory.create(ItemCooldownsProxy.class);

    @FieldGetter(name = "cooldowns")
    Map<Object, Object> getCooldowns(Object target);

    @FieldGetter(name = "tickCount")
    int getTickCount(Object target);

    @MethodInvoker(name = "addCooldown", activeIf = "min_version=1.21.2")
    void addCooldown(Object target, @Type(clazz = IdentifierProxy.class) Object cooldownGroup, int time);

    @ReflectionProxy(name = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
    interface CooldownInstanceProxy {
        CooldownInstanceProxy INSTANCE = ASMProxyFactory.create(CooldownInstanceProxy.class);

        @FieldGetter(name = "endTime")
        int getEndTime(Object target);
    }
}
