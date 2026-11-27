package net.momirealms.craftengine.core.item.network;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public final class NetworkItemPipeline {
    public static final NetworkItemProcessor[] EMPTY_PROCESSORS = new NetworkItemProcessor[0];

    private NetworkItemPipeline() {
    }

    public static Result process(Item original, Item clientItem, @Nullable Player player, ItemPacketSource source,
                                 NetworkItemProcessor[] itemProcessors, NetworkItemProcessor[] globalProcessors) {
        NetworkItemBuildContext context = NetworkItemBuildContext.of(player, clientItem, source);
        NetworkItemData networkData = source.requireNetworkTag ? new NetworkItemData(original) : null;
        boolean changed = apply(context, itemProcessors, networkData);
        changed |= apply(context, globalProcessors, networkData);
        return new Result(context.item(), changed && networkData != null ? networkData.tag() : null, changed);
    }

    private static boolean apply(NetworkItemBuildContext context, NetworkItemProcessor[] processors, @Nullable NetworkItemData networkData) {
        boolean changed = false;
        for (NetworkItemProcessor processor : processors) {
            if (processor.shouldSkip(context.source())) {
                continue;
            }
            changed |= processor.applyNetworkItem(context, networkData);
        }
        return changed;
    }

    public record Result(Item item, @Nullable CompoundTag networkData, boolean changed) {
    }
}
