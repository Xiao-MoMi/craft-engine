package net.momirealms.craftengine.core.item;

import org.jetbrains.annotations.Nullable;

public interface ExternalItemProvider<I> {

    String plugin();

    @Nullable
    I build(String id, ItemBuildContext context);

    String id(I item);
}
