package net.momirealms.craftengine.bukkit.compatibility.packetevents;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.sparrow.reflection.SReflection;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.Map;

public final class WrappedBlockStateHelper {
    private static MethodHandle methodHandle$WrappedBlockState$BY_STRING$getter;
    private static MethodHandle methodHandle$WrappedBlockState$BY_ID$getter;
    private static MethodHandle methodHandle$WrappedBlockState$INTO_STRING$getter;
    private static MethodHandle methodHandle$WrappedBlockState$INTO_ID$getter;
    private static MethodHandle methodHandle$WrappedBlockState$DEFAULT_STATES$getter;
    private static MethodHandle methodHandle$WrappedBlockState$loadMappings;
    private static MethodHandle methodHandle$WrappedBlockState$constructor;
    private static MethodHandle methodHandle$StateTypes$builder;
    private static MethodHandle methodHandle$StateTypes$builder$name;
    private static MethodHandle methodHandle$StateTypes$builder$isBlocking;
    private static MethodHandle methodHandle$StateTypes$builder$setMaterial;
    private static MethodHandle methodHandle$StateTypes$builder$build;
    private static MethodHandle methodHandle$StateTypes$REGISTRY$getter;
    private static MethodHandle methodHandle$StateTypes$REGISTRY$getTypesBuilder;
    private static MethodHandle methodHandle$StateTypes$REGISTRY$load;
    private static MethodHandle methodHandle$StateTypes$REGISTRY$unloadFileMappings;
    private static Object instance$MaterialType$STONE;
    private static Object clientVersion;

    private WrappedBlockStateHelper() {}

    @SuppressWarnings("unchecked")
    public static void register(@Nullable String packageName) throws Throwable {
        init(packageName);
        byte mappingsIndex = (byte) methodHandle$WrappedBlockState$loadMappings.invoke(clientVersion);
        Map<String, Object>[] BY_STRING = (Map<String, Object>[]) methodHandle$WrappedBlockState$BY_STRING$getter.invoke();
        Map<Integer, Object>[] BY_ID = (Map<Integer, Object>[]) methodHandle$WrappedBlockState$BY_ID$getter.invoke();
        Map<Object, String>[] INTO_STRING = (Map<Object, String>[]) methodHandle$WrappedBlockState$INTO_STRING$getter.invoke();
        Map<Object, Integer>[] INTO_ID = (Map<Object, Integer>[]) methodHandle$WrappedBlockState$INTO_ID$getter.invoke();
        Map<Object, Object>[] DEFAULT_STATES = (Map<Object, Object>[]) methodHandle$WrappedBlockState$DEFAULT_STATES$getter.invoke();
        Map<String, Object> stringWrappedBlockStateMap = BY_STRING[mappingsIndex];
        Map<Integer, Object> integerWrappedBlockStateMap = BY_ID[mappingsIndex];
        Map<Object, String> wrappedBlockStateStringMap = INTO_STRING[mappingsIndex];
        Map<Object, Integer> wrappedBlockStateIntegerMap = INTO_ID[mappingsIndex];
        Map<Object, Object> stateTypeWrappedBlockStateMap = DEFAULT_STATES[mappingsIndex];
        Object typesBuilder = methodHandle$StateTypes$REGISTRY$getTypesBuilder.invoke(methodHandle$StateTypes$REGISTRY$getter.invoke());
        methodHandle$StateTypes$REGISTRY$load.invoke(typesBuilder);
        for (int i = 0; i < Config.serverSideBlocks(); i++) {
            String blockId = "craftengine:custom_" + i;
            int id = BlockStateUtils.vanillaBlockStateCount() + i;
            Object stateType = methodHandle$StateTypes$builder$build.invoke(
                    methodHandle$StateTypes$builder$setMaterial.invoke(
                            methodHandle$StateTypes$builder$isBlocking.invoke(
                                    methodHandle$StateTypes$builder$name.invoke(
                                            methodHandle$StateTypes$builder.invoke(),
                                            blockId
                                    ), true
                            ), instance$MaterialType$STONE
                    )
            );
            Object wrappedBlockState = methodHandle$WrappedBlockState$constructor.invoke(stateType, Collections.emptyMap(), id, mappingsIndex);
            stringWrappedBlockStateMap.put(blockId, wrappedBlockState);
            integerWrappedBlockStateMap.put(id, wrappedBlockState);
            wrappedBlockStateStringMap.put(wrappedBlockState, blockId);
            wrappedBlockStateIntegerMap.put(wrappedBlockState, id);
            stateTypeWrappedBlockStateMap.put(stateType, wrappedBlockState);
        }
        methodHandle$StateTypes$REGISTRY$unloadFileMappings.invoke(typesBuilder);
    }

