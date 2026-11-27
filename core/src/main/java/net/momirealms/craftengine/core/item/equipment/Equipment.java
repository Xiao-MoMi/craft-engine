package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public interface Equipment {

    Key assetId();

    List<NetworkItemProcessor> modifiers();
}
