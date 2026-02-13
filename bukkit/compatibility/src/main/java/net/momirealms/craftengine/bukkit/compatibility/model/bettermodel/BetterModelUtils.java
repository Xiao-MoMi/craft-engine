package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.entity.BaseEntity;
import org.bukkit.entity.Entity;

import java.util.Optional;

public final class BetterModelUtils {
    private BetterModelUtils() {}

    public static void bindModel(Entity base, String id) {
        Optional<ModelRenderer> renderer = BetterModel.model(id);
        if (renderer.isEmpty()) {
            throw new NullPointerException("Could not find BetterModel blueprint " + id);
        }
        renderer.get().create(BaseEntity.of(BukkitAdapter.adapt(base)));
    }
}
