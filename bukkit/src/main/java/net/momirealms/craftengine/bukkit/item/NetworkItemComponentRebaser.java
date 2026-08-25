package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.PatchedDataComponentMapProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

final class NetworkItemComponentRebaser {

    private NetworkItemComponentRebaser() {
    }

    static boolean rebase(Object itemStack, @Nullable Object originalPrototype) {
        if (originalPrototype == null) {
            return false;
        }

        Object effectiveComponents = ItemStackProxy.INSTANCE.getComponents(itemStack);
        Object rebasedComponents = PatchedDataComponentMapProxy.INSTANCE.newInstance(originalPrototype);
        PatchedDataComponentMapProxy.INSTANCE.setAll(rebasedComponents, effectiveComponents);

        Set<Object> effectiveTypes = DataComponentMapProxy.INSTANCE.keySet(effectiveComponents);
        for (Object type : DataComponentMapProxy.INSTANCE.keySet(originalPrototype)) {
            if (requiresExplicitRemoval(effectiveTypes, type)) {
                PatchedDataComponentMapProxy.INSTANCE.remove(rebasedComponents, type);
            }
        }

        ItemStackProxy.INSTANCE.setComponents(itemStack, rebasedComponents);
        return true;
    }

    static boolean requiresExplicitRemoval(Set<Object> effectiveTypes, Object originalType) {
        return !effectiveTypes.contains(originalType);
    }
}
