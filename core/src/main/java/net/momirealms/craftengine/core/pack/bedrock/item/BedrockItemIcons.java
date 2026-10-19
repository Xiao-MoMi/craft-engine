package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaItemIconRenderer;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.PngOptimizer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BedrockItemIcons {
    private final JavaItemIconRenderer renderer;
    private final BedrockConversionContext context;

    public BedrockItemIcons(int size, BedrockConversionContext context) {
        this.context = context;
        this.renderer = new JavaItemIconRenderer(size);
    }

    public String write(BedrockModelConverter.Result model, Key resource, List<Integer> tints) throws IOException {
        String icon = resource.asString();
        String texture = "textures/craftengine_icons/" + resource.namespace() + '/' + resource.value();
        Path path = this.context.root().resolve(texture + ".png");
        Files.createDirectories(path.getParent());
        BufferedImage image = this.renderer.render(model.source(), model.atlas(), tints);
        try (OutputStream output = Files.newOutputStream(path)) {
            new PngOptimizer(image, 0).write(output);
        }
        this.context.textures().item(icon, texture);
        return icon;
    }

}
