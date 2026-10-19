package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;

import java.io.IOException;

public interface BedrockTextureConverter {

    void convert(BedrockConversionContext context) throws IOException;
}
