package net.momirealms.craftengine.bedrock.geyser.proxy.registry.type;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.geysermc.geyser.api.block.custom.CustomBlockState;

import java.util.Map;

@ReflectionProxy(name = "org.geysermc.geyser.registry.type.BlockMappings")
public interface BlockMappingsProxy {
    BlockMappingsProxy INSTANCE = ASMProxyFactory.create(BlockMappingsProxy.class);

    @MethodInvoker(name = "getCustomBlockStateDefinitions")
    Map<CustomBlockState, BlockDefinition> getCustomBlockStateDefinitions(Object target);
}
