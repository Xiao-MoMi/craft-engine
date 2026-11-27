package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.util.Key;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ExternalItemModel {
    private final Key baseItem;
    private final Key itemModel;
    @Nullable
    private final Key entityModel;

    public ExternalItemModel(Key baseItem, Key itemModel) {
        this(baseItem, itemModel, null);
    }

    public ExternalItemModel(Key baseItem, Key itemModel, @Nullable Key entityModel) {
        this.entityModel = entityModel;
        this.baseItem = baseItem;
        this.itemModel = itemModel;
    }

    @Nullable
    public Key entityModel() {
        return this.entityModel;
    }

    public Key baseItem() {
        return this.baseItem;
    }

    public Key itemModel() {
        return this.itemModel;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ExternalItemModel that)) {
            return false;
        }
        return Objects.equals(this.baseItem, that.baseItem) && Objects.equals(this.itemModel, that.itemModel) && Objects.equals(this.entityModel, that.entityModel);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Objects.hashCode(this.baseItem) + Objects.hashCode(this.itemModel)) + Objects.hashCode(this.entityModel);
    }
}
