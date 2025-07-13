package net.momirealms.craftengine.core.plugin;

import com.google.gson.JsonElement;
import net.momirealms.sparrow.nbt.Tag;

public interface Platform {

    void dispatchCommand(String command);

    Object snbtToJava(String nbt);

    Tag jsonToSparrowNBT(JsonElement json);

    Tag snbtToSparrowNBT(String nbt);

    Tag javaToSparrowNBT(Object object);
}
