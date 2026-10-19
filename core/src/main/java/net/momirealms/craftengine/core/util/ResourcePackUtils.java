package net.momirealms.craftengine.core.util;

public final class ResourcePackUtils {

    private ResourcePackUtils() {
    }

    public static String vanillaTexturePath(String texture) {
        return "assets/minecraft/textures/" + texture;
    }

    public static String itemDefinitionPath(Key key) {
        return "assets/" + key.namespace() + "/items/" + key.value() + ".json";
    }
}
