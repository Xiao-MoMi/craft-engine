package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundAddEntityPacket")
public interface ClientboundAddEntityPacketProxy extends PacketProxy {
    ClientboundAddEntityPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundAddEntityPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");

    @ConstructorInvoker
    Object newInstance(
            int id, UUID uuid,
            double x, double y, double z,
            float xRot, float yRot,
            @Type(clazz = EntityTypeProxy.class) Object type,
            int data,
            @Type(clazz = Vec3Proxy.class) Object deltaMovement,
            double yHeadRot
    );
}
