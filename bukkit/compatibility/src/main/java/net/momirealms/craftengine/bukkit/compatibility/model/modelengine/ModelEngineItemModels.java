package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import net.momirealms.craftengine.bukkit.api.event.AsyncExternalItemModelsCollectEvent;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.pack.ExternalItemModel;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public final class ModelEngineItemModels implements Listener {
    private final Plugin owner;

    public ModelEngineItemModels(Plugin owner) {
        this.owner = owner;
    }

    @EventHandler
    public void collect(AsyncExternalItemModelsCollectEvent event) {
        if (!Config.bedrockEntities().uses("modelengine", BedrockModelIntegration.DISPLAY_ENTITY) || !this.owner.isEnabled()) {
            return;
        }
        Set<ExternalItemModel> models = new HashSet<>();
        for (ModelBlueprint blueprint : ModelEngineAPI.getAPI().getModelRegistry().getValues()) {
            Key id = Key.of("modelengine", blueprint.getName());
            if (Config.bedrockEntities().mode(id) != BedrockModelIntegration.DISPLAY_ENTITY) {
                continue;
            }
            for (BlueprintBone bone : blueprint.getFlatMap().values()) {
                if (!bone.isRenderer()) {
                    continue;
                }
                for (ItemStack stack : bone.getModelData().createItemStack()) {
                    ExternalItemModel model = ItemStackUtils.externalItemModel(stack);
                    if (model != null) {
                        models.add(new ExternalItemModel(model.baseItem(), model.itemModel(), id));
                    }
                }
            }
        }
        event.contribute(this.owner, models);
    }
}
