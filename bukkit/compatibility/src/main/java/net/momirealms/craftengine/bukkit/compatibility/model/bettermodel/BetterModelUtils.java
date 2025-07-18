package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import org.bukkit.entity.Entity;

public class BetterModelUtils {

    public static void bindModel(Entity base, String id) {
        ModelRenderer renderer = BetterModel.plugin().modelManager().renderer(id);
        if (renderer == null) {
            throw new NullPointerException("Could not find BetterModel blueprint " + id);
        }
        renderer.create(base);
    }
}
