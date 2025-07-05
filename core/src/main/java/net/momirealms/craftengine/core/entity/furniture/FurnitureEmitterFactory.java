package net.momirealms.craftengine.core.entity.furniture;

import java.util.Map;

public interface FurnitureEmitterFactory {
    FurnitureEmitter create(Map<String, Object> arguments);
}
