package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugShape;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugText;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.TextDisplayEntity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

final class GeyserTextDisplayEntity extends TextDisplayEntity implements Tickable {
    private static final AtomicLong IDS = new AtomicLong(Long.MIN_VALUE);
    private static final Color TRANSPARENT = new Color(0, true);
    private final GeyserPlayer player;
    private final DisplayTransform transform = new DisplayTransform();
    private final List<DebugText> shapes = new ArrayList<>();
    private String text = "";
    private DisplayTextLayout layout;
    private int lineWidth = 200;
    private int background = 0x40000000;
    private int opacity = 255;
    private int style;
    private int billboard;
    private float viewRange = 1;
    private boolean dirty = true;
    private boolean fallback;
    private boolean hidden;
    private Vector3f vehiclePosition;
    private int dimension = Integer.MIN_VALUE;
    private float cameraYaw;
    private float cameraPitch;
    private int teleportDuration;
    private int moveStep;
    private Vector3f moveFrom;
    private Vector3f moveTo;
    private float yawFrom;
    private float pitchFrom;
    private float targetYaw;
    private float targetPitch;

    GeyserTextDisplayEntity(EntitySpawnContext context, GeyserPlayer player) {
        super(context);
        this.player = player;
    }

    DisplayTransform transform() {
        return this.transform;
    }

    void teleportDuration(int ticks) {
        this.teleportDuration = Math.clamp(ticks, 0, 59);
    }

    void billboard(byte value) {
        this.billboard = value;
        this.dirty = true;
    }

    void viewRange(float value) {
        this.viewRange = Math.max(0, value);
        this.dirty = true;
    }

    void lineWidth(int value) {
        this.lineWidth = Math.max(1, value);
        this.layout = null;
        this.dirty = true;
    }

    void background(int value) {
        this.background = value;
        this.dirty = true;
    }

    void opacity(byte value) {
        this.opacity = Byte.toUnsignedInt(value);
        this.dirty = true;
    }

    void style(byte value) {
        this.style = value;
        this.dirty = true;
    }

    @Override
    public void setText(EntityMetadata<Component, ?> metadata) {
        super.setText(metadata);
        this.text = MessageTranslatorProxy.INSTANCE.convertMessage(metadata.getValue(), this.session.locale());
        this.layout = null;
        this.dirty = true;
    }

    @Override
    public void setTranslation(EntityMetadata<Vector3f, ?> metadata) {
        this.transform.translation(metadata.getValue());
        super.setTranslation(metadata);
    }

    @Override
    protected void setInvisible(boolean value) {
        super.setInvisible(value);
        this.hidden = value;
        this.dirty = true;
    }

    @Override
    public void setVehicle(Entity vehicle) {
        super.setVehicle(vehicle);
        this.dirty = true;
    }

    @Override
    public void spawnEntity() {
        this.metadata.put(EntityDataTypes.NAME, "");
        super.spawnEntity();
        this.dirty = true;
        this.updateDisplay();
    }

    @Override
    public void despawnEntity() {
        this.removeShapes(0);
        super.despawnEntity();
    }

    @Override
    public void updateBedrockMetadata() {
        this.updateDisplay();
        this.metadata.put(EntityDataTypes.NAME, this.fallback ? this.text : "");
        super.updateBedrockMetadata();
    }

    @Override
    public void tick() {
        if (this.moveTo != null) {
            float fraction = Math.min(1, ++this.moveStep / (float) Math.max(1, this.teleportDuration));
            super.moveAbsoluteRaw(GenericMath.lerp(this.moveFrom, this.moveTo, fraction),
                    this.yawFrom + GenericMath.wrapAngleDeg(this.targetYaw - this.yawFrom) * fraction,
                    this.pitchFrom + GenericMath.wrapAngleDeg(this.targetPitch - this.pitchFrom) * fraction,
                    this.targetYaw, this.onGround, false);
            this.dirty = true;
            if (fraction == 1) {
                this.moveTo = null;
            }
        }
        this.updateDisplay();
    }

    @Override
    public void moveRelativeRaw(double x, double y, double z, float yaw, float pitch, float headYaw, boolean grounded) {
        Vector3f base = this.moveTo == null ? this.position : this.moveTo;
        this.moveAbsoluteRaw(base.add(x, y, z), yaw, pitch, headYaw, grounded, false);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean grounded, boolean teleported) {
        this.setOnGround(grounded);
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        if (!teleported && this.teleportDuration > 0) {
            this.moveFrom = this.position;
            this.moveTo = position;
            this.yawFrom = this.yaw;
            this.pitchFrom = this.pitch;
            this.moveStep = 0;
        } else {
            this.moveTo = null;
            super.moveAbsoluteRaw(position, yaw, pitch, headYaw, grounded, teleported);
            this.dirty = true;
        }
    }

