package net.momirealms.craftengine.bedrock.geyser.display;

import net.momirealms.craftengine.bedrock.geyser.item.GeyserDyeProperties;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.api.entity.property.BatchPropertyUpdater;
import org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.DISPLAY_DYE;

final class DisplayProperties {
    private final Identifier id;
    private GeyserIntEntityProperty shape;
    private GeyserDyeProperties dye;
    private Axes translation;
    private Axes left;
    private Axes scale;
    private Axes right;
    private Axes modelPosition;
    private Axes modelRotation;
    private Axes modelScale;
    private GeyserFloatEntityProperty pitch;
    private GeyserFloatEntityProperty yaw;

    DisplayProperties(Identifier id) {
        this.id = id;
    }

    Identifier id() {
        return this.id;
    }

    void register(GeyserDefineEntityPropertiesEvent event) {
        this.shape = event.registerIntegerProperty(this.id, Identifier.of("craftengine:shape"), 0, 1000000, 0);
        this.dye = GeyserDyeProperties.register(event, this.id, DISPLAY_DYE);
        this.translation = this.axes(event, "pos", 0);
        this.left = this.axes(event, "left", 0);
        this.scale = this.axes(event, "size", 1);
        this.right = this.axes(event, "right", 0);
        this.modelPosition = this.axes(event, "model_pos", 0);
        this.modelRotation = this.axes(event, "model_rot", 0);
        this.modelScale = this.axes(event, "model_size", 1);
        this.pitch = this.property(event, "pitch", 0);
        this.yaw = this.property(event, "yaw", 0);
    }

    private Axes axes(GeyserDefineEntityPropertiesEvent event, String name, float initial) {
        return new Axes(this.property(event, name + "_x", initial), this.property(event, name + "_y", initial), this.property(event, name + "_z", initial));
    }

    private GeyserFloatEntityProperty property(GeyserDefineEntityPropertiesEvent event, String name, float initial) {
        return event.registerFloatProperty(this.id, Identifier.of("craftengine:" + name), -1000000f, 1000000f, initial);
    }

    void write(BatchPropertyUpdater batch, DisplayPose pose, GeyserItemDisplays.Appearance model, int dyeColor, float yaw, float pitch) {
        batch.update(this.shape, model.shape());
        this.dye.write(batch, dyeColor);
        this.modelPosition.write(batch, model.translation());
        this.modelRotation.write(batch, model.rotation());
        this.modelScale.write(batch, model.scale());
        batch.update(this.yaw, yaw);
        batch.update(this.pitch, pitch);
        Vector3f position = pose.translation();
        this.translation.write(batch, Vector3f.from(position.getX(), position.getY(), -position.getZ()).mul(16));
        this.left.write(batch, DisplayPose.boneAngles(pose.left()));
        this.right.write(batch, DisplayPose.boneAngles(pose.right()));
        this.scale.write(batch, pose.scale().mul(pose.left().lengthSquared() * pose.right().lengthSquared()));
    }

    private record Axes(GeyserFloatEntityProperty x, GeyserFloatEntityProperty y, GeyserFloatEntityProperty z) {
        void write(BatchPropertyUpdater batch, Vector3f value) {
            batch.update(this.x, value.getX());
            batch.update(this.y, value.getY());
            batch.update(this.z, value.getZ());
        }
    }
}
