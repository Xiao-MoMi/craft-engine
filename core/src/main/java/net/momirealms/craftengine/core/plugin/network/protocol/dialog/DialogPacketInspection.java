package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Map;
import java.util.function.Predicate;

public final class DialogPacketInspection {
    private DialogPacketInspection() {
    }

    public static boolean requiresProcessing(Tag tag, Predicate<String> hasNetworkTag) {
        if (tag instanceof StringTag string) {
            return hasNetworkTag.test(string.value());
        }
        if (tag instanceof CompoundTag compound) {
            // Keep item conversion on the existing path, including items in nested dialogs and actions.
            if (compound.containsKey("item")) {
                return true;
            }
            for (Map.Entry<String, Tag> entry : compound.entrySet()) {
                if (requiresProcessing(entry.getValue(), hasNetworkTag)) {
                    return true;
                }
            }
        } else if (tag instanceof ListTag list) {
            for (Tag element : list) {
                if (requiresProcessing(element, hasNetworkTag)) {
                    return true;
                }
            }
        }
        return false;
    }
}
