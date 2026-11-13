package net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.FixedFormat;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.FixedFormat")
public interface FixedNumberProxy {
    FixedNumberProxy INSTANCE = ASMProxyFactory.create(FixedNumberProxy.class);

    @MethodInvoker(name = "getValue")
    Object value(Object packet);
    @ConstructorInvoker
    FixedFormat create(@Type(clazz = AdventureComponentProxy.class) Object value);
}

