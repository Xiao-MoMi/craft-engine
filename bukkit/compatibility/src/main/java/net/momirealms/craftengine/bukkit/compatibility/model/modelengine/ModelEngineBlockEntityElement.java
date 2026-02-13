package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.joml.Vector3f;

public final class ModelEngineBlockEntityElement implements BlockEntityElement {
    private Dummy<?> dummy;
    private final Location location;
    private final ModelEngineBlockEntityElementConfig config;

    public ModelEngineBlockEntityElement(World world, BlockPos pos, ModelEngineBlockEntityElementConfig config) {
        this.config = config;
        Vector3f position = config.position();
        this.location = new Location((org.bukkit.World) world.platformWorld(), pos.x() + position.x, pos.y() + position.y, pos.z() + position.z, config.yaw(), config.pitch());
        this.dummy = createDummy();
    }

    private Dummy<?> createDummy() {
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(config.model());
        if (activeModel == null) {
            return null;
        } else {
            Dummy<?> dummy = new Dummy<>();
            dummy.setLocation(this.location);
            dummy.setDetectingPlayers(false);
            ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(dummy);
            modeledEntity.addModel(activeModel, false);
            return dummy;
        }
    }

    @Override
    public void hide(Player player) {
        if (this.dummy != null) {
            this.dummy.setForceHidden((org.bukkit.entity.Player) player.platformPlayer(), true);
        }
    }

    @Override
    public void show(Player player) {
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
