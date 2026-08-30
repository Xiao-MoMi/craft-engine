package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import kr.toxicity.model.api.profile.ModelProfile;
import net.momirealms.craftengine.bukkit.entity.furniture.element.AbstractConditionalFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class BetterModelFurnitureElement extends AbstractConditionalFurnitureElement {
    public final Furniture furniture;
    public final BetterModelFurnitureElementConfig config;
    public final Location location;
    private DummyTracker dummyTracker;
    private volatile ModelProfile dynamicProfile;

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
        TrackerModifier modifier = TrackerModifier.builder()
                .sightTrace(this.config.sightTrace)
                .build();
        return dynamicProfile == null
                ? modelRenderer.create(BukkitAdapter.adapt(this.location), modifier)
                : modelRenderer.create(BukkitAdapter.adapt(this.location), dynamicProfile, modifier);
    }

    /**
     * Replaces the profile used by this CE-owned tracker. CraftEngine remains
     * responsible for showing, hiding, activating and deactivating the tracker.
     */
    public synchronized void setProfile(ModelProfile profile) {
        this.dynamicProfile = profile;
        if (this.dummyTracker != null) {
            this.dummyTracker.close();
            this.dummyTracker = createDummyTracker();
            if (this.dummyTracker != null) {
                this.furniture.getTrackedBy().forEach(this::show);
            }
        }
    }

    @Override
    public @NotNull Furniture furniture() {
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
    public void update(Player player) {
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
