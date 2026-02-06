package net.momirealms.craftengine.proxy.minecraft.world.item;

import com.mojang.serialization.Codec;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.ItemStack")
public interface ItemStackProxy {
    ItemStackProxy INSTANCE = ASMProxyFactory.create(ItemStackProxy.class);
    Object EMPTY = INSTANCE.getEmpty();

    @FieldGetter(name = "EMPTY", isStatic = true)
    Object getEmpty();

    @FieldGetter(name = "CODEC", isStatic = true, activeIf = "min_version=1.20.5")
    Codec<Object> getCodec();
}
