package net.momirealms.craftengine.core.pack.bedrock;

import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;

import java.nio.file.Path;

public record BedrockConversionContext(JavaResourcePack pack, Path root, BedrockTextureRegistry textures) {
}
