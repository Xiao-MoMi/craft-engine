package net.momirealms.craftengine.core.attribute.modifier.removal;

import net.momirealms.craftengine.core.plugin.context.Context;

public record RemovalContext(Context context, long boundTick, long currentTick, long currentTimeMillis) {
}
