package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.plugin.config.Config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class BedrockVanillaTextures {
    private static final List<BedrockTextureConverter> CONVERTERS = List.of(
            new BedrockLayerTextures(),
            new BedrockLayoutTextures(),
            new BedrockAtlasTextures(),
            new BedrockParticleTextures(),
            new BedrockUiTextures(),
            new BedrockMappedTextures(VanillaTextureMappings.INSTANCE.mappings())
    );

    private BedrockVanillaTextures() {
    }

    public static void write(JavaResourcePack resources, Path root, BedrockTerrainAtlas atlas) throws IOException {
        VanillaTextureRegistry textures = new VanillaTextureRegistry();
        VanillaTextureWriter output = new VanillaTextureWriter(root, textures, Config.bedrockConvertVanillaUiTextures());
        VanillaTextureContext context = new VanillaTextureContext(resources, root, output);
        for (BedrockTextureConverter converter : CONVERTERS) {
            converter.convert(context);
        }
        textures.contributeTo(atlas);
    }
}
