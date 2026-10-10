package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import net.momirealms.craftengine.core.block.entity.render.BlockEntityRenderOffset;
import net.momirealms.craftengine.core.block.entity.render.element.AbstractConstantBlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Optional;

public final class BetterModelBlockEntityElement extends AbstractConstantBlockEntityElement {
    private DummyTracker dummyTracker;
    private final Location location;
    private final BetterModelBlockEntityElementConfig config;

    public BetterModelBlockEntityElement(BetterModelBlockEntityElementConfig config, World world, BlockPos pos) {
        super(config.predicate, config.hasCondition);
        this.config = config;
        Vector3f position = config.position();
        double x = pos.x() + (double) position.x;
        double y = pos.y() + (double) position.y;
        double z = pos.z() + (double) position.z;
        if (config.offset.type() != BlockEntityRenderOffset.Type.NONE) {
            long offsetSeed = config.offset.seed(pos);
            x += config.offset.x(offsetSeed);
            y += config.offset.y(offsetSeed);
            z += config.offset.z(offsetSeed);
        }
        this.location = new Location((org.bukkit.World) world.platformWorld(), x, y, z, config.yaw(), config.pitch());
        this.dummyTracker = createDummyTracker();
    }

    private DummyTracker createDummyTracker() {
        Optional<ModelRenderer> modelRenderer = BetterModel.model(this.config.model());
        return modelRenderer.map(renderer -> renderer.create(BukkitAdapter.adapt(this.location),
                TrackerModifier.builder()
                .sightTrace(this.config.sightTrace())
                .build())
        ).orElse(null);
    }

    @Override
    public void hide(@NotNull Player player) {
        if (this.dummyTracker != null) {
            this.dummyTracker.remove(BukkitAdapter.adapt((org.bukkit.entity.Player) player.platformPlayer()));
        }
    }

    @Override
    public void showInternal(@NotNull Player player) {
        if (this.dummyTracker != null) {
            this.dummyTracker.spawn(BukkitAdapter.adapt((org.bukkit.entity.Player) player.platformPlayer()));
        }
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
}
