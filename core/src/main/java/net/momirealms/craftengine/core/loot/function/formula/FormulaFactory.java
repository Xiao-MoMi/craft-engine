package net.momirealms.craftengine.core.loot.function.formula;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface FormulaFactory<T extends Formula> {

    T create(ConfigSection section);
}