package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;

final class BedrockTridentRenderStrategy extends BedrockItemRenderStrategy {
    static final BedrockTridentRenderStrategy INSTANCE = new BedrockTridentRenderStrategy();

    private BedrockTridentRenderStrategy() {
    }

    @Override
    BedrockAttachablePose pose(ResolvedItemBinding entry) throws IOException {
        if (entry.useBehavior().animation() != ItemBehaviorDescription.UseAnimation.TRIDENT) {
            return super.pose(entry);
        }
        JsonObject source = PoseTemplate.TRIDENT;
        JsonArray scripts = useScripts(entry.useBehavior());
        scripts.addAll(source.getAsJsonArray("pre_animation"));
        return new BedrockAttachablePose(scripts, source.getAsJsonArray("bones"), source.getAsJsonObject("animations"), source.getAsJsonArray("animate"), BedrockAttachablePose.FIRST_PERSON_MAIN);
    }

    private static final class PoseTemplate {
        private static final JsonObject TRIDENT = GsonHelper.readJsonFromResource("/bedrock/item/trident/charge.pose.json").getAsJsonObject();
    }
}
