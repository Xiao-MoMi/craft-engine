package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.particles.ParticleOptionsProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.WorldGenLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ReflectionProxy(name = "net.minecraft.server.level.ServerLevel")
public interface ServerLevelProxy extends LevelProxy, WorldGenLevelProxy {
    ServerLevelProxy INSTANCE = ASMProxyFactory.create(ServerLevelProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.server.level.ServerLevel");

    @MethodInvoker(name = "sendBlockUpdated")
    void sendBlockUpdated(Object target,
                          @Type(clazz = BlockPosProxy.class) Object pos,
                          @Type(clazz = BlockStateProxy.class) Object oldState,
                          @Type(clazz = BlockStateProxy.class) Object newState,
                          int flags);

    @MethodInvoker(name = "getEntityLookup", activeIf = "max_version=1.20.6")
    Object getEntityLookup(Object target);

    @MethodInvoker(name = "getChunkSource")
    Object getChunkSource(Object target);

    @MethodInvoker(name = "sendParticles")
    int sendParticles(Object target,
                      @Type(clazz = ParticleOptionsProxy.class) Object options,
                      double x,
                      double y,
                      double z,
                      int count,
                      double dx,
                      double dy,
                      double dz,
                      double speed);

    @MethodInvoker(name = "players")
    List<?> players(Object target);

    @MethodInvoker(name = "sendParticles")
    boolean sendParticlesToPlayer(Object target,
                                  @Type(clazz = ServerPlayerProxy.class) Object player,
                                  boolean overrideLimiter,
                                  double x,
                                  double y,
                                  double z,
                                  @Type(clazz = PacketProxy.class) Object packet);

    @MethodInvoker(name = "getServer")
    Object getServer(Object target);

    @FieldGetter(name = "ENTITY_COUNTER", isStatic = true, activeIf = "min_version=26.2")
    AtomicInteger getEntityCounter();

    @FieldGetter(name = "structureManager")
    Object getStructureManager(Object target);
}
