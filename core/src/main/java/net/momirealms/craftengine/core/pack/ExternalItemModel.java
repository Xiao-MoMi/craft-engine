package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.util.Key;

import java.util.Objects;

public final class ExternalItemModel {
    private final Key baseItem;
    private final Key itemModel;

    public ExternalItemModel(Key baseItem, Key itemModel) {
        this.baseItem = baseItem;
        this.itemModel = itemModel;
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
        return Objects.equals(this.baseItem, that.baseItem) && Objects.equals(this.itemModel, that.itemModel);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(this.baseItem) + Objects.hashCode(this.itemModel);
    }

    @Override
    public String toString() {
        return "ExternalItemModel[baseItem=" + this.baseItem + ", itemModel=" + this.itemModel + ']';
    }
}
