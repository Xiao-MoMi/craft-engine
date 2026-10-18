package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;

import java.io.IOException;
import java.nio.file.Path;

public interface BedrockTextureConverter {

    void convert(JavaResourcePack pack, Path output, BedrockTextureRegistry textures) throws IOException;
}
