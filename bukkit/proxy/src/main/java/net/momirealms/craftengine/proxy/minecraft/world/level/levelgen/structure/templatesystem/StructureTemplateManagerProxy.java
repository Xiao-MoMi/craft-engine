package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.structure.templatesystem;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager")
public interface StructureTemplateManagerProxy {
    StructureTemplateManagerProxy INSTANCE = ASMProxyFactory.create(StructureTemplateManagerProxy.class);
}
