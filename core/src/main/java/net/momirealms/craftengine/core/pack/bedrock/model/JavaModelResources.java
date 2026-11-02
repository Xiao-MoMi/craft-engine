package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;

public final class JavaModelResources {
    private final JavaResourcePack pack;
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public JavaModelResources(JavaResourcePack pack) {
        this.pack = pack;
        this.models = new JavaModelResolver(pack);
        this.textures = new JavaPackTextureSource(pack);
    }

    public JavaResourcePack pack() {
        return this.pack;
    }

    public JavaModelResolver models() {
        return this.models;
    }

    public JavaPackTextureSource textures() {
        return this.textures;
    }
}
