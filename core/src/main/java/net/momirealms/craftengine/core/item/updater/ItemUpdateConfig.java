package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.processor.ItemVersionProcessor;
import net.momirealms.sparrow.nbt.NumericTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ItemUpdateConfig {
    private final List<Version> versions;
    private final int maxVersion;

    public ItemUpdateConfig(List<Version> versions) {
        this.versions = new ArrayList<>(versions);
        this.versions.sort(Version::compareTo);
        int maxVersion = 0;
        for (Version version : versions) {
            maxVersion = Math.max(maxVersion, version.version);
        }
        this.maxVersion = maxVersion;
    }

    public int maxVersion() {
        return this.maxVersion;
    }

    public ItemUpdateResult update(Item item, Supplier<ItemBuildContext> context) {
        Tag versionTag = item.getSparrowTag(ItemVersionProcessor.VERSION_TAG);
        int currentVersion = 0;
        if (versionTag instanceof NumericTag numericTag) {
            currentVersion = numericTag.getAsInt();
        }
        if (currentVersion >= this.maxVersion) {
            return new ItemUpdateResult(item, false, false);
        }
        ItemBuildContext buildContext = context.get();
        Item originalItem = item;
        buildContext.setItem(originalItem);
        for (Version version : this.versions) {
            if (currentVersion < version.version) {
                version.apply(buildContext);
            }
        }
        item = buildContext.item();
        int finalVersion = this.maxVersion;
        ItemDefinition definition = item.getDefinitionOrNull();
        if (definition != null && !definition.isVanillaItem()) {
            ItemUpdateConfig updater = definition.updater().orElse(null);
            finalVersion = updater != null ? updater.maxVersion() : 0;
        }
        item.setTag(finalVersion, ItemVersionProcessor.VERSION_TAG);
        return new ItemUpdateResult(item, originalItem != item, true);
    }

    public record Version(int version, ItemUpdater[] updaters) implements Comparable<Version> {

        public void apply(ItemBuildContext context) {
            for (ItemUpdater updater : updaters) {
                updater.update(context);
            }
        }

        @Override
        public int compareTo(@NotNull ItemUpdateConfig.Version o) {
            return Integer.compare(this.version, o.version);
        }
    }
}
