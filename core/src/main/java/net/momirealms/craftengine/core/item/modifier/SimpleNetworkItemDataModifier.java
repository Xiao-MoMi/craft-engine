package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public interface SimpleNetworkItemDataModifier<I> extends ItemDataModifier<I> {

    @Override
    default Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.COMPONENT_RELEASE) {
            Key componentType= componentType(item, context);
            if (componentType != null) {
                Tag previous = item.getSparrowNBTComponent(componentType);
                if (previous != null) {
                    networkData.put(componentType.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put(componentType.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            }
        } else {
            Object[] path = nbtPath(item, context);
            if (path != null) {
                Tag previous = item.getTag(path);
                if (previous != null) {
                    networkData.put(nbtPathString(item, context), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put(nbtPathString(item, context), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            }
        }
        return item;
    }

    @Nullable
    default Key componentType(Item<I> item, ItemBuildContext context) {
        return null;
    }

    @Nullable
    default Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return null;
    }

    default String nbtPathString(Item<I> item, ItemBuildContext context) {
        Object[] path = nbtPath(item, context);
        if (path != null && path.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (Object object : path) {
                builder.append(object.toString());
            }
            return builder.toString();
        }
        return "";
    }
}
