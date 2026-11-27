package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import net.momirealms.craftengine.core.block.entity.render.BlockEntityRenderOffset;
import net.momirealms.craftengine.core.block.entity.render.element.AbstractConstantBlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class ModelEngineBlockEntityElement extends AbstractConstantBlockEntityElement {
    private Dummy<?> dummy;
    private final Location location;
    private final ModelEngineBlockEntityElementConfig config;

    public ModelEngineBlockEntityElement(ModelEngineBlockEntityElementConfig config, World world, BlockPos pos) {
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
        this.dummy = createDummy();
    }

    private Dummy<?> createDummy() {
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(config.model());
        if (activeModel == null) {
            return null;
        } else {
            Dummy<?> dummy = new Dummy<>();
            dummy.syncLocation(this.location);
            dummy.setDetectingPlayers(false);
            ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(dummy);
            modeledEntity.addModel(activeModel, false);
            return dummy;
        }
    }

    @Override
    public void hide(@NotNull Player player) {
        if (this.dummy != null) {
            this.dummy.setForceHidden((org.bukkit.entity.Player) player.platformPlayer(), true);
        }
    }

    @Override
    public void showInternal(@NotNull Player player) {
        if (this.dummy != null) {
            this.dummy.setForceViewing((org.bukkit.entity.Player) player.platformPlayer(), true);
        }
    }

    @Override
    public void deactivate() {
        if (this.dummy != null) {
            this.dummy.setRemoved(true);
            this.dummy = null;
        }
    }

    @Override
    public void activate() {
        if (this.dummy == null) {
            this.dummy = createDummy();
        }
    }
}
