package net.momirealms.craftengine.proxy.minecraft.world.item.equipment.trim;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = {"net.minecraft.world.item.equipment.trim.ArmorTrim", "net.minecraft.world.item.armortrim.ArmorTrim"})
public interface ArmorTrimProxy {
    ArmorTrimProxy INSTANCE = ASMProxyFactory.create(ArmorTrimProxy.class);
    Codec<Object> CODEC = INSTANCE.getCodec();

    @FieldGetter(name = "CODEC", isStatic = true)
    Codec<Object> getCodec();

    @MethodInvoker(name = "setTrim", isStatic = true)
    boolean setTrim(@Type(clazz = RegistryAccessProxy.class) Object registryAccess,
                    @Type(clazz = ItemStackProxy.class) Object itemStack,
                    @Type(clazz = ArmorTrimProxy.class) Object armorTrim);

    @MethodInvoker(name = "getTrim", isStatic = true, activeIf = "min_version=1.20.2 && max_version=1.20.4")
    Optional<Object> getTrim(@Type(clazz = RegistryAccessProxy.class) Object registryAccess,
                             @Type(clazz = ItemStackProxy.class) Object itemStack,
                             boolean suppressError);

    @MethodInvoker(name = "getTrim", isStatic = true, activeIf = "max_version=1.20.1")
    Optional<Object> getTrim(@Type(clazz = RegistryAccessProxy.class) Object registryAccess,
                             @Type(clazz = ItemStackProxy.class) Object itemStack);
}
