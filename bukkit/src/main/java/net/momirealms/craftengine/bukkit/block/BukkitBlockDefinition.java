package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.core.block.AbstractBlockDefinition;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateVariantProvider;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.registry.Holder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class BukkitBlockDefinition extends AbstractBlockDefinition {

    public BukkitBlockDefinition(
            @NotNull Holder.Reference<BlockDefinition> holder,
            @NotNull BlockStateVariantProvider variantProvider,
            @NotNull Map<EventTrigger, List<Function<Context>>> events,
            @Nullable Loot loot
    ) {
        super(holder, variantProvider, events, loot);
    }
}
