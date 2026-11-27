package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.block.JavaBlockModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;

public final class JavaModelResources {
    private final JavaResourcePack pack;
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;
    private final JavaBlockModelResolver blockModels;

    public JavaModelResources(JavaResourcePack pack) {
        this.pack = pack;
        this.models = new JavaModelResolver(pack);
        this.textures = new JavaPackTextureSource(pack);
        this.blockModels = new JavaBlockModelResolver(this);
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

    public JavaBlockModelResolver blockModels() {
        return this.blockModels;
    }
}
