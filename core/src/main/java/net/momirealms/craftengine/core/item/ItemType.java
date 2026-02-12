package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Optional;

public interface ItemType {

    Key id();

    Object getExactComponent(Object type);

    <T> Optional<T> getJavaComponent(Object type);

    Optional<JsonElement> getJsonComponent(Object type);

    Optional<Object> getNBTComponent(Object type);

    Optional<Tag> getSparrowNBTComponent(Object type);
}
