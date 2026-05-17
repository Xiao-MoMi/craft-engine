package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
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
        return maxVersion;
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
        Item orginalItem = item;
        for (Version version : this.versions) {
            if (currentVersion < version.version) {
                item = version.apply(item, buildContext);
            }
        }
        item.setTag(this.maxVersion, ItemVersionProcessor.VERSION_TAG);
        return new ItemUpdateResult(item, orginalItem != item, true);
    }

    public record Version(int version, ItemUpdater[] updaters) implements Comparable<Version> {

        public <T> Item apply(Item item, ItemBuildContext context) {
            for (ItemUpdater updater : updaters) {
                item = updater.update(item, context);
            }
            return item;
        }

        @Override
        public int compareTo(@NotNull ItemUpdateConfig.Version o) {
            return Integer.compare(this.version, o.version);
        }
    }
}
