package net.momirealms.craftengine.bukkit.util;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.PatchedDataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

public final class ItemComponentUtils {

    private ItemComponentUtils() {}

    public static Object copy(Object components) {
        Object builder = DataComponentMapProxy.INSTANCE.builder();
        DataComponentMapProxy.BuilderProxy.INSTANCE.addAll(builder, components);
        return DataComponentMapProxy.BuilderProxy.INSTANCE.build(builder);
    }

    public static void replaceContents(Object target, Object components) {
        // Flatten before publishing: components may themselves inherit from target.
        // Keep the prototype object held by existing ItemStacks, and never clear a live map.
        Object snapshot = copy(components);
        Reference2ObjectMap<Object, Object> values = DataComponentMapProxy.INSTANCE.keySet(snapshot).isEmpty()
                ? Reference2ObjectMaps.emptyMap()
                : DataComponentMapProxy.SimpleMapProxy.INSTANCE.getMap(snapshot);
        DataComponentMapProxy.SimpleMapProxy.INSTANCE.setMap(target, values);
    }

    public static void rebasePrototype(Object itemStack, Object prototype) {
        Object patch = ItemStackProxy.INSTANCE.getComponentsPatch(itemStack);
        ItemStackProxy.INSTANCE.setComponents(itemStack, PatchedDataComponentMapProxy.INSTANCE.fromPatch(prototype, patch));
    }
}
