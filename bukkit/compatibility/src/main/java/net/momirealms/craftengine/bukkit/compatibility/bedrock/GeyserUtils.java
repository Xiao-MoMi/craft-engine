package net.momirealms.craftengine.bukkit.compatibility.bedrock;

import org.geysermc.api.Geyser;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.extension.Extension;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;

public final class GeyserUtils {
    private static volatile MethodHandle miningProgress;

    private GeyserUtils() {}

    public static boolean isGeyserPlayer(UUID uuid) {
        return Geyser.api().isBedrockPlayer(uuid);
    }

    public static void initializeMiningSync() throws ReflectiveOperationException {
        Extension extension = GeyserApi.api().extensionManager().extension("craftengine");
        miningProgress = extension == null ? null : MethodHandles.publicLookup().findVirtual(extension.getClass(),
                "sendMiningProgress", MethodType.methodType(boolean.class, UUID.class, int.class, int.class,
                        int.class, int.class, int.class, float.class, float.class)).bindTo(extension);
    }

    public static boolean sendMiningProgress(UUID viewer, int breakerId, int stateId, int x, int y, int z, float progress, float speed) {
        MethodHandle handle = miningProgress;
        if (handle == null) {
            return false;
        }
        try {
            return (boolean) handle.invokeExact(viewer, breakerId, stateId, x, y, z, progress, speed);
        } catch (Throwable exception) {
            throw new IllegalStateException("Failed to synchronize CraftEngine mining with Geyser", exception);
        }
    }
}
