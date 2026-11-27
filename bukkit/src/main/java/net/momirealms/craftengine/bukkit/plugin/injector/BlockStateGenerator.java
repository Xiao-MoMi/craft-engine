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
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.loot.DatapackLootTable;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.loot.LootTableReference;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateDefinitionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.PropertyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamSetsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;

import java.lang.reflect.Constructor;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class BlockStateGenerator {
    public static Object instance$StateDefinition$Factory;

    private BlockStateGenerator() {
    }

    public static void init() {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        DynamicType.Builder<?> stateBuilder = byteBuddy
                .subclass(BlockStateProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(BlockStateGenerator.class.getPackageName() + ".CraftEngineBlockState")
                .defineField("immutableBlockState", ImmutableBlockState.class, Visibility.PUBLIC)
                .defineField("blockOwner", Object.class, Visibility.PUBLIC)
                .implement(DelegatingBlockState.class)
                .method(ElementMatchers.named("blockState"))
                .intercept(FieldAccessor.ofField("immutableBlockState"))
                .method(ElementMatchers.named("setBlockState"))
                .intercept(FieldAccessor.ofField("immutableBlockState"))
                .method(ElementMatchers.named("blockOwner"))
                .intercept(FieldAccessor.ofField("blockOwner"))
                .method(ElementMatchers.named("setBlockOwner"))
                .intercept(FieldAccessor.ofField("blockOwner"))
                .method(ElementMatchers.is(BlockReflections.GET_DROPS))
                .intercept(MethodDelegation.to(GetDropsInterceptor.INSTANCE))
                .method(ElementMatchers.is(BlockReflections.HAS_PROPERTY))
                .intercept(MethodDelegation.withDefaultConfiguration().filter(named("hasProperty")).to(BlockStatePropertyAccess.class))
                .method(ElementMatchers.is(BlockReflections.GET_VALUE))
                .intercept(MethodDelegation.withDefaultConfiguration().filter(named("getValue")).to(BlockStatePropertyAccess.class))
                .method(ElementMatchers.is(BlockReflections.SET_VALUE))
                .intercept(MethodDelegation.withDefaultConfiguration().filter(named("setValue")).to(BlockStatePropertyAccess.class))
                .method(ElementMatchers.is(BlockReflections.IS_BLOCK))
                .intercept(MethodDelegation.to(IsBlockInterceptor.INSTANCE));
        Class<?> stateClass = stateBuilder.make().load(BlockStateGenerator.class.getClassLoader()).getLoaded();
        Constructor<?> constructor = requireNonNull(SparrowClass.of(stateClass).getConstructor(ConstructorMatcher.takeArguments(
                BlockProxy.CLASS,
                VersionHelper.isOrAbove26_1 ? PropertyProxy.CLASS.arrayType() : (VersionHelper.isOrAbove1_20_5 ? Reference2ObjectArrayMap.class : ImmutableMap.class),
                VersionHelper.isOrAbove26_1 ? Comparable.class.arrayType() : MapCodec.class
        )));
        DynamicType.Builder<?> factoryBuilder = byteBuddy
                .subclass(Object.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(BlockStateGenerator.class.getPackageName() + ".CraftEngineStateFactory")
                .implement(StateDefinitionProxy.FactoryProxy.CLASS)
                .method(ElementMatchers.named("create"))
                .intercept(MethodCall.construct(constructor).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));

        SparrowClass<?> clazz$Factory = SparrowClass.of(factoryBuilder.make().load(stateClass.getClassLoader()).getLoaded());
        instance$StateDefinition$Factory = clazz$Factory.getSparrowConstructor(ConstructorMatcher.any()).asm$0().newInstance();
    }

    public static class GetDropsInterceptor {
        public static final GetDropsInterceptor INSTANCE = new GetDropsInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @Argument(value = 0) Object builder) {
            ImmutableBlockState state = ((DelegatingBlockState) thisObj).blockState();
            if (state == null) {
                return List.of();
            }
            Object vec3 = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.ORIGIN);
            if (vec3 == null) {
                return List.of();
            }

            Object tool = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.TOOL);
            Item item = BukkitItemManager.instance().wrap(tool == null ? null : ItemStackUtils.getBukkitStack(tool));
            Object optionalEntity = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.THIS_ENTITY);
            Object optionalPlayer = PlayerProxy.CLASS.isInstance(optionalEntity) ? optionalEntity : null;

            // do not drop if it's not the correct tool
            if (optionalEntity != null && !BlockStateUtils.isCorrectTool(state, item)) {
                return List.of();
            }

            // 数据包 LootTable.
            Loot loot = state.owner().value().loot();
            if (loot instanceof LootTableReference lootTableReference /* 不可能是 DatapackLootTable. */) {
                Loot underlying = lootTableReference.delegate.get();
                if (underlying instanceof DatapackLootTable datapackLootTable) {
                    LootParamsProxy.BuilderProxy.INSTANCE.withParameter(builder, LootContextParamsProxy.BLOCK_STATE, state);
                    Object lootParams = LootParamsProxy.BuilderProxy.INSTANCE.create(builder, LootContextParamSetsProxy.BLOCK);
                    return datapackLootTable.getRandomItemsByLootParams(lootParams);
                }
            }

            // 自定义 LootTable.
            Object serverLevel = LootParamsProxy.BuilderProxy.INSTANCE.getLevel(builder);
            World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(serverLevel));
            ContextHolder.Builder lootBuilder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.POSITION, new WorldPosition(world, Vec3Proxy.INSTANCE.getX(vec3), Vec3Proxy.INSTANCE.getY(vec3), Vec3Proxy.INSTANCE.getZ(vec3)))
                    .withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, state);
            lootBuilder.withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, ItemUtils.emptyToNull(item));
            BukkitServerPlayer player = optionalPlayer != null ? BukkitAdaptor.adapt(ServerPlayerProxy.INSTANCE.getBukkitEntity(optionalPlayer)) : null;
            if (player != null) {
                lootBuilder.withParameter(DirectContextParameters.PLAYER, player);
            }
            if (optionalEntity != null) {
                lootBuilder.withParameter(DirectContextParameters.ENTITY, EntityUtils.adaptNMS(optionalEntity));
            }
            Float radius = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.EXPLOSION_RADIUS);
            if (radius != null) {
                lootBuilder.withParameter(DirectContextParameters.EXPLOSION_RADIUS, radius);
            }
            return state.getDrops(lootBuilder.build(), world, player).stream().map(Item::minecraftItem).toList();
        }
    }

    public static class IsBlockInterceptor {
        public static final IsBlockInterceptor INSTANCE = new IsBlockInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @Argument(value = 0) Object block) {
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            Object thisBlock = customState.blockOwner();
            if (thisBlock == null || block == null) {
                return false;
            }
            if (BlockProxy.INSTANCE.getDefaultBlockState(block) instanceof DelegatingBlockState holder) {
                Object holderBlock = holder.blockOwner();
                return thisBlock == holderBlock;
            }
            return false;
        }
    }
}