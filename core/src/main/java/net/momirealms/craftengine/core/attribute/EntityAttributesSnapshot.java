package net.momirealms.craftengine.core.attribute;

import net.momirealms.craftengine.core.customdata.CustomDataKey;
import net.momirealms.craftengine.core.util.CustomDataSerializer;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

public final class EntityAttributesSnapshot implements AttributeGetter {
    public static final CustomDataKey<EntityAttributesSnapshot> PROJECTILE_DATA_KEY = new CustomDataKey<>(
            Key.ce("projectile_attribute_snapshot"),
            new CustomDataSerializer<>() {
                @Override
                public Tag serialize(EntityAttributesSnapshot snapshot) {
                    return snapshot.data;
                }

                @Override
                public EntityAttributesSnapshot deserialize(Tag tag) {
                    return new EntityAttributesSnapshot(tag instanceof CompoundTag compound ? compound : new CompoundTag());
                }
            }
    );

    private final CompoundTag data;

    EntityAttributesSnapshot(CompoundTag data) {
        this.data = data;
    }

    @Override
    public double getAttributeValue(Attribute attribute) {
        return this.data.getDouble(attribute.id().asString(), 0.0);
    }
}
