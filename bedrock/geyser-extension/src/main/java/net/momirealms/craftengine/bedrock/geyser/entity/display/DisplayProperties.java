package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.momirealms.craftengine.bedrock.geyser.item.GeyserDyeProperties;
import org.cloudburstmc.math.imaginary.Quaternionf;
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
    private Rotation left;
    private Axes scale;
    private Rotation right;
    private GeyserIntEntityProperty interpolation;
    private GeyserFloatEntityProperty duration;
    private GeyserFloatEntityProperty delay;
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
        this.left = new Rotation(this.axes(event, "left", 0), this.property(event, "left_w", 1));
        this.scale = this.axes(event, "size", 1);
        this.right = new Rotation(this.axes(event, "right", 0), this.property(event, "right_w", 1));
        this.interpolation = event.registerIntegerProperty(this.id, Identifier.of("craftengine:interpolation"), 0, 1000000, 0);
        this.duration = this.property(event, "duration", 0);
        this.delay = this.property(event, "delay", 0);
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

    void write(BatchPropertyUpdater batch, DisplayPose pose, DisplayPose previous,
               GeyserItemDisplays.Appearance model, boolean modelChanged, int dyeColor, boolean dyeChanged, float yaw, float pitch, int interpolation, float duration, float delay) {
        if (interpolation >= 0) {
            batch.update(this.interpolation, interpolation);
            batch.update(this.duration, duration);
            batch.update(this.delay, delay);
        }
        if (modelChanged) {
            batch.update(this.shape, model.shape());
            this.modelPosition.write(batch, model.translation());
            this.modelRotation.write(batch, model.rotation());
            this.modelScale.write(batch, model.scale());
        }
        if (dyeChanged) {
            this.dye.write(batch, dyeColor);
        }
        batch.update(this.yaw, yaw);
        batch.update(this.pitch, pitch);
        if (previous == null || !pose.translation().equals(previous.translation())) {
            Vector3f position = pose.translation();
            batch.update(this.translation.x(), position.getX() * 16);
            batch.update(this.translation.y(), position.getY() * 16);
            batch.update(this.translation.z(), -position.getZ() * 16);
        }
        boolean leftChanged = previous == null || !pose.left().equals(previous.left());
        boolean rightChanged = previous == null || !pose.right().equals(previous.right());
        if (leftChanged) {
            this.left.write(batch, pose.left());
        }
        if (rightChanged) {
            this.right.write(batch, pose.right());
        }
        if (leftChanged || rightChanged || !pose.scale().equals(previous.scale())) {
            this.scale.write(batch, pose.scale().mul(pose.left().lengthSquared() * pose.right().lengthSquared()));
        }
    }

    private record Rotation(Axes xyz, GeyserFloatEntityProperty w) {
        void write(BatchPropertyUpdater batch, Quaternionf value) {
            Quaternionf unit = value.lengthSquared() > 1e-12f ? value.normalize() : Quaternionf.IDENTITY;
            batch.update(this.xyz.x(), unit.getX());
            batch.update(this.xyz.y(), -unit.getY());
            batch.update(this.xyz.z(), -unit.getZ());
            batch.update(this.w, unit.getW());
        }
    }

    private record Axes(GeyserFloatEntityProperty x, GeyserFloatEntityProperty y, GeyserFloatEntityProperty z) {
        void write(BatchPropertyUpdater batch, Vector3f value) {
            batch.update(this.x, value.getX());
            batch.update(this.y, value.getY());
            batch.update(this.z, value.getZ());
        }
    }
}
