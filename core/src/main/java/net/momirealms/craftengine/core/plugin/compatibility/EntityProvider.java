package net.momirealms.craftengine.core.plugin.compatibility;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EntityProvider {

    @NotNull
    String plugin();

    @Nullable
    Entity spawnEntity(WorldPosition position, String id, Context context);

    @Nullable
    String getEntityId(Entity entity);
}
