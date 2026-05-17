package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.momirealms.craftengine.proxy.minecraft.core.SectionPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket")
public interface ClientboundSectionBlocksUpdatePacketProxy {
    ClientboundSectionBlocksUpdatePacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSectionBlocksUpdatePacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = SectionPosProxy.class) Object sectionPos, ShortSet shortSet, @Type(clazz = BlockStateProxy[].class) Object states);

}
