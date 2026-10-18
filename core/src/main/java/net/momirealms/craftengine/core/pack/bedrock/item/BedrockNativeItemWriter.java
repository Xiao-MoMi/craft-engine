package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class BedrockNativeItemWriter {
    private final JavaModelResolver resolver;
    private final BedrockTextureAtlas.TextureSource textures;
    private final BedrockItemIcons icons;
    private final BedrockBlockItems blocks;
    private final Path root;
    private final BedrockModelConverter converter = new BedrockModelConverter();

    public BedrockNativeItemWriter(JavaModelResolver resolver, BedrockTextureAtlas.TextureSource textures,
                                   BedrockItemIcons icons, BedrockBlockItems blocks, Path root) {
        this.resolver = resolver;
        this.textures = textures;
        this.icons = icons;
        this.blocks = blocks;
        this.root = root;
    }

    public Appearance write(JavaItemModelLeaf leaf, Key resource) throws IOException {
        return this.write(leaf, resource, false);
    }

    public Appearance writeBlock(JavaItemModelLeaf leaf, Key resource) throws IOException {
        return this.write(leaf, resource, true);
    }

    private Appearance write(JavaItemModelLeaf leaf, Key resource, boolean blockModel) throws IOException {
        JavaModel model = this.resolver.resolve(leaf.model());
        BedrockModelConverter.Result converted = this.converter.convert(model, "geometry." + resource.namespace() + '.' + resource.value(), this.textures, leaf.tints());
        List<BedrockAnimatedTextures.Track> tracks = BedrockAnimatedTextures.write(converted, resource, this.root, false);
        if (!blockModel && model.geometryKind() == JavaModel.GeometryKind.GENERATED && tracks.isEmpty()) {
            return new Appearance(this.icons.write(converted, resource, leaf.tints(), this.root), null);
        }
        return new Appearance(null, this.blocks.write(converted, resource, this.root, tracks));
    }

    public record Appearance(String icon, BedrockPackMappings.DisplayBlock displayBlock) {
    }
}
