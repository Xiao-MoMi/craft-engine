package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitItemStack;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import net.momirealms.craftengine.bukkit.api.event.AsyncExternalItemModelsCollectEvent;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.pack.ExternalItemModel;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import net.momirealms.craftengine.core.plugin.config.Config;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

public final class BetterModelItemModels implements Listener {
    private final Plugin owner;

    public BetterModelItemModels(Plugin owner) {
        this.owner = owner;
    }

    @EventHandler
    public void collect(AsyncExternalItemModelsCollectEvent event) {
        if (Config.bedrockBetterModelIntegration() != BedrockModelIntegration.DISPLAY_ENTITY || !this.owner.isEnabled()) {
            return;
        }
        Set<ExternalItemModel> models = new HashSet<>();
        for (ModelRenderer renderer : BetterModel.models()) {
            try (Stream<RendererGroup> groups = renderer.flatten()) {
                Iterator<RendererGroup> iterator = groups.iterator();
                while (iterator.hasNext()) {
                    BukkitItemStack stack = (BukkitItemStack) iterator.next().getItemStack().itemStack();
                    ExternalItemModel model = ItemStackUtils.externalItemModel(stack.source());
                    if (model != null) {
                        models.add(model);
                    }
                }
            }
        }
        event.contribute(this.owner, models);
    }
}
