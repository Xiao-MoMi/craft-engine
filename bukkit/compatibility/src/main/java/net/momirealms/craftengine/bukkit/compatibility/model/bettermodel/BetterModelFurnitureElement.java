package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import net.momirealms.craftengine.bukkit.entity.furniture.element.AbstractConditionalFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public final class BetterModelFurnitureElement extends AbstractConditionalFurnitureElement {
    public final Furniture furniture;
    public final BetterModelFurnitureElementConfig config;
    public final Location location;
    private DummyTracker dummyTracker;

    BetterModelFurnitureElement(Furniture furniture, BetterModelFurnitureElementConfig config) {
        super(config.predicate, config.hasCondition);
        this.furniture = furniture;
        this.config = config;
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, config.position);
        this.location = new Location((World) furniturePos.world.platformWorld(), position.x, position.y, position.z, furniturePos.yRot + config.yaw, furniturePos.xRot + config.pitch);
        this.dummyTracker = createDummyTracker();
    }

    private DummyTracker createDummyTracker() {
        ModelRenderer modelRenderer = BetterModel.model(this.config.model).orElse(null);
        if (modelRenderer == null) return null;
        return modelRenderer.create(
                BukkitAdapter.adapt(this.location),
                TrackerModifier.builder()
                        .sightTrace(this.config.sightTrace)
                        .build()
        );
    }

    @Override
    public @NonNull Furniture furniture() {
        return this.furniture;
    }

    @Override
    public void showInternal(Player player) {
        if (this.dummyTracker != null) {
            this.dummyTracker.spawn(BukkitAdapter.adapt((org.bukkit.entity.Player) player.platformPlayer()));
        }
    }

    @Override
    public void hide(Player player) {
        if (this.dummyTracker != null) {
            this.dummyTracker.remove(BukkitAdapter.adapt((org.bukkit.entity.Player) player.platformPlayer()));
        }
    }

    @Override
    public void refresh(Player player) {
        this.hide(player);
        this.show(player);
    }

    @Override
    public void deactivate() {
        if (this.dummyTracker != null) {
            this.dummyTracker.close();
            this.dummyTracker = null;
        }
    }

    @Override
    public void activate() {
        if (this.dummyTracker == null) {
            this.dummyTracker = createDummyTracker();
        }
    }

    @Override
    public void gatherInteractableEntityId(Consumer<Integer> collector) {
    }
}
