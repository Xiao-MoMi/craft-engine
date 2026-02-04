package net.momirealms.craftengine.core.plugin.config;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.CachedConfigSection;

public abstract class AbstractConfigParser implements ConfigParser {
    protected final ObjectArrayList<CachedConfigSection> configStorage;

    public AbstractConfigParser() {
        this.configStorage = new ObjectArrayList<>();
    }

    @Override
    public void addConfig(CachedConfigSection section) {
        this.configStorage.add(section);
    }

    @Override
    public void loadAll() {
        Object[] elements = this.configStorage.elements();
        for (int i = 0, size = this.configStorage.size(); i < size; i++) {
            parseSection((CachedConfigSection) elements[i]);
        }
    }

    @Override
    public void clearConfigs() {
        this.configStorage.clear();
    }

    protected abstract void parseSection(CachedConfigSection section);
}
