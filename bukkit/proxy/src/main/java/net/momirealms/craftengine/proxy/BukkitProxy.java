package net.momirealms.craftengine.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.remapper.Remapper;

import java.util.List;

public final class BukkitProxy {
    private BukkitProxy() {}

    public static void init(String version, List<String> patches) {
        SReflection.setAsmClassPrefix("CraftEngine");
        SReflection.setActivePredicate(new MinecraftPredicate(version, patches));
        SReflection.setRemapper(new WithCraftBukkitClassNameRemapper(Remapper.createFromPaperJar()));
    }
}
