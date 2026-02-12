package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MappedRegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;

import java.util.Set;

public final class LootEntryInjector {
    private LootEntryInjector() {}

    public static void init() {
        Object registry = MBuiltInRegistries.LOOT_POOL_ENTRY_TYPE;
        MappedRegistryProxy.INSTANCE.setFrozen(registry, false);
        Object identifier = KeyUtils.toIdentifier(Key.ce("item"));
        Object type = FastNMS.INSTANCE.getCraftEngineLootItemType();
        Object holder = RegistryProxy.INSTANCE.registerForHolder$1(registry, identifier, type);
        HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, type);
        HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
        MappedRegistryProxy.INSTANCE.setFrozen(registry, true);
    }
}
