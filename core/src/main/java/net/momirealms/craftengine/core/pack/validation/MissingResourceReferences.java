package net.momirealms.craftengine.core.pack.validation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.momirealms.craftengine.core.pack.ResourcePackView;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.util.Key;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class MissingResourceReferences {
    private final ResourcePackView view;
    private final Set<Key> vanillaResources;
    private final String directory;
    private final String extension;
    private final Set<Key> existing = new HashSet<>();
    private final Multimap<Key, Key> missing;

    MissingResourceReferences(ResourcePackView view,
                              Set<Key> vanillaResources,
                              String directory,
                              String extension,
                              int expectedReferences) {
        this.view = view;
        this.vanillaResources = vanillaResources;
        this.directory = directory;
        this.extension = extension;
        this.missing = HashMultimap.create(0, expectedReferences);
    }

    void add(Key resource, Key reference) {
        if (this.vanillaResources.contains(resource) || this.existing.contains(resource)) {
            return;
        }
        if (!this.missing.containsKey(resource) && this.view.exists(path(resource))) {
            this.existing.add(resource);
            return;
        }
        this.missing.put(resource, reference);
    }

    void report(PluginLogger logger, String translationKey) {
        for (Map.Entry<Key, Collection<Key>> entry : this.missing.asMap().entrySet()) {
            logger.warn(TranslationManager.instance().plainTranslation(translationKey, entry.getValue().toString(), path(entry.getKey())));
        }
    }

    private String path(Key resource) {
        return "assets/" + resource.namespace() + "/" + this.directory + "/" + resource.value() + this.extension;
    }
}
