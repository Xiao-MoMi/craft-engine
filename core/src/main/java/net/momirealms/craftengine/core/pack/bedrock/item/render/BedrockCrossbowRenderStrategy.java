package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemDisplayBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTexture;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CrossBowPullingRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class BedrockCrossbowRenderStrategy extends BedrockItemRenderStrategy {
    static final List<BedrockCrossbowRenderStrategy> STATES = List.of(new BedrockCrossbowRenderStrategy("none"), new BedrockCrossbowRenderStrategy("arrow"), new BedrockCrossbowRenderStrategy("rocket"));
    private final String ammunition;
    private final boolean charged;
    private final List<ItemBindingCondition> stateConditions;
    private final JsonPrimitive value;

    private BedrockCrossbowRenderStrategy(String ammunition) {
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
        List<ItemBindingRule> rules = plan.rulesByContext().get(ItemDisplayContext.GUI);
        int idle = plan.sharedModelIdleIndex();
        if (idle < 0) {
            return null;
        }
        BedrockCrossbowStates.State state = new BedrockCrossbowStates.State(plan.id(), this.charged, rules, idle, plan.conditions());
        GeneratedItemVariant flat = writeFlat(plan.item(), state, resources.models(), renderer, context);
        return flat != null ? flat : write3D(plan.item(), state, resources, context);
    }

    @Override
    BedrockAttachablePose pose(ResolvedItemBinding entry) {
        return BedrockCrossbow3DAttachables.pose(BedrockCrossbowStates.useScripts(this.charged, entry.useBehavior()), entry.useBehavior(), this.charged);
    }

    @Override
    void registerHandModel(BedrockPlayerAnimations animations, Key id, ResolvedItemBinding entry) {
        animations.register3DCrossbow(id, this.charged, entry.chargeDuration().value());
    }

    private static GeneratedItemVariant writeFlat(ResolvedItemBinding entry, BedrockCrossbowStates.State state,
                                                  JavaModelResolver models, BedrockItemIconRenderer renderer, BedrockItemGenerationContext context) throws IOException {
        List<BedrockItemAttachables.Stage> stages = new ArrayList<>(state.rules().size());
        for (int i = 0; i < state.rules().size(); i++) {
            ItemBindingRule rule = state.rules().get(i);
            if (!rule.transformations().isEmpty()) {
                return null;
            }
            ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
            BedrockTexture image = renderer.render(target);
            if (image == null) {
                return null;
            }
            String texture = "textures/" + state.id().namespace() + '/' + state.id().value() + "/stage_" + i;
            stages.add(new BedrockItemAttachables.Stage(target.resolve(models), image.image(), texture, BedrockCrossbowStates.condition(rule), new Matrix4f()));
        }
        for (BedrockItemAttachables.Stage stage : stages) {
            ImageUtils.writePng(stage.image(), context.root().resolve(stage.texture() + ".png"));
        }
        BedrockCrossbow2DAttachables.write(state, stages, entry.useBehavior(), context.root());
        context.playerAnimations().register2DCrossbow(state.id(), state.charged(), entry.chargeDuration().value());
        return GeneratedItemVariant.icon(state.id(), state.conditions(), stages.get(state.idle()).texture(), true);
    }

    private static GeneratedItemVariant write3D(ResolvedItemBinding entry, BedrockCrossbowStates.State state,
                                                JavaModelResources resources, BedrockItemGenerationContext context) throws IOException {
        BedrockItemGeometryBuilder geometry = new BedrockItemGeometryBuilder(resources.models(), resources.textures());
        List<BedrockItem3DAttachables.Stage> stages = new ArrayList<>(state.rules().size());
        BedrockItemDisplayBlocks.DisplayBlock display;
        try {
            for (ItemBindingRule rule : state.rules()) {
                stages.add(new BedrockItem3DAttachables.Stage(geometry.build(rule), BedrockCrossbowStates.condition(rule)));
            }
            display = context.itemDisplayBlocks().write(state.id(), List.of(state.rules().get(state.idle())), resources.models(), resources.textures(), context.root());
        } catch (BedrockResourceException exception) {
            return null;
        }
        BedrockCrossbow3DAttachables.write(state.id(), stages, geometry.materials(), state, entry.useBehavior(), context.root());
        context.playerAnimations().register3DCrossbow(state.id(), state.charged(), entry.chargeDuration().value());
        return GeneratedItemVariant.block(state.id(), state.conditions(), display, true);
    }
}
