package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import java.io.IOException;

interface BedrockTextureConverter {

    void convert(VanillaTextureContext context) throws IOException;
}
