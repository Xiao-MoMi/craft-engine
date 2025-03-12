package net.momirealms.craftengine.core.pack.model.special;

import java.util.Map;

public interface SpecialModelFactory {

    SpecialModel create(Map<String, Object> arguments);
}
