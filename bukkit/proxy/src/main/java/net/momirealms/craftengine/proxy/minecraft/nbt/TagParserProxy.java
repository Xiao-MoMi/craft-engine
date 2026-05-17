package net.momirealms.craftengine.proxy.minecraft.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.nbt.TagParser")
public interface TagParserProxy {
    TagParserProxy INSTANCE = ASMProxyFactory.create(TagParserProxy.class);

    @MethodInvoker(name = {"parseCompoundFully", "parseTag"}, isStatic = true)
    Object parseCompoundFully(String snbt) throws CommandSyntaxException;
}
