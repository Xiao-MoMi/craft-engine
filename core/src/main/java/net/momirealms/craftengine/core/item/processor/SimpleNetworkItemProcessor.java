package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

public interface SimpleNetworkItemProcessor extends ItemProcessor {

    @Override
    default Item prepareNetworkItem(Item item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.COMPONENT_RELEASE) {
            Key componentType= componentType(item, context);
            if (componentType != null) {
                Tag previous = item.getComponentAsSparrowTag(componentType);
                if (previous != null) {
                    networkData.put(componentType.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put(componentType.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            }
        } else {
            Object[] path = nbtPath(item, context);
            if (path != null) {
                Tag previous = item.getSparrowTag(path);
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
