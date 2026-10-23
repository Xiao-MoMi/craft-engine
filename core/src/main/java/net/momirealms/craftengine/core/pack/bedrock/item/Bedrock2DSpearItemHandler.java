package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
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
    public boolean write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        BedrockSpearModels models = BedrockSpearModels.collect(entry);
        if (models == null) {
            return false;
        }
        for (ItemBindingRule rule : entry.definition().rules()) {
            if (!rule.transformations().isEmpty()) {
                return false;
            }
        }
        BufferedImage iconImage = this.renderer.render((ItemRenderTarget.Model) models.inventory().target());
        if (iconImage == null) {
            return false;
        }
        List<BufferedImage> hands = new ArrayList<>();
        for (ItemBindingRule rule : models.hands()) {
            BufferedImage image = this.renderer.render((ItemRenderTarget.Model) rule.target());
            if (image == null) {
                return false;
            }
            hands.add(image);
        }
        String path = "textures/craftengine_icons/" + entry.bedrockId().namespace() + '/' + entry.bedrockId().value();
        ImageUtils.writePng(iconImage, context.root().resolve(path + ".png"));
        JsonObject icon = new JsonObject();
        icon.addProperty("textures", path);
        context.icons().add(entry.icon(), icon);
        Bedrock2DSpearAttachables.write(entry, models, hands, context.root());
        context.variants().put(entry.bedrockId(), List.of(new BedrockItemIcons.Variant(entry.bedrockId(), entry.icon(), models.registration(), null)));
        context.attachables().add(entry.bedrockId());
        return true;
    }
}
