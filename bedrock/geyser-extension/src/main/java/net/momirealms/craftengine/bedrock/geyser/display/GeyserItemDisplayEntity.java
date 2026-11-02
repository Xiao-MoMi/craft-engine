package net.momirealms.craftengine.bedrock.geyser.display;

import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

final class GeyserItemDisplayEntity extends Entity implements Tickable {
    private final GeyserItemDisplays displays;
    private DisplayProperties carrier;
    private GeyserItemDisplays.Model model;
    private int context;
    private int billboard;
    private int duration;
    private long start;
    private DisplayPose previous = DisplayPose.IDENTITY;
    private DisplayPose target = DisplayPose.IDENTITY;
    private DisplayPose sent;
    private GeyserItemDisplays.Appearance sentAppearance;
    private float sentYaw;
    private float sentPitch;
    private int teleportDuration;
    private int moveStep;
    private Vector3f moveFrom;
    private Vector3f moveTo;
    private float yawFrom;
    private float pitchFrom;
    private float targetYaw;
    private float targetPitch;

    GeyserItemDisplayEntity(EntitySpawnContext context, GeyserItemDisplays displays, DisplayProperties carrier) {
        super(context);
        this.displays = displays;
        this.carrier = carrier;
        this.targetYaw = this.yaw;
        this.targetPitch = this.pitch;
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        this.setFlag(EntityFlag.HAS_GRAVITY, false);
        this.setFlag(EntityFlag.HAS_COLLISION, false);
    }

    void item(ItemStack stack) {
        this.model = this.displays.resolve(this.session, stack);
    }

    void context(byte value) {
        this.context = value;
    }

    void billboard(byte value) {
        this.billboard = value;
    }

    void startInterpolation(int delay) {
        long now = System.nanoTime();
        this.previous = this.sample(now);
        this.start = now + delay * 50000000L;
    }

    void duration(int ticks) {
        this.duration = Math.max(0, ticks);
    }

    void teleportDuration(int ticks) {
        this.teleportDuration = Math.clamp(ticks, 0, 59);
    }

    void translation(Vector3f value) {
        this.target = new DisplayPose(value, this.target.left(), this.target.scale(), this.target.right());
    }

    void scale(Vector3f value) {
        this.target = new DisplayPose(this.target.translation(), this.target.left(), value, this.target.right());
    }

    void left(Quaternionf value) {
        this.target = new DisplayPose(this.target.translation(), value, this.target.scale(), this.target.right());
    }

    void right(Quaternionf value) {
        this.target = new DisplayPose(this.target.translation(), this.target.left(), this.target.scale(), value);
    }

    private DisplayPose sample(long now) {
        if (this.duration == 0 || this.start == 0) {
            return this.target;
        }
        float fraction = (now - this.start) / (50000000f * this.duration);
        return fraction >= 1 ? this.target : (fraction <= 0 ? this.previous : this.previous.interpolate(this.target, fraction));
    }

    @Override
    public void updateBedrockMetadata() {
        this.updateDisplay();
        super.updateBedrockMetadata();
    }

    private void updateDisplay() {
        GeyserItemDisplays.Appearance appearance = this.model == null ? GeyserItemDisplays.Appearance.EMPTY : this.model.appearance(this.context);
        boolean changeCarrier = appearance.shape() != 0 && this.carrier != this.model.carrier();
        if (changeCarrier) {
            this.carrier = this.model.carrier();
            this.bedrockDefinition = Registries.BEDROCK_ENTITY_DEFINITIONS.get(this.carrier.id());
        }
        DisplayPose pose = this.sample(System.nanoTime());
        float renderYaw = this.billboard == 1 || this.billboard == 3 ? this.session.getPlayerEntity().getYaw() + 180 : this.yaw;
        float renderPitch = this.billboard == 2 || this.billboard == 3 ? -this.session.getPlayerEntity().getPitch() : this.pitch;
        if (!changeCarrier && pose.equals(this.sent) && appearance == this.sentAppearance && renderYaw == this.sentYaw && renderPitch == this.sentPitch) {
            return;
        }
        this.sent = pose;
        this.sentAppearance = appearance;
        this.sentYaw = renderYaw;
        this.sentPitch = renderPitch;
        this.updatePropertiesBatched(batch -> this.carrier.write(batch, pose, appearance, renderYaw, renderPitch), false);
        if (changeCarrier && this.valid) {
            // 换资源包时客户端实体类型也要切换。保持 Java 实体、运行时 ID 和乘客关系。
            RemoveEntityPacket remove = new RemoveEntityPacket();
            remove.setUniqueEntityId(this.geyserId);
            this.session.sendUpstreamPacket(remove);
            super.spawnEntity();
            if (this.vehicle != null) {
                this.link(this.vehicle, this);
            }
            for (Entity passenger : this.passengers) {
                this.link(this, passenger);
            }
        }
    }

    private void link(Entity vehicle, Entity passenger) {
        SetEntityLinkPacket link = new SetEntityLinkPacket();
        link.setEntityLink(new EntityLinkData(vehicle.geyserId(), passenger.geyserId(),
                vehicle.getPassengers().indexOf(passenger) <= 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER, true, false, 0));
        this.session.sendUpstreamPacket(link);
    }

    @Override
    public void tick() {
        if (this.moveTo != null) {
            float fraction = Math.min(1, ++this.moveStep / (float) Math.max(1, this.teleportDuration));
            this.move(GenericMath.lerp(this.moveFrom, this.moveTo, fraction),
                    this.yawFrom + GenericMath.wrapAngleDeg(this.targetYaw - this.yawFrom) * fraction,
                    this.pitchFrom + GenericMath.wrapAngleDeg(this.targetPitch - this.pitchFrom) * fraction, false);
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
            this.move(position, yaw, pitch, teleported);
        }
    }

    private void move(Vector3f position, float yaw, float pitch, boolean teleported) {
        super.moveAbsoluteRaw(position, yaw, pitch, yaw, this.onGround, teleported);
    }

    @Override
    public Vector3f bedrockRotation() {
        // 世界朝向由展示骨骼承担，避免实体自身的 yaw 重复作用于局部平移。
        return Vector3f.ZERO;
    }

    @Override
    public void addAdditionalSpawnData(AddEntityPacket packet) {
        super.addAdditionalSpawnData(packet);
        packet.setRotation(Vector2f.ZERO);
        packet.setHeadRotation(0);
        packet.setBodyRotation(0);
    }
}
