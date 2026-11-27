package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

public interface NetworkItemProcessor extends ItemProcessor {

    default boolean applyNetworkItem(NetworkItemBuildContext context, @Nullable NetworkItemData networkData) {
        if (networkData != null) {
            this.prepareNetworkItem(context, networkData);
        }
        this.apply(context);
        return true;
    }

    default boolean shouldSkip(ItemPacketSource source) {
        return false;
    }

    default void prepareNetworkItem(NetworkItemBuildContext context, NetworkItemData networkData) {
        Item item = context.item();
        if (VersionHelper.COMPONENT_RELEASE) {
            Key componentType = componentType(item, context);
            if (componentType != null) {
                networkData.saveComponent(componentType);
            }
        } else {
            Object[] path = nbtPath(item, context);
            if (path != null) {
                networkData.saveTag(nbtPathString(item, context), path);
            }
        }
    }

    @Nullable
    default Key componentType(Item item, ItemBuildContext context) {
        return null;
    }

    @Nullable
    default Object[] nbtPath(Item item, ItemBuildContext context) {
        return null;
    }

    default String nbtPathString(Item item, ItemBuildContext context) {
        Object[] path = nbtPath(item, context);
        if (path != null && path.length > 0) {
            StringJoiner builder = new StringJoiner(".");
            for (Object object : path) {
                builder.add(object.toString());
            }
            return builder.toString();
        }
        return "";
    }
}
