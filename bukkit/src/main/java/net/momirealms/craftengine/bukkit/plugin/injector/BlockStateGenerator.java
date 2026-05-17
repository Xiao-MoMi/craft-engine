package net.momirealms.craftengine.bukkit.plugin.injector;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
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
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.loot.DatapackLootTable;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.block.DelegatingBlockState;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.setting.BlockSettings;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.loot.LootTableReference;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
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
import net.momirealms.sparrow.reflection.constructor.SConstructor3;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BlockStateGenerator {
    private static SConstructor3 constructor$CraftEngineBlockState;
    public static Object instance$StateDefinition$Factory;
    private static final Cache<Pair<Property<?>, Object>, Boolean> COMPATIBLE_PROPERTIES = Caffeine.newBuilder()
            .scheduler(Scheduler.systemScheduler())
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public static void init() {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        String packageWithName = BlockStateGenerator.class.getName();
        String generatedStateClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineBlockState";
        DynamicType.Builder<?> stateBuilder = byteBuddy
                .subclass(BlockStateProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(generatedStateClassName)
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
        SparrowClass<?> clazz$CraftEngineBlock = SparrowClass.of(stateBuilder.make().load(BlockStateGenerator.class.getClassLoader()).getLoaded());

        constructor$CraftEngineBlockState = clazz$CraftEngineBlock.getSparrowConstructor(ConstructorMatcher.takeArguments(
                BlockProxy.CLASS,
                VersionHelper.isOrAbove26_1 ? PropertyProxy.CLASS.arrayType() : VersionHelper.isOrAbove1_20_5 ? Reference2ObjectArrayMap.class : ImmutableMap.class,
                VersionHelper.isOrAbove26_1 ? Comparable.class.arrayType() : MapCodec.class
        )).asm$3();

        String generatedFactoryClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineStateFactory";
        DynamicType.Builder<?> factoryBuilder = byteBuddy
                .subclass(Object.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(generatedFactoryClassName)
                .implement(StateDefinitionProxy.FactoryProxy.CLASS)
                .method(ElementMatchers.named("create"))
                .intercept(MethodDelegation.to(CreateStateInterceptor.INSTANCE));

        SparrowClass<?> clazz$Factory = SparrowClass.of(factoryBuilder.make().load(BlockStateGenerator.class.getClassLoader()).getLoaded());
        instance$StateDefinition$Factory = clazz$Factory.getSparrowConstructor(ConstructorMatcher.any()).asm$0().newInstance();
    }

    public static class GetDropsInterceptor {
        public static final GetDropsInterceptor INSTANCE = new GetDropsInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisObj, @Argument(value = 0) Object builder) {
            ImmutableBlockState state = ((DelegatingBlockState) thisObj).blockState();
            if (state == null) return List.of();
            Object vec3 = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.ORIGIN);
            if (vec3 == null) return List.of();

            Object tool = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.TOOL);
            Item item = BukkitItemManager.instance().wrap(tool == null ? null : ItemStackUtils.getBukkitStack(tool));
            Object optionalPlayer = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.THIS_ENTITY);
            if (!PlayerProxy.CLASS.isInstance(optionalPlayer)) {
                optionalPlayer = null;
            }

            // do not drop if it's not the correct tool
            BlockSettings settings = state.settings();
            if (optionalPlayer != null && settings.requireCorrectTool()) {
                if (item.isEmpty()) return List.of();
                if (!settings.isCorrectTool(item.id()) &&
                        (!settings.respectToolComponent() || !ItemStackProxy.INSTANCE.isCorrectToolForDrops(tool, state.customBlockState().minecraftState()))) {
                    return List.of();
                }
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
            ContextHolder.Builder lootBuilder = new ContextHolder.Builder()
                    .withParameter(DirectContextParameters.POSITION, new WorldPosition(world, Vec3Proxy.INSTANCE.getX(vec3), Vec3Proxy.INSTANCE.getY(vec3), Vec3Proxy.INSTANCE.getZ(vec3)));
            if (!item.isEmpty()) {
                lootBuilder.withParameter(DirectContextParameters.ITEM_IN_HAND, item);
            }
            BukkitServerPlayer player = optionalPlayer != null ? BukkitAdaptor.adapt(ServerPlayerProxy.INSTANCE.getBukkitEntity(optionalPlayer)) : null;
            if (player != null) {
                lootBuilder.withParameter(DirectContextParameters.PLAYER, player);
            }
            Float radius = LootParamsProxy.BuilderProxy.INSTANCE.getOptionalParameter(builder, LootContextParamsProxy.EXPLOSION_RADIUS);
            if (radius != null) {
                lootBuilder.withParameter(DirectContextParameters.EXPLOSION_RADIUS, radius);
            }
            return state.getDrops(lootBuilder, world, player).stream().map(Item::minecraftItem).toList();
        }
    }

    public static class HasPropertyInterceptor {
        public static final HasPropertyInterceptor INSTANCE = new HasPropertyInterceptor();

        @SuppressWarnings("DuplicatedCode")
        @RuntimeType
        public boolean intercept(@This Object thisObj, @Argument(value = 0) Object mcProperty) {
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            ImmutableBlockState state = customState.blockState();
            if (state == null) return false;
            String name = PropertyProxy.INSTANCE.getName(mcProperty);
            Property<?> ceProperty = state.owner().value().getProperty(name);
            if (ceProperty == null) return false;
            Class<?> mcPropertyClass = PropertyProxy.INSTANCE.getValueClass(mcProperty);
            Class<?> cePropertyClass = ceProperty.valueClass();
            if (cePropertyClass == mcPropertyClass) {
                Pair<Property<?>, Object> propertyPair = Pair.of(ceProperty, mcProperty);
                return Boolean.TRUE.equals(COMPATIBLE_PROPERTIES.get(propertyPair,
                        k -> {
                            if (VersionHelper.isOrAbove1_21_2) {
                                return PropertyProxy.INSTANCE.getPossibleValues(mcProperty).equals(ceProperty.possibleValues());
                            } else {
                                Collection<?> possibleMCValues = PropertyProxy.INSTANCE.getPossibleValues(mcProperty);
                                List<?> possibleCEValues = ceProperty.possibleValues();
                                if (possibleMCValues.size() != possibleCEValues.size()) return false;
                                Set<String> possibleMCValueSet = possibleMCValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                Set<String> possibleCEValueSet = possibleCEValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                return possibleMCValueSet.equals(possibleCEValueSet);
                            }
                        }));
            } else if (mcPropertyClass.isEnum() && cePropertyClass.isEnum()) {
                Pair<Property<?>, Object> propertyPair = Pair.of(ceProperty, mcProperty);
                return Boolean.TRUE.equals(COMPATIBLE_PROPERTIES.get(propertyPair,
                        k -> {
                            Collection<?> possibleMCValues = PropertyProxy.INSTANCE.getPossibleValues(mcProperty);
                            List<?> possibleCEValues = ceProperty.possibleValues();
                            if (possibleMCValues.size() != possibleCEValues.size()) return false;
                            if (VersionHelper.isOrAbove1_21_2) {
                                List<?> possibleMCValueList = (List<?>) possibleMCValues;
                                for (int i = 0; i < possibleMCValues.size(); i++) {
                                    if (!possibleMCValueList.get(i).toString().equals(possibleCEValues.get(i).toString())) {
                                        return false;
                                    }
                                }
                            } else {
                                Set<String> possibleMCValueSet = possibleMCValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                Set<String> possibleCEValueSet = possibleCEValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                return possibleMCValueSet.equals(possibleCEValueSet);
                            }
                            return true;
                        }));
            }
            return false;
        }
    }

    public static class GetPropertyValueInterceptor {
        public static final GetPropertyValueInterceptor INSTANCE = new GetPropertyValueInterceptor();

        @SuppressWarnings({"unchecked", "DuplicatedCode", "rawtypes"})
        @RuntimeType
        public Object intercept(@This Object thisObj, @Argument(value = 0) Object mcProperty) {
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            ImmutableBlockState state = customState.blockState();
            if (state == null) return null;
            String name = PropertyProxy.INSTANCE.getName(mcProperty);
            Property<?> ceProperty = state.owner().value().getProperty(name);
            if (ceProperty == null) return null;
            Class<?> mcPropertyClass = PropertyProxy.INSTANCE.getValueClass(mcProperty);
            Class<?> cePropertyClass = ceProperty.valueClass();
            if (cePropertyClass == mcPropertyClass) {
                Pair<Property<?>, Object> propertyPair = Pair.of(ceProperty, mcProperty);
                if (Boolean.TRUE.equals(COMPATIBLE_PROPERTIES.get(propertyPair,
                        k -> {
                            if (VersionHelper.isOrAbove1_21_2) {
                                return PropertyProxy.INSTANCE.getPossibleValues(mcProperty).equals(ceProperty.possibleValues());
                            } else {
                                Collection<?> possibleMCValues = PropertyProxy.INSTANCE.getPossibleValues(mcProperty);
                                List<?> possibleCEValues = ceProperty.possibleValues();
                                if (possibleMCValues.size() != possibleCEValues.size()) return false;
                                Set<String> possibleMCValueSet = possibleMCValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                Set<String> possibleCEValueSet = possibleCEValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                return possibleMCValueSet.equals(possibleCEValueSet);
                            }
                        }))) {
                    return state.get(ceProperty);
                }
            } else if (mcPropertyClass.isEnum() && cePropertyClass.isEnum()) {
                Pair<Property<?>, Object> propertyPair = Pair.of(ceProperty, mcProperty);
                if (Boolean.TRUE.equals(COMPATIBLE_PROPERTIES.get(propertyPair,
                        k -> {
                            Collection<?> possibleMCValues = PropertyProxy.INSTANCE.getPossibleValues(mcProperty);
                            List<?> possibleCEValues = ceProperty.possibleValues();
                            if (possibleMCValues.size() != possibleCEValues.size()) return false;
                            if (VersionHelper.isOrAbove1_21_2) {
                                List<?> possibleMCValueList = (List<?>) possibleMCValues;
                                for (int i = 0; i < possibleMCValues.size(); i++) {
                                    if (!possibleMCValueList.get(i).toString().equals(possibleCEValues.get(i).toString())) {
                                        return false;
                                    }
                                }
                            } else {
                                Set<String> possibleMCValueSet = possibleMCValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                Set<String> possibleCEValueSet = possibleCEValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                return possibleMCValueSet.equals(possibleCEValueSet);
                            }
                            return true;
                        }))) {
                    Class<Enum> mcEnumClass = (Class<Enum>) mcPropertyClass;
                    return Enum.valueOf(mcEnumClass, ((Enum<?>) state.get(ceProperty)).name());
                }
            }
            return null;
        }
    }

    public static class SetPropertyValueInterceptor {
        public static final SetPropertyValueInterceptor INSTANCE = new SetPropertyValueInterceptor();

        @SuppressWarnings({"unchecked", "DuplicatedCode", "rawtypes"})
        @RuntimeType
        public Object intercept(@This Object thisObj, @Argument(value = 0) Object mcProperty, @Argument(value = 1) Object mcValue) {
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            ImmutableBlockState state = customState.blockState();
            if (state == null) return thisObj;
            String name = PropertyProxy.INSTANCE.getName(mcProperty);
            Property<?> ceProperty = state.owner().value().getProperty(name);
            if (ceProperty == null) return thisObj;
            Class<?> mcPropertyClass = PropertyProxy.INSTANCE.getValueClass(mcProperty);
            Class<?> cePropertyClass = ceProperty.valueClass();
            Object valueToSet = null;
            if (cePropertyClass == mcPropertyClass) {
                Pair<Property<?>, Object> propertyPair = Pair.of(ceProperty, mcProperty);
                if (Boolean.TRUE.equals(COMPATIBLE_PROPERTIES.get(propertyPair,
                        k -> {
                            if (VersionHelper.isOrAbove1_21_2) {
                                return PropertyProxy.INSTANCE.getPossibleValues(mcProperty).equals(ceProperty.possibleValues());
                            } else {
                                Collection<?> possibleMCValues = PropertyProxy.INSTANCE.getPossibleValues(mcProperty);
                                List<?> possibleCEValues = ceProperty.possibleValues();
                                if (possibleMCValues.size() != possibleCEValues.size()) return false;
                                Set<String> possibleMCValueSet = possibleMCValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                Set<String> possibleCEValueSet = possibleCEValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                return possibleMCValueSet.equals(possibleCEValueSet);
                            }
                        }))) {
                    valueToSet = mcValue;
                }
            } else if (mcPropertyClass.isEnum() && cePropertyClass.isEnum()) {
                Pair<Property<?>, Object> propertyPair = Pair.of(ceProperty, mcProperty);
                if (Boolean.TRUE.equals(COMPATIBLE_PROPERTIES.get(propertyPair,
                        k -> {
                            Collection<?> possibleMCValues = PropertyProxy.INSTANCE.getPossibleValues(mcProperty);
                            List<?> possibleCEValues = ceProperty.possibleValues();
                            if (possibleMCValues.size() != possibleCEValues.size()) return false;
                            if (VersionHelper.isOrAbove1_21_2) {
                                List<?> possibleMCValueList = (List<?>) possibleMCValues;
                                for (int i = 0; i < possibleMCValues.size(); i++) {
                                    if (!possibleMCValueList.get(i).toString().equals(possibleCEValues.get(i).toString())) {
                                        return false;
                                    }
                                }
                            } else {
                                Set<String> possibleMCValueSet = possibleMCValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                Set<String> possibleCEValueSet = possibleCEValues.stream().map(String::valueOf).collect(Collectors.toSet());
                                return possibleMCValueSet.equals(possibleCEValueSet);
                            }
                            return true;
                        }))) {
                    valueToSet = Enum.valueOf((Class<Enum>) cePropertyClass, ((Enum<?>) mcValue).name());
                }
            }
            if (valueToSet != null) {
                try {
                    return ImmutableBlockState.with(state, ceProperty, valueToSet).customBlockState().minecraftState();
                } catch (IllegalArgumentException e) {
                    return thisObj;
                }
            }
            return thisObj;
        }
    }

    public static class IsBlockInterceptor {
        public static final IsBlockInterceptor INSTANCE = new IsBlockInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisObj, @Argument(value = 0) Object block) {
            DelegatingBlockState customState = (DelegatingBlockState) thisObj;
            Object thisBlock = customState.blockOwner();
            if (thisBlock == null) return false;
            if (BlockProxy.INSTANCE.getDefaultBlockState(block) instanceof DelegatingBlockState holder) {
                Object holderBlock = holder.blockOwner();
                if (holderBlock == null) return false;
                return thisBlock == holderBlock;
            }
            return false;
        }
    }

    public static class CreateStateInterceptor {
        public static final CreateStateInterceptor INSTANCE = new CreateStateInterceptor();

        @RuntimeType
        public Object intercept(@AllArguments Object[] args) {
            return constructor$CraftEngineBlockState.newInstance(args[0], args[1], args[2]);
        }
    }
}
