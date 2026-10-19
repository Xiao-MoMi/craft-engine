package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockAnimatedTextures;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureSource;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.List;

public final class BedrockNativeItemWriter {
    private final JavaModelResolver resolver;
    private final TextureSource textures;
    private final BedrockItemIcons icons;
    private final BedrockBlockItems blocks;
    private final BedrockConversionContext context;
    private final BedrockModelConverter converter = new BedrockModelConverter();

    public BedrockNativeItemWriter(JavaModelResolver resolver, TextureSource textures,
                                   BedrockItemIcons icons, BedrockBlockItems blocks, BedrockConversionContext context) {
        this.resolver = resolver;
        this.textures = textures;
        this.icons = icons;
        this.blocks = blocks;
        this.context = context;
    }

    public Appearance write(BaseItemModel leaf, Key resource) throws IOException {
        return this.write(leaf, resource, false);
    }

    public Appearance writeBlock(BaseItemModel leaf, Key resource) throws IOException {
        return this.write(leaf, resource, true);
    }

    private Appearance write(BaseItemModel leaf, Key resource, boolean blockModel) throws IOException {
        JavaModel model = this.resolver.resolve(leaf.path());
        List<Integer> tints = BedrockItemRendering.tints(leaf);
        BedrockModelConverter.Result converted = this.converter.convert(model, "geometry." + resource.namespace() + '.' + resource.value(), this.textures, tints);
        List<BedrockAnimatedTextures.Track> tracks = BedrockAnimatedTextures.write(converted, resource, this.context.root(), false);
        if (!blockModel && model.geometryKind() == JavaModel.GeometryKind.GENERATED && tracks.isEmpty()) {
            return new Appearance(this.icons.write(converted, resource, tints), null);
        }
        return new Appearance(null, this.blocks.write(converted, resource, tracks));
    }

    public record Appearance(String icon, BedrockPackMappings.DisplayBlock displayBlock) {
    }
}
