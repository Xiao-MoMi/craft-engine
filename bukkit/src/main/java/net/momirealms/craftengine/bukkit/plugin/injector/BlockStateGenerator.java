package net.momirealms.craftengine.bukkit.plugin.injector;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.block.BlockSettings;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateDefinitionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.BlockStatePropertiesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.inventory.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public final class BlockStateGenerator {
    private static MethodHandle constructor$CraftEngineBlockState;
    public static Object instance$StateDefinition$Factory;

    public static void init() throws ReflectiveOperationException {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        String packageWithName = BlockStateGenerator.class.getName();
        String generatedStateClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineBlockState";
        DynamicType.Builder<?> stateBuilder = byteBuddy
                .subclass(BlockStateProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(generatedStateClassName)
                .defineField("immutableBlockState", ImmutableBlockState.class, Visibility.PUBLIC)
                .implement(DelegatingBlockState.class)
                .method(ElementMatchers.named("blockState"))
                .intercept(FieldAccessor.ofField("immutableBlockState"))
                .method(ElementMatchers.named("setBlockState"))
                .intercept(FieldAccessor.ofField("immutableBlockState"))
                .method(ElementMatchers.is(BlockReflections.method$BlockStateBase$getDrops))
                .intercept(MethodDelegation.to(GetDropsInterceptor.INSTANCE))
                .method(ElementMatchers.is(BlockReflections.method$StateHolder$hasProperty))
                .intercept(MethodDelegation.to(HasPropertyInterceptor.INSTANCE))
                .method(ElementMatchers.is(BlockReflections.method$StateHolder$getValue))
                .intercept(MethodDelegation.to(GetPropertyValueInterceptor.INSTANCE))
                .method(ElementMatchers.is(BlockReflections.method$StateHolder$setValue))
                .intercept(MethodDelegation.to(SetPropertyValueInterceptor.INSTANCE))
                .method(ElementMatchers.is(BlockReflections.method$BlockStateBase$is))
                .intercept(MethodDelegation.to(IsBlockInterceptor.INSTANCE));
        Class<?> clazz$CraftEngineBlock = stateBuilder.make().load(BlockStateGenerator.class.getClassLoader()).getLoaded();
        constructor$CraftEngineBlockState = VersionHelper.isOrAbove1_20_5() ?
                MethodHandles.publicLookup().in(clazz$CraftEngineBlock)
                        .findConstructor(clazz$CraftEngineBlock, MethodType.methodType(void.class, BlockProxy.CLASS, Reference2ObjectArrayMap.class, MapCodec.class))
                        .asType(MethodType.methodType(BlockStateProxy.CLASS, BlockProxy.CLASS, Reference2ObjectArrayMap.class, MapCodec.class)) :
                MethodHandles.publicLookup().in(clazz$CraftEngineBlock)
                        .findConstructor(clazz$CraftEngineBlock, MethodType.methodType(void.class, BlockProxy.CLASS, ImmutableMap.class, MapCodec.class))
                        .asType(MethodType.methodType(BlockStateProxy.CLASS, BlockProxy.CLASS, ImmutableMap.class, MapCodec.class));

        String generatedFactoryClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineStateFactory";
        DynamicType.Builder<?> factoryBuilder = byteBuddy
                .subclass(Object.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(generatedFactoryClassName)
                .implement(StateDefinitionProxy.FactoryProxy.CLASS)
                .method(ElementMatchers.named("create"))
                .intercept(MethodDelegation.to(CreateStateInterceptor.INSTANCE));

        Class<?> clazz$Factory = factoryBuilder.make().load(BlockStateGenerator.class.getClassLoader()).getLoaded();
        instance$StateDefinition$Factory = ReflectionUtils.getTheOnlyConstructor(clazz$Factory).newInstance();
    }

    public static class GetDropsInterceptor {
        public static final GetDropsInterceptor INSTANCE = new GetDropsInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            ImmutableBlockState state = ((DelegatingBlockState) thisObj).blockState();
            if (state == null) return List.of();
            Object builder = args[0];
            Object vec3 = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.ORIGIN);
            if (vec3 == null) return List.of();

            Object tool = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.TOOL);
            Item<ItemStack> item = BukkitItemManager.instance().wrap(tool == null ? null : CraftItemStackProxy.INSTANCE.asCraftMirror(tool));
            Object optionalPlayer = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.THIS_ENTITY);
            if (!PlayerProxy.CLASS.isInstance(optionalPlayer)) {
                optionalPlayer = null;
            }

            // do not drop if it's not the correct tool
            BlockSettings settings = state.settings();
            if (optionalPlayer != null && settings.requireCorrectTool()) {
                if (item.isEmpty()) return List.of();
                if (!settings.isCorrectTool(item.id()) &&
                        (!settings.respectToolComponent() || !ItemStackProxy.INSTANCE.isCorrectToolForDrops(tool, state.customBlockState().literalObject()))) {
                    return List.of();
                }
            }

            Object serverLevel = LootParamsProxy.BuilderProxy.INSTANCE.getLevel(builder);
            World world = BukkitAdaptors.adapt(LevelProxy.INSTANCE.getWorld(serverLevel));
            ContextHolder.Builder lootBuilder = new ContextHolder.Builder()
                    .withParameter(DirectContextParameters.POSITION, new WorldPosition(world, Vec3Proxy.INSTANCE.getX(vec3), Vec3Proxy.INSTANCE.getY(vec3), Vec3Proxy.INSTANCE.getZ(vec3)));
            if (!item.isEmpty()) {
                lootBuilder.withParameter(DirectContextParameters.ITEM_IN_HAND, item);
            }
            BukkitServerPlayer player = optionalPlayer != null ? BukkitCraftEngine.instance().adapt(ServerPlayerProxy.INSTANCE.getBukkitEntity(optionalPlayer)) : null;
            if (player != null) {
                lootBuilder.withParameter(DirectContextParameters.PLAYER, player);
            }
            Float radius = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.EXPLOSION_RADIUS);
            if (radius != null) {
                lootBuilder.withParameter(DirectContextParameters.EXPLOSION_RADIUS, radius);
            }
            return state.getDrops(lootBuilder, world, player).stream().map(Item::getLiteralObject).toList();
        }
    }

    public static class HasPropertyInterceptor {
        public static final HasPropertyInterceptor INSTANCE = new HasPropertyInterceptor();

        @SuppressWarnings("unchecked")
        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args) {
            Object property = args[0];
            if (property != BlockStatePropertiesProxy.WATERLOGGED) return false;
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            ImmutableBlockState state = customState.blockState();
            if (state == null) return false;
            Property<Boolean> waterloggedProperty = (Property<Boolean>) state.owner().value().getProperty("waterlogged");
            return waterloggedProperty != null;
        }
    }

    // TODO 将 property 获取代理到同名 property 上，并检查类型是否兼容
    public static class GetPropertyValueInterceptor {
        public static final GetPropertyValueInterceptor INSTANCE = new GetPropertyValueInterceptor();

        @SuppressWarnings("unchecked")
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            Object property = args[0];
            if (property != BlockStatePropertiesProxy.WATERLOGGED) return null;
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            ImmutableBlockState state = customState.blockState();
            if (state == null) return null;
            Property<Boolean> waterloggedProperty = (Property<Boolean>) state.owner().value().getProperty("waterlogged");
            if (waterloggedProperty == null) return null;
            return state.get(waterloggedProperty);
        }
    }

    public static class SetPropertyValueInterceptor {
        public static final SetPropertyValueInterceptor INSTANCE = new SetPropertyValueInterceptor();

        @SuppressWarnings("unchecked")
        @RuntimeType
        public Object intercept(@This Object thisObj, @AllArguments Object[] args) {
            Object property = args[0];
            if (property != BlockStatePropertiesProxy.WATERLOGGED) return thisObj;
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            ImmutableBlockState state = customState.blockState();
            if (state == null) return thisObj;
            Property<Boolean> waterloggedProperty = (Property<Boolean>) state.owner().value().getProperty("waterlogged");
            if (waterloggedProperty == null) return thisObj;
            return state.with(waterloggedProperty, (boolean) args[1]).customBlockState().literalObject();
        }
    }

    public static class IsBlockInterceptor {
        public static final IsBlockInterceptor INSTANCE = new IsBlockInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @AllArguments Object[] args) {
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            ImmutableBlockState thisState = customState.blockState();
            if (thisState == null) return false;
            if (BlockProxy.INSTANCE.getDefaultBlockState(args[0]) instanceof DelegatingBlockState holder) {
                ImmutableBlockState holderState = holder.blockState();
                if (holderState == null) return false;
                return holderState.owner().equals(thisState.owner());
            }
            return false;
        }
    }

    public static class CreateStateInterceptor {
        public static final CreateStateInterceptor INSTANCE = new CreateStateInterceptor();

        @RuntimeType
        public Object intercept(@AllArguments Object[] args) throws Throwable {
            return constructor$CraftEngineBlockState.invoke(args[0], args[1], args[2]);
        }
    }
}
