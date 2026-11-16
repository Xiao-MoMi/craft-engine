package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

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
import java.util.function.BiFunction;

class BedrockEquipmentBehavior {
    static final BedrockEquipmentBehavior DEFAULT = new BedrockEquipmentBehavior();
    private static final List<BedrockEquipmentBehavior> DEFAULT_BEHAVIORS = List.of(DEFAULT);
    private static final Map<Key, BiFunction<ResolvedItemBinding, Boolean, List<? extends BedrockEquipmentBehavior>>> BEHAVIORS = new HashMap<>();

    static {
        BEHAVIORS.put(ItemKeys.CROSSBOW, (entry, separateWorldViews) -> BedrockCrossbowBehavior.STATES);
        BEHAVIORS.put(ItemKeys.BOW, (entry, separateWorldViews) -> List.of(BedrockBowBehavior.INSTANCE));
        BEHAVIORS.put(ItemKeys.SHIELD, (entry, separateWorldViews) -> List.of(BedrockShieldBehavior.INSTANCE));
        BEHAVIORS.put(ItemKeys.TRIDENT, (entry, separateWorldViews) -> List.of(BedrockTridentBehavior.INSTANCE));
        for (Key spear : ItemKeys.SPEARS) {
            BEHAVIORS.put(spear, (entry, separateWorldViews) -> List.of(BedrockSpearBehavior.INSTANCE));
        }
    }

    static List<? extends BedrockEquipmentBehavior> forItem(ResolvedItemBinding entry, boolean separateWorldViews) {
        BiFunction<ResolvedItemBinding, Boolean, List<? extends BedrockEquipmentBehavior>> behavior = BEHAVIORS.get(entry.clientItemType());
        return behavior != null ? behavior.apply(entry, separateWorldViews) : DEFAULT_BEHAVIORS;
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
    GeneratedItemVariant tryWriteSpecialized(BedrockItemPresentation plan, JavaModelResources resources, BedrockItemIconRenderer renderer,
                                     BedrockItemGenerationContext context) throws IOException {
        return null;
    }

    BedrockEquipmentPose pose(ResolvedItemBinding entry) throws IOException {
        return BedrockEquipmentPose.withoutBones(useScripts(entry.useBehavior()));
    }

    void registerHandModel(BedrockPlayerAnimations animations, Key id, ResolvedItemBinding entry) {
    }

    static JsonArray useScripts(ItemBehaviorDescription.Use use) {
        JsonArray scripts = new JsonArray();
        scripts.add("variable.ce_using_item = context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0;");
        scripts.add("variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;");
        scripts.add("variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, " + use.maximumDuration().value() + " - variable.ce_use_ticks) : 0;");
        return scripts;
    }
}
