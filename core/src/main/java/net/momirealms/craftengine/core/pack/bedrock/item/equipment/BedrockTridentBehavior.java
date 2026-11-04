package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;

final class BedrockTridentBehavior extends BedrockEquipmentBehavior {
    static final BedrockTridentBehavior INSTANCE = new BedrockTridentBehavior();

    private BedrockTridentBehavior() {
    }

    @Override
    BedrockEquipmentPose pose(ItemRegistrationPlan.Entry entry) throws IOException {
        if (entry.useBehavior().animation() != ItemBehaviorDescription.UseAnimation.TRIDENT) {
            return super.pose(entry);
        }
        JsonObject source = GsonHelper.readJsonFromResource("/bedrock/item/trident/charge.pose.json").getAsJsonObject();
        JsonArray scripts = useScripts(entry.useBehavior());
        scripts.addAll(source.getAsJsonArray("pre_animation"));
        return new BedrockEquipmentPose(scripts, source.getAsJsonArray("bones"), source.getAsJsonObject("animations"), source.getAsJsonArray("animate"), BedrockEquipmentPose.FIRST_PERSON_MAIN);
    }
}
