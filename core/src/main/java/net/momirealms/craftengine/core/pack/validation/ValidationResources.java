package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.atlas.SimplifiedModelFile;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Set;

public record ValidationResources(JsonObject blockAtlas,
                                  JsonObject itemAtlas,
                                  Map<Key, SimplifiedModelFile> models,
                                  Set<Key> modelIds,
                                  Set<Key> textures,
                                  Set<Key> sounds) {
}