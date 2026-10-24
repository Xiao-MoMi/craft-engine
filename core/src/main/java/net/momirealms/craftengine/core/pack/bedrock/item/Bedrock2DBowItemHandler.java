package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemIcons.Variant;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import org.joml.Matrix4fc;

public final class Bedrock2DBowItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final BedrockItemIconRenderer renderer;

    public Bedrock2DBowItemHandler(JavaModelResolver models, BedrockItemIconRenderer renderer) {
        this.models = models;
        this.renderer = renderer;
    }

    @Override
    public boolean write(ItemRegistrationPlan.Entry entry,
                         BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = entry.definition().rules();
        int idle = BedrockItemAttachables.findBowIdle(rules);
        if (idle == -1) {
            return false;
        }
        ItemBindingRule idleRule = rules.get(idle);
        // 待机变换也影响背包和掉落显示，交给默认方块路径，不能只改手持模型。
        if (!idleRule.transformations().isEmpty()) {
            return false;
        }
        List<ItemBindingCondition> registrationConditions = idleRule
                .conditions()
                .stream()
                .filter(condition -> !BedrockItemAttachables.isUseCondition(condition))
                .toList();
        List<BedrockItemAttachables.Stage> stages = new ObjectArrayList<>(rules.size());
        for (int i = 0; i < rules.size(); i++) {
            ItemBindingRule rule = rules.get(i);
            Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
            try {
                BedrockModelTransform.uniformScale(transformation);
            } catch (UnsupportedOperationException exception) {
                return false;
            }
            ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
            BufferedImage image = this.renderer.render(target);
            if (image == null) {
                return false;
            }
            String texture = "textures/craftengine_icons/" + entry.bedrockId().namespace() + '/' + entry.bedrockId().value() + "_v" + i;
            stages.add(new BedrockItemAttachables.Stage(this.models.resolve(target.model()), image, texture, BedrockItemAttachables.bowCondition(rule), transformation));
        }
        // 全部阶段准备好以后才写入资源和单条注册映射，避免生成半套弓。
        for (BedrockItemAttachables.Stage stage : stages) {
            ImageUtils.writePng(stage.image(), context.root().resolve(stage.texture() + ".png"));
        }
        BedrockItemAttachables.writeBow(entry.bedrockId(), stages, idle, entry.source().clientBehavior().use().maximumDuration().value(), context.root());
        JsonObject icon = new JsonObject();
        icon.addProperty("textures", stages.get(idle).texture());
        context.icons().add(entry.bedrockId().asString(), icon);
        ItemBindingRule registrationRule = new ItemBindingRule(registrationConditions, idleRule.target(), idleRule.transformations(), idleRule.sourcePath());
        context.variants().put(entry.bedrockId(), List.of(new Variant(entry.bedrockId(), registrationRule, null)));
        context.attachables().add(entry.bedrockId());
        return true;
    }
}