    private static void init(@Nullable String packageName) throws Throwable {
        packageName = (packageName != null ? packageName : "com{}github{}retrooper{}packetevents").replace("{}", ".");
        Class<?> clazz$WrappedBlockState = Class.forName(packageName + ".protocol.world.states.WrappedBlockState");
        Class<?> clazz$PacketEvents = Class.forName(packageName + ".PacketEvents");
        Class<?> clazz$PacketEventsAPI = Class.forName(packageName + ".PacketEventsAPI");
        Class<?> clazz$ServerManager = Class.forName(packageName + ".manager.server.ServerManager");
        Class<?> clazz$ServerVersion = Class.forName(packageName + ".manager.server.ServerVersion");
        Class<?> clazz$ClientVersion = Class.forName(packageName + ".protocol.player.ClientVersion");
        Class<?> clazz$StateType = Class.forName(packageName + ".protocol.world.states.type.StateType");
        Class<?> clazz$StateTypes = Class.forName(packageName + ".protocol.world.states.type.StateTypes");
        Class<?> clazz$StateTypes$Builder = Class.forName(packageName + ".protocol.world.states.type.StateTypes$Builder");
        Class<?> clazz$MaterialType = Class.forName(packageName + ".protocol.world.MaterialType");
        Class<?> clazz$VersionedRegistry = Class.forName(packageName + ".util.mappings.VersionedRegistry");
        Class<?> clazz$TypesBuilder = Class.forName(packageName + ".util.mappings.TypesBuilder");
        MethodHandle methodHandle$PacketEvents$getAPI = SReflection.unreflectMethod(clazz$PacketEvents.getDeclaredMethod("getAPI"));
        MethodHandle methodHandle$PacketEventsAPI$getServerManager = SReflection.unreflectMethod(clazz$PacketEventsAPI.getDeclaredMethod("getServerManager"));
        MethodHandle methodHandle$ServerManager$getVersion = SReflection.unreflectMethod(clazz$ServerManager.getDeclaredMethod("getVersion"));
        MethodHandle methodHandle$ServerVersion$toClientVersion = SReflection.unreflectMethod(clazz$ServerVersion.getDeclaredMethod("toClientVersion"));
        methodHandle$WrappedBlockState$BY_STRING$getter = SReflection.unreflectGetter(clazz$WrappedBlockState.getDeclaredField("BY_STRING"));
        methodHandle$WrappedBlockState$BY_ID$getter = SReflection.unreflectGetter(clazz$WrappedBlockState.getDeclaredField("BY_ID"));
        methodHandle$WrappedBlockState$INTO_STRING$getter = SReflection.unreflectGetter(clazz$WrappedBlockState.getDeclaredField("INTO_STRING"));
        methodHandle$WrappedBlockState$INTO_ID$getter = SReflection.unreflectGetter(clazz$WrappedBlockState.getDeclaredField("INTO_ID"));
        methodHandle$WrappedBlockState$DEFAULT_STATES$getter = SReflection.unreflectGetter(clazz$WrappedBlockState.getDeclaredField("DEFAULT_STATES"));
        methodHandle$WrappedBlockState$loadMappings = SReflection.unreflectMethod(clazz$WrappedBlockState.getDeclaredMethod("loadMappings", clazz$ClientVersion));
        methodHandle$WrappedBlockState$constructor = SReflection.unreflectConstructor(clazz$WrappedBlockState.getDeclaredConstructor(clazz$StateType, Map.class, int.class, byte.class));
        methodHandle$StateTypes$builder = SReflection.unreflectMethod(clazz$StateTypes.getDeclaredMethod("builder"));
        methodHandle$StateTypes$builder$name = SReflection.unreflectMethod(clazz$StateTypes$Builder.getDeclaredMethod("name", String.class));
        methodHandle$StateTypes$builder$isBlocking = SReflection.unreflectMethod(clazz$StateTypes$Builder.getDeclaredMethod("isBlocking", boolean.class));
        methodHandle$StateTypes$builder$setMaterial = SReflection.unreflectMethod(clazz$StateTypes$Builder.getDeclaredMethod("setMaterial", clazz$MaterialType));
        methodHandle$StateTypes$builder$build = SReflection.unreflectMethod(clazz$StateTypes$Builder.getDeclaredMethod("build"));
        methodHandle$StateTypes$REGISTRY$getter = SReflection.unreflectGetter(clazz$StateTypes.getDeclaredField("REGISTRY"));
        methodHandle$StateTypes$REGISTRY$getTypesBuilder = SReflection.unreflectMethod(clazz$VersionedRegistry.getDeclaredMethod("getTypesBuilder"));
        methodHandle$StateTypes$REGISTRY$load = SReflection.unreflectMethod(clazz$TypesBuilder.getDeclaredMethod("load"));
        methodHandle$StateTypes$REGISTRY$unloadFileMappings = SReflection.unreflectMethod(clazz$TypesBuilder.getDeclaredMethod("unloadFileMappings"));
        instance$MaterialType$STONE = clazz$MaterialType.getDeclaredField("STONE").get(null);
        clientVersion = methodHandle$ServerVersion$toClientVersion.invoke(methodHandle$ServerManager$getVersion.invoke(methodHandle$PacketEventsAPI$getServerManager.invoke(methodHandle$PacketEvents$getAPI.invoke())));
    }
}
