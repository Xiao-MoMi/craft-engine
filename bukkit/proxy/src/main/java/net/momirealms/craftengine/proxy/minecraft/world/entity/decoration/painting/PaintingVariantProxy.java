package net.momirealms.craftengine.proxy.minecraft.world.entity.decoration.painting;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ReflectionProxy(name = {"net.minecraft.world.entity.decoration.painting.PaintingVariant", "net.minecraft.world.entity.decoration.PaintingVariant"})
public interface PaintingVariantProxy {
    PaintingVariantProxy INSTANCE = ASMProxyFactory.create(PaintingVariantProxy.class);

    @ConstructorInvoker(activeIf = "max_version=1.20.6")
    Object newInstance(int width, int height);

    @ConstructorInvoker(activeIf = "min_version=1.21 && max_version=1.21.1")
    Object newInstance(int width, int height, @Type(clazz = IdentifierProxy.class) Object assetId);

    @ConstructorInvoker(activeIf = "min_version=1.21.2")
    Object newInstance(int width, int height, @Type(clazz = IdentifierProxy.class) Object assetId, Optional<?> title, Optional<?> author);

    @FieldSetter(name = "width")
    void setWidth(Object target, int width);

    @FieldSetter(name = "height")
    void setHeight(Object target, int height);

    @FieldSetter(name = "assetId", activeIf = "min_version=1.21")
    default void setAssetId(Object target, @Type(clazz = IdentifierProxy.class) Object assetId) {
    }

    @FieldSetter(name = "title", activeIf = "min_version=1.21.2")
    default void setTitle(Object target, Optional<?> title) {
    }

    @FieldSetter(name = "author", activeIf = "min_version=1.21.2")
    default void setAuthor(Object target, Optional<?> author) {
    }
}
