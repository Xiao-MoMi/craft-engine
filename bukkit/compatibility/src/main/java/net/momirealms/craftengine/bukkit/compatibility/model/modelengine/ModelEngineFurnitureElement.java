package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import net.momirealms.craftengine.bukkit.entity.furniture.element.AbstractConditionalFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public final class ModelEngineFurnitureElement extends AbstractConditionalFurnitureElement {
    public final Furniture furniture;
    public final ModelEngineFurnitureElementConfig config;
    public final Location location;
    private Dummy<?> dummy;

    ModelEngineFurnitureElement(Furniture furniture, ModelEngineFurnitureElementConfig config) {
        super(config.predicate, config.hasCondition);
        this.furniture = furniture;
        this.config = config;
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, config.position);
        this.location = new Location((World) furniturePos.world.platformWorld(), position.x, position.y, position.z, furniturePos.yRot + config.yaw, furniturePos.xRot + config.pitch);
        this.dummy = createDummy();
    }

    private Dummy<?> createDummy() {
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(config.model);
        if (activeModel == null) return null;
        Dummy<?> dummy = new Dummy<>();
        dummy.syncLocation(this.location);
        dummy.setDetectingPlayers(false);
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(dummy);
        modeledEntity.addModel(activeModel, false);
        return dummy;
    }

    @Override
    public @NonNull Furniture furniture() {
        return this.furniture;
    }

    @Override
    public void showInternal(Player player) {
        if (this.dummy != null) {
            this.dummy.setForceViewing((org.bukkit.entity.Player) player.platformPlayer(), true);
        }
    }

    @Override
    public void hide(Player player) {
        if (this.dummy != null) {
            this.dummy.setForceHidden((org.bukkit.entity.Player) player.platformPlayer(), true);
        }
    }

    @Override
    public void refresh(Player player) {
        this.hide(player);
        this.show(player);
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

    @Override
    public void gatherInteractableEntityId(Consumer<Integer> collector) {
        if (this.dummy != null) {
            collector.accept(this.dummy.getEntityId());
        }
    }
}
