package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.util.Key;

import java.util.Set;

public final class FeatureInjector {

    private FeatureInjector() {}

    public static void init() throws ReflectiveOperationException {
        Object registry = MBuiltInRegistries.FEATURE;
        CoreReflections.field$MappedRegistry$frozen.set(registry, false);
        Object resourceLocation = KeyUtils.toIdentifier(Key.ce("simple_block"));
        Object type = FastNMS.INSTANCE.getCraftEngineCustomSimpleBlockFeature();
        Object holder = CoreReflections.method$Registry$registerForHolder.invoke(null, registry, resourceLocation, type);
        CoreReflections.method$Holder$Reference$bindValue.invoke(holder, type);
        CoreReflections.field$Holder$Reference$tags.set(holder, Set.of());
        CoreReflections.field$MappedRegistry$frozen.set(registry, true);
    }
}
