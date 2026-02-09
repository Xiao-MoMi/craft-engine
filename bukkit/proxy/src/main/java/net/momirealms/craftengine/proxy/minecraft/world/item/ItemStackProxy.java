package net.momirealms.craftengine.proxy.minecraft.world.item;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.function.Consumer;

@ReflectionProxy(name = "net.minecraft.world.item.ItemStack")
public interface ItemStackProxy {
    ItemStackProxy INSTANCE = ASMProxyFactory.create(ItemStackProxy.class);
    Object EMPTY = INSTANCE.getEmpty();

    @FieldGetter(name = "EMPTY", isStatic = true)
    Object getEmpty();

    @FieldGetter(name = "CODEC", isStatic = true, activeIf = "min_version=1.20.5")
    Codec<Object> getCodec();

    @MethodInvoker(name = "hurtAndBreak", activeIf = "min_version=1.20.5")
    void hurtAndBreak(Object target, int amount, @Type(clazz = LivingEntityProxy.class) Object entity, @Type(clazz = EquipmentSlotProxy.class) Object slot);

    @MethodInvoker(name = "hurtAndBreak", activeIf = "max_version=1.20.4")
    Object hurtAndBreak(Object target, int amount, @Type(clazz = LivingEntityProxy.class) Object entity, Consumer<Object> breakCallback);

    @MethodInvoker(name = "getComponents", activeIf = "min_version=1.20.5")
    Object getComponents(Object target);

    @MethodInvoker(name = "of", isStatic = true, activeIf = "max_version=1.20.4")
    Object of(@Type(clazz = CompoundTagProxy.class) Object nbt);

    @MethodInvoker(name = "is")
    boolean is(Object target, @Type(clazz = TagKeyProxy.class) Object tag);

    @MethodInvoker(name = "getTag", activeIf = "max_version=1.20.4")
    Object getTag(Object target);

    @MethodInvoker(name = "save", activeIf = "max_version=1.20.4")
    Object save(Object target, @Type(clazz = CompoundTagProxy.class) Object nbt);
}
