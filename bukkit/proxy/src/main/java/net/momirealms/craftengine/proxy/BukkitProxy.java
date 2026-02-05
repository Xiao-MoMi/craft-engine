package net.momirealms.craftengine.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.remapper.Remapper;

public final class BukkitProxy {
    private BukkitProxy() {}

    public static void init(String version) {
        SReflection.setRemapper(Remapper.createFromPaperJar());
        SReflection.setAsmClassPrefix("CraftEngine");
        SReflection.setActivePredicate(new MinecraftVersionPredicate(version));
    }
}
