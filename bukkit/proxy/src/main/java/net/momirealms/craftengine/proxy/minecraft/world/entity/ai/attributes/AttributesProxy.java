package net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.ai.attributes.Attributes")
public interface AttributesProxy {
    AttributesProxy INSTANCE = ASMProxyFactory.create(AttributesProxy.class);
    Object BLOCK_BREAK_SPEED = INSTANCE.getBlockBreakSpeed();
    Object BLOCK_INTERACTION_RANGE = INSTANCE.getBlockInteractionRange();
    Object SCALE = INSTANCE.getScale();

    @FieldGetter(name = "BLOCK_BREAK_SPEED", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getBlockBreakSpeed() {
        return null;
    }

    @FieldGetter(name = "BLOCK_INTERACTION_RANGE", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getBlockInteractionRange() {
        return null;
    }

    @FieldGetter(name = "SCALE", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getScale() {
        return null;
    }
}
