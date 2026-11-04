package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CrossBowPullingRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Matrix4f;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class BedrockCrossbowBehavior extends BedrockEquipmentBehavior {
    static final List<BedrockCrossbowBehavior> STATES = List.of(new BedrockCrossbowBehavior("none"), new BedrockCrossbowBehavior("arrow"), new BedrockCrossbowBehavior("rocket"));
    private final String ammunition;
    private final boolean charged;
    private final List<ItemBindingCondition> stateConditions;
    private final JsonPrimitive value;

    private BedrockCrossbowBehavior(String ammunition) {
        this.ammunition = ammunition;
        this.value = new JsonPrimitive(ammunition);
        this.charged = !ammunition.equals("none");
        this.stateConditions = List.of(new ItemBindingCondition.SelectMatch(ChargeTypeSelectProperty.INSTANCE, this.charged ? List.of(this.value) : List.of(new JsonPrimitive("arrow"), new JsonPrimitive("rocket")), !this.charged));
    }

    @Override
    Key variantId(Key id) {
        return Key.of(id.namespace(), id.value() + "_" + this.ammunition);
    }

    @Override
    List<ItemBindingCondition> stateConditions() {
        return this.stateConditions;
    }

    @Override
    boolean accepts(ItemBindingRule rule) {
        for (ItemBindingCondition condition : rule.conditions()) {
            if (condition instanceof ItemBindingCondition.SelectMatch select && select.property() instanceof ChargeTypeSelectProperty && !select.matches(this.value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    boolean isEquipmentCondition(ItemBindingCondition condition) {
        return super.isEquipmentCondition(condition)
                || condition instanceof ItemBindingCondition.NumericRange range && range.property() instanceof CrossBowPullingRangeDispatchProperty
                || condition instanceof ItemBindingCondition.SelectMatch select && select.property() instanceof ChargeTypeSelectProperty;
    }

    @Override
    boolean matchesIdle(ItemBindingRule rule) {
        return BedrockCrossbowStates.matchesIdle(rule);
    }

    @Override
    String stageCondition(ItemBindingRule rule) {
        return BedrockCrossbowStates.condition(rule);
    }

    @Override
    GeneratedItemVariant tryWriteSpecialized(BedrockItemPresentation plan, JavaModelResources resources, BedrockItemIconRenderer renderer,
                                     BedrockItemGenerationContext context) throws IOException {
        if (plan.explicitViews()) {
            return null;
        }
        List<ItemBindingRule> rules = plan.sharedRules();
        int idle = plan.sharedModelIdleIndex();
        if (idle < 0) {
            return null;
        }
        BedrockCrossbowStates.State state = new BedrockCrossbowStates.State(plan.id(), this.charged, rules, idle, plan.registration());
        GeneratedItemVariant flat = writeFlat(plan.item(), state, resources.models(), renderer, context);
        return flat != null ? flat : write3D(plan.item(), state, resources, context);
    }

    @Override
    BedrockEquipmentPose pose(ItemRegistrationPlan.Entry entry) {
        return Bedrock3DCrossbowAttachables.pose(BedrockCrossbowStates.useScripts(this.charged, entry.useBehavior()), entry.useBehavior(), this.charged);
    }

    @Override
    void registerHandModel(BedrockPlayerAnimations animations, Key id, ItemRegistrationPlan.Entry entry) {
        animations.register3DCrossbow(id, this.charged, entry.chargeDuration().value());
    }

    private static GeneratedItemVariant writeFlat(ItemRegistrationPlan.Entry entry, BedrockCrossbowStates.State state,
                                                  JavaModelResolver models, BedrockItemIconRenderer renderer, BedrockItemGenerationContext context) throws IOException {
        List<BedrockItemAttachables.Stage> stages = new ArrayList<>(state.rules().size());
        for (int i = 0; i < state.rules().size(); i++) {
            ItemBindingRule rule = state.rules().get(i);
            if (!rule.transformations().isEmpty()) {
                return null;
            }
            ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
            BufferedImage image = renderer.render(target);
            if (image == null) {
                return null;
            }
            String texture = "textures/" + state.id().namespace() + '/' + state.id().value() + "/stage_" + i;
            stages.add(new BedrockItemAttachables.Stage(target.resolve(models), image, texture, BedrockCrossbowStates.condition(rule), new Matrix4f()));
        }
        for (BedrockItemAttachables.Stage stage : stages) {
            ImageUtils.writePng(stage.image(), context.root().resolve(stage.texture() + ".png"));
        }
        Bedrock2DCrossbowAttachables.write(state, stages, entry.useBehavior(), context.root());
        context.playerAnimations().register2DCrossbow(state.id(), state.charged(), entry.chargeDuration().value());
        return GeneratedItemVariant.icon(state.id(), state.registration(), stages.get(state.idle()).texture(), true);
    }

    private static GeneratedItemVariant write3D(ItemRegistrationPlan.Entry entry, BedrockCrossbowStates.State state,
                                                JavaModelResources resources, BedrockItemGenerationContext context) throws IOException {
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(resources.models(), resources.textures());
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>(state.rules().size());
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (ItemBindingRule rule : state.rules()) {
                stages.add(geometry.createStage(rule, BedrockCrossbowStates.condition(rule)));
            }
            display = context.itemDisplayBlocks().write(state.id(), List.of(state.rules().get(state.idle())), resources.models(), resources.textures(), context.root());
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        Bedrock3DCrossbowAttachables.write(state.id(), stages, geometry.materials(), state, entry.useBehavior(), context.root());
        context.playerAnimations().register3DCrossbow(state.id(), state.charged(), entry.chargeDuration().value());
        return GeneratedItemVariant.block(state.id(), state.registration(), display, true);
    }
}
