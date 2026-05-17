package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.placement.PlacementModifier")
public interface PlacementModifierProxy {
    PlacementModifierProxy INSTANCE = ASMProxyFactory.create(PlacementModifierProxy.class);
    Codec<Object> CODEC = INSTANCE.getCodec();

    @FieldGetter(name = "CODEC", isStatic = true)
    Codec<Object> getCodec();
}
