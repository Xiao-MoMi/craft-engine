package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Bedrock2DSpearItemHandler implements BedrockItemHandler {
    private final BedrockItemIconRenderer renderer;

    public Bedrock2DSpearItemHandler(BedrockItemIconRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        BedrockHeldItemModels models = BedrockHeldItemModels.collect(entry);
        if (models == null) {
            return null;
        }
        for (ItemBindingRule rule : entry.definition().rules()) {
            if (!rule.transformations().isEmpty()) {
                return null;
            }
        }
        BufferedImage iconImage = this.renderer.render((ItemRenderTarget.Model) models.inventory().target());
        if (iconImage == null) {
            return null;
        }
        List<BufferedImage> hands = new ArrayList<>();
        for (ItemBindingRule rule : models.hands()) {
            BufferedImage image = this.renderer.render((ItemRenderTarget.Model) rule.target());
            if (image == null) {
                return null;
            }
            hands.add(image);
        }
        String path = "textures/" + entry.bedrockId().namespace() + '/' + entry.bedrockId().value() + "/icon";
        ImageUtils.writePng(iconImage, context.root().resolve(path + ".png"));
        Bedrock2DSpearAttachables.write(entry, models, hands, context.root());
        return List.of(GeneratedItemVariant.icon(entry.bedrockId(), models.registration(), path, true));
    }
}
