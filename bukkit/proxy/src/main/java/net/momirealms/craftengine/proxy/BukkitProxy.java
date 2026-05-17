package net.momirealms.craftengine.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.remapper.Remapper;

import java.lang.invoke.MethodHandles;
import java.util.List;

public final class BukkitProxy {
    private static boolean init;

    private BukkitProxy() {}

    public static void init(String version, List<String> patches, MethodHandles.Lookup lookup) {
        if (!init) {
            SReflection.init(lookup);
            SReflection.setAsmClassPrefix("CraftEngine");
            SReflection.setActivePredicate(new MinecraftPredicate(version, patches));
            Remapper remapper = Remapper.createFromPaperJar();
            if (remapper != Remapper.noOp()) {
                SReflection.setRemapper(CraftBukkitRemapper.create(remapper));
            }
            init = true;
        }
    }
}
