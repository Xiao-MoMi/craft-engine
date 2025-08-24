package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;

import java.lang.reflect.Modifier;

public final class ProtectedFieldVisitor {
    private static FieldAccessor internalFieldAccessor;

    public static void init() throws ReflectiveOperationException {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        // InternalFieldAccessor Interface
        Class<?> internalFieldAccessorInterface = byteBuddy
                .makeInterface()
                .name("net.momirealms.craftengine.bukkit.plugin.injector.FieldAccessor")
                .defineMethod("field$ClientboundMoveEntityPacket$entityId", int.class, Modifier.PUBLIC)
                .withParameter(Object.class, "packet")
                .withoutCode()
                .make()
                .load(NetworkReflections.clazz$ClientboundMoveEntityPacket.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();

        // Internal field accessor
        FieldDescription moveEntityIdFieldDesc = new FieldDescription.ForLoadedField(NetworkReflections.field$ClientboundMoveEntityPacket$entityId);
        Class<?> clazz$InternalFieldAccessor = byteBuddy
                .subclass(Object.class)
                .name("net.minecraft.network.protocol.game.CraftEngineInternalFieldAccessor")
                .implement(internalFieldAccessorInterface)
                .method(ElementMatchers.named("field$ClientboundMoveEntityPacket$entityId"))
                .intercept(new Implementation.Simple(
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        TypeCasting.to(TypeDescription.ForLoadedType.of(NetworkReflections.clazz$ClientboundMoveEntityPacket)),
                        FieldAccess.forField(moveEntityIdFieldDesc).read(),
                        MethodReturn.INTEGER
                ))
                .make()
                .load(NetworkReflections.clazz$ClientboundMoveEntityPacket.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        internalFieldAccessor = (FieldAccessor) clazz$InternalFieldAccessor.getConstructor().newInstance();
    }

    public static FieldAccessor get() {
        return internalFieldAccessor;
    }
}
