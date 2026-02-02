package net.momirealms.craftengine.bukkit.reflection.craftbukkit.event;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.InteractionHandProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.Nullable;


@ReflectionProxy(name = "org.bukkit.craftbukkit.event.CraftEventFactory")
public interface CraftEventFactoryProxy {
    CraftEventFactoryProxy INSTANCE = ReflectionHelper.getProxy(CraftEventFactoryProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftEventFactoryProxy.class);

    @MethodInvoker(name = "callBlockPlaceEvent", isStatic = true, version = "<1.21.5")
    BlockPlaceEvent callBlockPlaceEvent(
            @Type(clazz = ServerLevelProxy.class) Object world,
            @Type(clazz = PlayerProxy.class) Object who,
            @Type(clazz = InteractionHandProxy.class) Object hand,
            BlockState replacedBlockState,
            int clickedX, int clickedY, int clickedZ
    );

    @MethodInvoker(name = "callBlockPlaceEvent", isStatic = true, version = ">=1.21.5")
    BlockPlaceEvent callBlockPlaceEvent(
            @Type(clazz = ServerLevelProxy.class) Object world,
            @Type(clazz = PlayerProxy.class) Object who,
            @Type(clazz = InteractionHandProxy.class) Object hand,
            BlockState replacedBlockState,
            @Type(clazz = BlockPosProxy.class) Object clickedPos
    );

    @MethodInvoker(name = "handleBlockFormEvent", isStatic = true)
    boolean handleBlockFormEvent(
            @Type(clazz = LevelProxy.class) Object world,
            @Type(clazz = BlockPosProxy.class) Object pos,
            @Type(clazz = BlockStateProxy.class) Object block,
            int flag,
            @Type(clazz = EntityProxy.class) @Nullable Object entity
    );

    @MethodInvoker(name = "handleBlockGrowEvent", isStatic = true)
    boolean handleBlockGrowEvent(
            @Type(clazz = LevelProxy.class) Object world,
            @Type(clazz = BlockPosProxy.class) Object pos,
            @Type(clazz = BlockStateProxy.class) Object block,
            int flag
    );

    @MethodInvoker(name = "callInventoryOpenEvent", isStatic = true)
    Object callInventoryOpenEvent(
            @Type(clazz = ServerPlayerProxy.class) Object player,
            @Type(clazz = AbstractContainerMenuProxy.class) Object container
    );
}
