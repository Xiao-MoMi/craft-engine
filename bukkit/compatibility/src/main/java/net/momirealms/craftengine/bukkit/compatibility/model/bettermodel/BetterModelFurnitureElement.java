package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import kr.toxicity.model.api.tracker.TrackerUpdateAction;
import kr.toxicity.model.api.util.function.BonePredicate;
import net.momirealms.craftengine.bukkit.entity.furniture.element.AbstractConditionalFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataResolver;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class BetterModelFurnitureElement extends AbstractConditionalFurnitureElement {
    public final Furniture furniture;
    public final BetterModelFurnitureElementConfig config;
    public final Location location;
    private final List<TintLayer> tintLayers;
    private final FurnitureDataResolver<ModelProfile.Uncompleted> profileSource;
    private ModelProfile.Uncompleted profile;
    private DummyTracker dummyTracker;

    BetterModelFurnitureElement(Furniture furniture, BetterModelFurnitureElementConfig config) {
        super(config.predicate, config.hasCondition);
        this.furniture = furniture;
        this.config = config;
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, config.position);
        this.location = new Location((World) furniturePos.world.platformWorld(), position.x, position.y, position.z, furniturePos.yRot + config.yaw, furniturePos.xRot + config.pitch);
        this.tintLayers = new ArrayList<>(config.tintLayers.size());
        for (BetterModelTintLayerConfig layer : config.tintLayers) {
            this.tintLayers.add(new TintLayer(layer.source().bind(furniture), layer.bones()));
        }
        this.profileSource = SourceItemProfileDataSourceConfig.fromConfig().bind(furniture);
        this.resolveSourceState();
        this.dummyTracker = createDummyTracker(this.profile);
    }

    private @Nullable DummyTracker createDummyTracker(@Nullable ModelProfile.Uncompleted profile) {
        ModelRenderer modelRenderer = BetterModel.model(this.config.model).orElse(null);
        if (modelRenderer == null) return null;
        TrackerModifier modifier = TrackerModifier.builder()
                .sightTrace(this.config.sightTrace)
                .build();
        Consumer<DummyTracker> preUpdate = this.tintLayers.isEmpty()
                ? tracker -> {}
                : this::applyTintLayers;
        return profile == null
                ? modelRenderer.create(BukkitAdapter.adapt(this.location), modifier, preUpdate)
                : modelRenderer.create(BukkitAdapter.adapt(this.location), profile, modifier, preUpdate);
    }

    private void resolveSourceState() {
        for (TintLayer layer : this.tintLayers) {
            layer.resolve();
        }
        this.profile = this.profileSource.resolve();
    }

    private void applyTintLayers(DummyTracker tracker) {
        for (TintLayer layer : this.tintLayers) {
            Color color = layer.value;
            if (color != null) {
                tracker.update(TrackerUpdateAction.tint(Color.transparent(color.color())), layer.bones);
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
            this.resolveSourceState();
            this.dummyTracker = createDummyTracker(this.profile);
        }
    }

    @Override
    public void gatherInteractableEntityId(Consumer<Integer> collector) {
    }

    private static final class TintLayer {
        private final FurnitureDataResolver<Color> source;
        private final BonePredicate bones;
        private Color value;

        private TintLayer(
                FurnitureDataResolver<Color> source,
                BonePredicate bones
        ) {
            this.source = source;
            this.bones = bones;
        }

        private void resolve() {
            this.value = this.source.resolve();
        }
    }
}
