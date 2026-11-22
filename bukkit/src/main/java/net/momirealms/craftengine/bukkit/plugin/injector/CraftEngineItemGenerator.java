package net.momirealms.craftengine.bukkit.plugin.injector;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.entries.LootPoolEntryTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.entries.SingleEntryContainerBaseProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.entries.UniformContainerBaseProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamsProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor5;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class CraftEngineItemGenerator {
    private static final ByteBuddy BYTE_BUDDY = new ByteBuddy(ClassFileVersion.JAVA_V21);
    private static final Class<?> BASE = SingleEntryContainerBaseProxy.CLASS;
    private static final Constructor<?> PARENT = parentConstructor();
    private static final SConstructor5 ENTRY_CONSTRUCTOR = generateEntry();
    private static final Object TYPE = createType();

    private CraftEngineItemGenerator() {
    }

    public static Object type() {
        return TYPE;
    }

    private static Constructor<?> parentConstructor() {
        Class<?> conditions;
        Class<?> functions;
        if (VersionHelper.isOrAbove26_3) {
            conditions = Optional.class;
            functions = Optional.class;
        } else if (VersionHelper.isOrAbove1_20_2) {
            conditions = List.class;
            functions = List.class;
        } else {
            conditions = requireNonNull(SparrowClass.find("net.minecraft.world.level.storage.loot.predicates.LootItemCondition")).arrayType();
            functions = requireNonNull(SparrowClass.find("net.minecraft.world.level.storage.loot.functions.LootItemFunction")).arrayType();
        }
        return requireNonNull(SparrowClass.of(BASE).getDeclaredConstructor(ConstructorMatcher.takeArguments(int.class, int.class, conditions, functions)));
    }

    private static SConstructor5 generateEntry() {
        Class<?>[] parentParameters = PARENT.getParameterTypes();
        Class<?>[] parameters = {IdentifierProxy.CLASS, int.class, int.class, parentParameters[2], parentParameters[3]};
        Method createItemStack = method(BASE, "createItemStack", Consumer.class, LootContextProxy.CLASS);
        Method typeMethod = requireNonNull(SparrowClass.of(BASE).getMethod(
                MethodMatcher.named(VersionHelper.isOrAbove26_1 ? "codec" : "getType").and(MethodMatcher.takeArguments(new Class<?>[0]))));
        Class<?> generated = load(subclass(BYTE_BUDDY, BASE, "CraftEngineItem")
                .implement(NamedEntry.class)
                .defineField("name", IdentifierProxy.CLASS, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC).withParameters(parameters)
                .intercept(MethodCall.invoke(PARENT).withArgument(1, 2, 3, 4)
                        .andThen(FieldAccessor.ofField("name").setsArgumentAt(0)))
                .method(named("itemName"))
                .intercept(FieldAccessor.ofField("name"))
                .method(is(createItemStack))
                .intercept(MethodCall.invoke(method(CraftEngineItemGenerator.class, "createItemStack", Object.class, Consumer.class, Object.class))
                        .withField("name").withAllArguments())
                .method(is(typeMethod))
                .intercept(MethodCall.invoke(method(CraftEngineItemGenerator.class, "type")).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)));
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(parameters)).asm$5();
    }

    private static Object createType() {
        if (!VersionHelper.isOrAbove1_20_2) {
            return LootPoolEntryTypeProxy.INSTANCE.newInstance(generateSerializer());
        }
        MapCodec<NamedEntry> codec = RecordCodecBuilder.mapCodec(instance ->
                instance.group(IdentifierProxy.INSTANCE.codec().fieldOf("name").forGetter(NamedEntry::itemName))
                        .and(VersionHelper.isOrAbove26_3
                                ? UniformContainerBaseProxy.INSTANCE.uniformFields(instance)
                                : SingleEntryContainerBaseProxy.INSTANCE.singletonFields(instance))
                        .apply(instance, (name, weight, quality, conditions, functions) ->
                                (NamedEntry) ENTRY_CONSTRUCTOR.newInstance(name, weight, quality, conditions, functions)));
        if (VersionHelper.isOrAbove26_1) {
            return codec;
        }
        return VersionHelper.isOrAbove1_20_5
                ? LootPoolEntryTypeProxy.INSTANCE.newInstance(codec)
                : LootPoolEntryTypeProxy.INSTANCE.newInstance(codec.codec());
    }

    private static Object generateSerializer() {
        Class<?> base = SingleEntryContainerBaseProxy.SerializerProxy.CLASS;
        Class<?>[] parentParameters = PARENT.getParameterTypes();
        Method serialize = method(base, "serializeCustom", JsonObject.class, BASE, JsonSerializationContext.class);
        Method deserialize = method(base, "deserialize", JsonObject.class, JsonDeserializationContext.class, int.class, int.class, parentParameters[2], parentParameters[3]);
        MethodDescription serializeCustom = new MethodDescription.ForLoadedMethod(serialize);
        MethodDescription writeName = new MethodDescription.ForLoadedMethod(method(CraftEngineItemGenerator.class, "writeName", JsonObject.class, Object.class));
        // The inherited Gson bridge writes conditions before calling serializeCustom.
        Class<?> generated = load(subclass(BYTE_BUDDY.with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE), base, "CraftEngineItemSerializer")
                .defineConstructor(Visibility.PUBLIC)
                .intercept(MethodCall.invoke(requireNonNull(SparrowClass.of(base).getDeclaredConstructor(ConstructorMatcher.takeArguments(new Class<?>[0])))))
                .defineMethod(serialize.getName(), void.class, Visibility.PUBLIC).withParameters(serialize.getParameterTypes())
                .intercept(new Implementation.Simple(
                        MethodVariableAccess.allArgumentsOf(serializeCustom).prependThisReference(),
                        MethodInvocation.invoke(serializeCustom).special(TypeDescription.ForLoadedType.of(base)),
                        MethodVariableAccess.REFERENCE.loadFrom(1),
                        MethodVariableAccess.REFERENCE.loadFrom(2),
                        MethodInvocation.invoke(writeName),
                        MethodReturn.VOID))
                .defineMethod(deserialize.getName(), deserialize.getReturnType(), Visibility.PROTECTED).withParameters(deserialize.getParameterTypes())
                .intercept(MethodCall.invoke(method(CraftEngineItemGenerator.class, "readEntry", JsonObject.class, int.class, int.class, Object.class, Object.class))
                        .withArgument(0, 2, 3, 4, 5).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)));
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(new Class<?>[0])).asm$0().newInstance();
    }

    private static DynamicType.Builder<?> subclass(ByteBuddy byteBuddy, Class<?> base, String name) {
        return byteBuddy.subclass(base, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(CraftEngineItemGenerator.class.getPackageName() + "." + name);
    }

    private static Class<?> load(DynamicType.Builder<?> builder) {
        return builder.make().load(CraftEngineItemGenerator.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())).getLoaded();
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) {
        return requireNonNull(SparrowClass.of(type).getDeclaredMethod(MethodMatcher.named(name).and(MethodMatcher.takeArguments(parameters))),
                () -> type.getName() + "." + name);
    }

    static void writeName(JsonObject json, Object entry) {
        json.addProperty("name", ((NamedEntry) entry).itemName().toString());
    }

    static Object readEntry(JsonObject json, int weight, int quality, Object conditions, Object functions) {
        Object name = IdentifierProxy.INSTANCE.tryParse(json.get("name").getAsString());
        return ENTRY_CONSTRUCTOR.newInstance(name, weight, quality, conditions, functions);
    }

    static void createItemStack(Object name, Consumer<Object> consumer, Object context) {
        ItemDefinition definition = BukkitItemManager.instance().getItemDefinition(KeyUtils.identifierToKey(name)).orElse(null);
        if (definition == null) {
            return;
        }
        Object parameter = LootContextParamsProxy.THIS_ENTITY;
        Object entity;
        if (VersionHelper.isOrAbove26_3) {
            entity = LootContextProxy.INSTANCE.getOptional(context, parameter);
        } else if (VersionHelper.isOrAbove1_21_2) {
            entity = LootContextProxy.INSTANCE.getOptionalParameter(context, parameter);
        } else {
            entity = LootContextProxy.INSTANCE.getParamOrNull(context, parameter);
        }
        BukkitServerPlayer player = PlayerProxy.CLASS.isInstance(entity)
                ? BukkitAdaptor.adapt((Player) EntityProxy.INSTANCE.getBukkitEntity(entity))
                : null;
        consumer.accept(definition.buildItem(player).minecraftItem());
    }

    public interface NamedEntry {
        Object itemName();
    }
}
