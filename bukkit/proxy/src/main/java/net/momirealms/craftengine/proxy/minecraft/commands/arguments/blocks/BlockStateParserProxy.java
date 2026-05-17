package net.momirealms.craftengine.proxy.minecraft.commands.arguments.blocks;

import net.momirealms.craftengine.proxy.minecraft.core.HolderLookupProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.commands.arguments.blocks.BlockStateParser")
public interface BlockStateParserProxy {
    BlockStateParserProxy INSTANCE = ASMProxyFactory.create(BlockStateParserProxy.class);

    @MethodInvoker(name = "parseForBlock", isStatic = true)
    Object parseForBlock(@Type(clazz = HolderLookupProxy.class) Object registry,
                         String blockState,
                         boolean allowSnbt);

    @ReflectionProxy(name = "net.minecraft.commands.arguments.blocks.BlockStateParser$BlockResult")
    interface BlockResultProxy {
        BlockResultProxy INSTANCE = ASMProxyFactory.create(BlockResultProxy.class);

        @FieldGetter(name = "blockState")
        Object getBlockState(Object target);
    }
}
