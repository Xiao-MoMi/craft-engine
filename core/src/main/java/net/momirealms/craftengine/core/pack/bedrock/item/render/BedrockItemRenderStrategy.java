package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BedrockItemRenderStrategy {
    static final String CHARGE_AMOUNT_SCRIPT = "variable.ce_charge_amount = variable.ce_using_item ? math.clamp((variable.ce_use_ticks + query.frame_alpha - 1) / 10, 0, 1) : 0;";
    static final BedrockItemRenderStrategy DEFAULT = new BedrockItemRenderStrategy();
    private static final List<BedrockItemRenderStrategy> DEFAULT_STRATEGIES = List.of(DEFAULT);

    static List<? extends BedrockItemRenderStrategy> forItem(ResolvedItemBinding entry) {
        if (entry.useBehavior().animation() == ItemBehaviorDescription.UseAnimation.EAT) {
            return DEFAULT_STRATEGIES;
        }
        return Registry.STRATEGIES.getOrDefault(entry.clientItemType(), DEFAULT_STRATEGIES);
    }

    static JsonArray useScripts(ItemBehaviorDescription.Use use) {
        return BedrockItemUseScripts.create(use.maximumDuration().value(), true);
    }

    boolean accepts(ItemBindingRule rule) {
        return true;
    }

    boolean isEquipmentCondition(ItemBindingCondition condition) {
        return BedrockItemAttachables.isUseCondition(condition);
    }

    boolean matchesIdle(ItemBindingRule rule) {
        return BedrockItemAttachables.matchesIdle(rule);
    }

    String stageCondition(ItemBindingRule rule) {
        return BedrockItemAttachables.useCondition(rule);
    }

    List<ItemBindingCondition> stateConditions() {
        return List.of();
    }

    Key variantId(Key id) {
        return id;
    }

    @Nullable
    GeneratedItemVariant tryWriteSpecialized(BedrockItemPresentation plan,
                                             JavaModelResources resources,
                                             BedrockItemIconRenderer renderer,
                                             BedrockItemGenerationContext context) throws IOException {
        return null;
    }

    BedrockAttachablePose pose(ResolvedItemBinding entry) throws IOException {
        ItemBehaviorDescription.Use use = entry.useBehavior();
        JsonArray scripts = useScripts(use);
        int duration = use.maximumDuration().value();
        if (use.animation() == ItemBehaviorDescription.UseAnimation.EAT && duration > 0) {
            return BedrockFoodAttachables.eatPose(scripts, duration);
        }
        return BedrockAttachablePose.withoutBones(scripts);
    }

    void registerHandModel(BedrockPlayerAnimations animations, Key id, ResolvedItemBinding entry) {
    }

    private static final class Registry {
        private static final Map<Key, List<? extends BedrockItemRenderStrategy>> STRATEGIES = new HashMap<>();

        static {
            STRATEGIES.put(ItemKeys.CROSSBOW, BedrockCrossbowRenderStrategy.STATES);
            STRATEGIES.put(ItemKeys.BOW, List.of(BedrockBowRenderStrategy.INSTANCE));
            STRATEGIES.put(ItemKeys.SHIELD, List.of(BedrockShieldRenderStrategy.INSTANCE));
            STRATEGIES.put(ItemKeys.TRIDENT, List.of(BedrockTridentRenderStrategy.INSTANCE));
            List<BedrockSpearRenderStrategy> spears = List.of(BedrockSpearRenderStrategy.INSTANCE);
            for (Key spear : ItemKeys.SPEARS) {
                STRATEGIES.put(spear, spears);
            }
        }
    }
}
