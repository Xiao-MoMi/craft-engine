package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.nio.file.Path;

record VanillaTextureContext(JavaResourcePack pack, Path root, VanillaTextureRegistry textures) {

    boolean changedTexture(String source) {
        String path = ResourcePackUtils.vanillaTexturePath(source);
        return this.pack.hasCustomResource(path) || this.pack.hasCustomResource(path + ".mcmeta");
    }
}
