package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@ReflectionProxy(name = "net.minecraft.server.level.ServerPlayer")
public interface ServerPlayerProxy extends PlayerProxy {
    ServerPlayerProxy INSTANCE = ASMProxyFactory.create(ServerPlayerProxy.class);

    @FieldGetter(name = "chunkLoader")
    Object getChunkLoader(Object target);

    @FieldGetter(name = "connection")
    Object getConnection(Object target);

    @MethodInvoker(name = "initMenu")
    void initMenu(Object target, @Type(clazz = AbstractContainerMenuProxy.class) Object menu);

    @MethodInvoker(name = "nextContainerCounter")
    int nextContainerCounter(Object target);

    @MethodInvoker(name = "drop", activeIf = "min_version=1.21.4")
    Object drop(Object target, @Type(clazz = ItemStackProxy.class) Object droppedItem, boolean dropAround, boolean traceItem, boolean callEvent, @Nullable Consumer<Item> entityOperation);

    @MethodInvoker(name = "drop", activeIf = "min_version=1.20.3 && max_version=1.21.3")
    Object drop$1(Object target, @Type(clazz = ItemStackProxy.class) Object droppedItem, boolean dropAround, boolean traceItem, boolean callEvent);

    @MethodInvoker(name = "getBukkitEntity")
    Player getBukkitEntity(Object target);

    @FieldGetter(name = "gameMode")
    Object getGameMode(Object target);

    @FieldSetter(name = "gameMode")
    void setGameMode(Object target, Object gameMode);
}
