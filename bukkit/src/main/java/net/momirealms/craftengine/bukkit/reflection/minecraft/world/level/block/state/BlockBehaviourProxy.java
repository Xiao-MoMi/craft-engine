package net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.block.state;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour")
public interface BlockBehaviourProxy {
    BlockBehaviourProxy INSTANCE = ReflectionHelper.getProxy(BlockBehaviourProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BlockBehaviourProxy.class);

    @ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
    interface BlockStateBaseProxy {
        BlockStateBaseProxy INSTANCE = ReflectionHelper.getProxy(BlockStateBaseProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(BlockStateBaseProxy.class);

        @FieldGetter(name = "cache")
        Object cache(Object instance);

        @FieldSetter(name = "cache")
        void cache(Object instance, Object cache);

        @FieldGetter(name = "propagatesSkylightDown", version = ">=1.21.2")
        boolean propagatesSkylightDown(Object instance);

        @FieldSetter(name = "propagatesSkylightDown", version = ">=1.21.2")
        void propagatesSkylightDown(Object instance, boolean propagatesSkylightDown);

        @ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
        interface CacheProxy {
            CacheProxy INSTANCE = ReflectionHelper.getProxy(CacheProxy.class);
            Class<?> CLAZZ = ReflectionHelper.getClass(CacheProxy.class);

            @FieldGetter(name = "lightBlock", version = "<1.21.2")
            int lightBlock(Object instance);

            @FieldSetter(name = "lightBlock", strategy = Strategy.MH, version = "<1.21.2")
            void lightBlock(Object instance, int lightBlock);

            @FieldGetter(name = "propagatesSkylightDown", version = "<1.21.2")
            boolean propagatesSkylightDown(Object instance);

            @FieldSetter(name = "propagatesSkylightDown", strategy = Strategy.MH, version = "<1.21.2")
            void propagatesSkylightDown(Object instance, boolean propagatesSkylightDown);
        }
    }
}
