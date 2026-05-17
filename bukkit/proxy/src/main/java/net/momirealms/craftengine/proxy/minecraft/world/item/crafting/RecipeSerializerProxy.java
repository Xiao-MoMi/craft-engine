package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamCodecProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeSerializer")
public interface RecipeSerializerProxy {
    RecipeSerializerProxy INSTANCE = ASMProxyFactory.create(RecipeSerializerProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.RecipeSerializer");

    @ConstructorInvoker(activeIf = "min_version=26.1")
    Object newInstance(MapCodec<Object> codec, @Type(clazz = StreamCodecProxy.class) Object streamCodec);
}
