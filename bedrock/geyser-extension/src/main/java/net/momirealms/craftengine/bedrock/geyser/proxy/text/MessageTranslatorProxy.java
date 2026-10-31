package net.momirealms.craftengine.bedrock.geyser.proxy.text;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "org.geysermc.geyser.translator.text.MessageTranslator")
public interface MessageTranslatorProxy {
    MessageTranslatorProxy INSTANCE = ASMProxyFactory.create(MessageTranslatorProxy.class);

    @FieldGetter(name = "BEDROCK_SERIALIZER", isStatic = true)
    Object bedrockSerializer();

    @FieldSetter(name = "BEDROCK_SERIALIZER", isStatic = true)
    void bedrockSerializer(Object serializer);

    @MethodInvoker(name = "convertMessage", isStatic = true)
    String convertMessage(@Type(clazz = AdventureComponentProxy.class) Object component, String locale);

    @FieldGetter(name = "RENDERER", isStatic = true)
    Object renderer();
}
