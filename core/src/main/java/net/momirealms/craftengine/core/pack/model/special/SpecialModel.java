package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.List;
import java.util.function.Function;

public interface SpecialModel extends Function<MinecraftVersion, JsonObject> {

    Key type();

    List<Revision> revisions();
}
