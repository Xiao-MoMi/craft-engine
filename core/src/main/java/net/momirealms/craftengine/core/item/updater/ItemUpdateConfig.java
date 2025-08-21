package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.modifier.ItemVersionModifier;
import net.momirealms.sparrow.nbt.NumericTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ItemUpdateConfig {
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

    public ItemUpdateResult update(Item<?> item, Supplier<ItemBuildContext> context) {
        Tag versionTag = item.getTag(ItemVersionModifier.VERSION_TAG);
        int currentVersion = 0;
        if (versionTag instanceof NumericTag numericTag) {
            currentVersion = numericTag.getAsInt();
        }
        if (currentVersion >= this.maxVersion) {
            return new ItemUpdateResult(item, false, false);
        }
        ItemBuildContext buildContext = context.get();
        Item<?> orginalItem = item;
        for (Version version : this.versions) {
            if (currentVersion < version.version) {
                item = version.apply(item, buildContext);
            }
        }
        item.setTag(this.maxVersion, ItemVersionModifier.VERSION_TAG);
        return new ItemUpdateResult(item, orginalItem != item, true);
    }

    public record Version(int version, ItemUpdater<?>[] updaters) implements Comparable<Version> {

        @SuppressWarnings("unchecked")
        public <T> Item<T> apply(Item<T> item, ItemBuildContext context) {
            for (ItemUpdater<T> updater : (ItemUpdater<T>[]) updaters) {
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