    private void updateDisplay() {
        if (!this.valid) {
            return;
        }
        int dimension = this.session.getBedrockDimension().bedrockId();
        Vector3f vehiclePosition = this.vehicle == null ? null : this.vehicle.getPosition();
        if (dimension != this.dimension || !java.util.Objects.equals(vehiclePosition, this.vehiclePosition)) {
            this.dirty = true;
            this.dimension = dimension;
            this.vehiclePosition = vehiclePosition;
        }
        float cameraYaw = this.session.getPlayerEntity().getYaw();
        float cameraPitch = this.session.getPlayerEntity().getPitch();
        if (!this.dirty && !this.transform.needsUpdate()
                && (this.billboard == 0 || (cameraYaw == this.cameraYaw && cameraPitch == this.cameraPitch))) {
            return;
        }
        this.cameraYaw = cameraYaw;
        this.cameraPitch = cameraPitch;
        this.dirty = false;
        DisplayPose pose = this.transform.sample();
        boolean fallback = this.vehicle != null && this.billboard != 0;
        if (fallback != this.fallback) {
            this.fallback = fallback;
            this.metadata.put(EntityDataTypes.NAME, fallback ? this.text : "");
            super.updateBedrockMetadata();
        }
        if (fallback || this.hidden || this.text.isEmpty() || this.viewRange == 0) {
            this.removeShapes(0);
            return;
        }
        if (this.layout == null) {
            this.layout = DisplayTextLayout.wrap(this.text, this.lineWidth);
        }
        float yaw = this.billboard == 1 || this.billboard == 3 ? cameraYaw + 180 : this.yaw;
        float pitch = this.billboard == 2 || this.billboard == 3 ? -cameraPitch : this.pitch;
        Quaternionf orientation = Quaternionf.fromAngleDegAxis(-yaw, Vector3f.UNIT_Y)
                .mul(Quaternionf.fromAngleDegAxis(pitch, Vector3f.UNIT_X));
        Vector3f base = this.vehicle == null ? this.position : this.vehiclePosition.up(this.vehicle.getBoundingBoxHeight());
        Vector3f anchor = base.add(orientation.rotate(pose.translation()));
        Quaternionf rotation = orientation.mul(pose.left()).mul(pose.right());
        float scale = (float) Math.sqrt(Math.abs(pose.scale().getX() * pose.scale().getY()))
                * pose.left().lengthSquared() * pose.right().lengthSquared();
        if (scale < 0.0001f) {
            this.removeShapes(0);
            return;
        }
        Vector3f angles = textRotation(rotation.normalize());
        Color background = new Color((this.style & 4) != 0 ? 0x40000000 : this.background, true);
        Color foreground = new Color(255, 255, 255, this.opacity < 26 ? 0 : this.opacity);
        int shapeIndex = 0;
        for (int index = 0; index < this.layout.lines().size(); index++) {
            DisplayTextLayout.Line line = this.layout.lines().get(index);
            float x = (this.style & 8) != 0 ? (line.width() - this.layout.width()) * 0.5f
                    : ((this.style & 16) != 0 ? (this.layout.width() - line.width()) * 0.5f : 0);
            float y = this.layout.lines().size() * 10 - 1 - index * 10 - 4.5f;
            Vector3f offset = Vector3f.from(x + 1, y, 0).mul(0.025f);
            Vector3f position = anchor.add(orientation.rotate(pose.left().rotate(pose.right().rotate(offset).mul(pose.scale()))));
            if ((this.style & 1) != 0) {
                Vector3f shadow = position.add(rotation.normalize().rotate(Vector3f.from(0.025f, -0.025f, 0.001f).mul(scale)));
                this.writeShape(shapeIndex++, line.text(), shadow, angles, scale, new Color(63, 63, 63, this.opacity), TRANSPARENT);
            }
            this.writeShape(shapeIndex++, line.text(), position, angles, scale, foreground, background);
        }
        this.removeShapes(shapeIndex);
    }

    private void writeShape(int index, String text, Vector3f position, Vector3f rotation, float scale, Color color, Color background) {
        DebugText previous = index < this.shapes.size() ? this.shapes.get(index) : null;
        DebugText shape = new DebugText();
        shape.setId(previous == null ? IDS.getAndIncrement() : previous.getId());
        shape.setDimension(this.session.getBedrockDimension().bedrockId());
        shape.setPosition(position);
        shape.setRotation(rotation);
        shape.setScale(scale);
        shape.setUseRotation(true);
        shape.setColor(color);
        shape.setBackgroundColor(background);
        shape.setText(text);
        shape.setDepthTest((this.style & 2) == 0);
        shape.setMaximumRenderDistance(this.viewRange * 64);
        if (previous == null) {
            this.shapes.add(shape);
        } else {
            if (shape.equals(previous)) {
                return;
            }
            if (shape.getDimension() != previous.getDimension()) {
                this.player.queueShape(new DebugShape(previous.getId(), previous.getDimension()));
                shape.setId(IDS.getAndIncrement());
            }
            this.shapes.set(index, shape);
        }
        this.player.queueShape(shape);
    }

    private static Vector3f textRotation(Quaternionf rotation) {
        Vector3f right = rotation.rotate(Vector3f.UNIT_X);
        Vector3f up = rotation.rotate(Vector3f.UNIT_Y);
        Vector3f normal = rotation.rotate(Vector3f.UNIT_Z);
        double sinYaw = Math.clamp(right.getZ(), -1, 1);
        double pitch;
        double yaw;
        double roll;
        // DebugText 的正面沿 -Z，且采用 XYZ 欧拉旋转，不能直接使用通用四元数欧拉角。
        if (Math.abs(sinYaw) < 0.9999) {
            pitch = Math.toDegrees(Math.atan2(up.getZ(), -normal.getZ()));
            yaw = Math.toDegrees(Math.asin(sinYaw));
            roll = Math.toDegrees(Math.atan2(-right.getY(), -right.getX()));
        } else {
            pitch = Math.toDegrees(Math.atan2(normal.getY(), up.getY()));
            yaw = Math.copySign(90, sinYaw);
            roll = 0;
        }
        if (Math.abs(pitch) > 90) {
            pitch = GenericMath.wrapAngleDeg((float) (pitch + 180));
            yaw = GenericMath.wrapAngleDeg((float) (180 - yaw));
            roll = GenericMath.wrapAngleDeg((float) (roll + 180));
        }
        return Vector3f.from(pitch, yaw, roll);
    }

    private void removeShapes(int remaining) {
        for (int index = this.shapes.size() - 1; index >= remaining; index--) {
            DebugText shape = this.shapes.remove(index);
            this.player.queueShape(new DebugShape(shape.getId(), shape.getDimension()));
        }
    }
}
