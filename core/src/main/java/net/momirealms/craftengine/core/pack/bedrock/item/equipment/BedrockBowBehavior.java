package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Matrix4fc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class BedrockBowBehavior extends BedrockEquipmentBehavior {
    static final BedrockBowBehavior INSTANCE = new BedrockBowBehavior();

    private BedrockBowBehavior() {
    }

    @Override
    GeneratedItemVariant tryWriteSpecialized(BedrockItemPresentation plan,
                                             JavaModelResources resources,
                                             BedrockItemIconRenderer renderer,
                                             BedrockItemGenerationContext context) throws IOException {
        if (plan.explicitViews() || !BedrockItemPresentation.hasUseConditions(plan.sharedRules())) {
            return null;
        }
        int idle = plan.sharedModelIdleIndex();
        if (idle < 0) {
            return null;
        }
        GeneratedItemVariant flat = writeFlat(plan, idle, resources.models(), renderer, context);
        return flat != null ? flat : write3D(plan, idle, resources, context);
    }

    @Override
    BedrockEquipmentPose pose(ItemRegistrationPlan.Entry entry) {
        return Bedrock3DBowAttachables.bowPose(useScripts(entry.useBehavior()));
    }

    @Override
    void registerHandModel(BedrockPlayerAnimations animations, Key id, ItemRegistrationPlan.Entry entry) {
        animations.registerBowUsingJavaPose(id);
    }

    private static GeneratedItemVariant writeFlat(BedrockItemPresentation plan, int idle, JavaModelResolver models, BedrockItemIconRenderer renderer, BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = plan.sharedRules();
        ItemBindingRule idleRule = rules.get(idle);
        // 待机变换也影响背包和掉落显示，交给默认方块路径，不能只改手持模型。
        if (!idleRule.transformations().isEmpty()) {
            return null;
        }
        List<BedrockItemAttachables.Stage> stages = new ObjectArrayList<>(rules.size());
        for (int i = 0; i < rules.size(); i++) {
            ItemBindingRule rule = rules.get(i);
            Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
            try {
                BedrockModelTransform.uniformScale(transformation);
            } catch (UnsupportedOperationException exception) {
                return null;
            }
            ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
            BufferedImage image = renderer.render(target);
            if (image == null) {
                return null;
            }
            String texture = "textures/" + plan.id().namespace() + '/' + plan.id().value() + "/stage_" + i;
            stages.add(new BedrockItemAttachables.Stage(target.resolve(models), image, texture, BedrockItemAttachables.useCondition(rule), transformation));
        }
        // 全部阶段准备好以后才写入资源和单条注册映射，避免生成半套弓。
        for (BedrockItemAttachables.Stage stage : stages) {
            ImageUtils.writePng(stage.image(), context.root().resolve(stage.texture() + ".png"));
        }
        BedrockItemAttachables.writeBow(plan.id(), stages, idle, plan.item().useBehavior().maximumDuration().value(), context.root());
        context.playerAnimations().registerBow(plan.id());
        return GeneratedItemVariant.icon(plan.id(), plan.registration(), stages.get(idle).texture(), true);
    }

    private static GeneratedItemVariant write3D(BedrockItemPresentation plan, int idle, JavaModelResources resources, BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = plan.sharedRules();
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(resources.models(), resources.textures());
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(rules.size());
        ItemBindingRule idleRule = rules.get(idle);
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (ItemBindingRule rule : rules) {
                stages.add(geometry.createStage(rule, BedrockItemAttachables.useCondition(rule)));
            }
            // 背包和掉落使用待机 display，手持几何不缩到方块范围。
            display = context.itemDisplayBlocks().write(plan.id(), List.of(idleRule), resources.models(), resources.textures(), context.root());
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        Bedrock3DBowAttachables.write(plan.id(), stages, geometry.materials(), plan.item().useBehavior().maximumDuration().value(), context.root());
        context.playerAnimations().registerBowUsingJavaPose(plan.id());
        return GeneratedItemVariant.block(plan.id(), plan.registration(), display, true);
    }
}
