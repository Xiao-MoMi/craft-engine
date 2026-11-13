package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.momirealms.craftengine.bedrock.BedrockHeldItemType;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserDyeProperties;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

final class GeyserItemDisplayEntity extends Entity implements Tickable {
    private final GeyserItemDisplays displays;
    private DisplayProperties carrier;
    private GeyserItemDisplays.Model model;
    private ItemData equipment = ItemData.AIR;
    private ItemData sentEquipment = ItemData.AIR;
    private BedrockHeldItemType heldItemType = BedrockHeldItemType.NATIVE;
    private BedrockHeldItemType sentHeldItemType;
    private int context;
    private int billboard;
    private final DisplayInterpolation transform = new DisplayInterpolation();
    private boolean dirty = true;
    private DisplayPose sent;
    private GeyserItemDisplays.Appearance sentAppearance;
    private int dyeColor = -1;
    private int sentDyeColor = -1;
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
        this.dirty = true;
        ItemData translated = stack == null || stack.getAmount() <= 0 ? ItemData.AIR : ItemTranslator.translateToBedrock(this.session, stack);
        this.model = this.displays.resolve(translated);
        this.dyeColor = this.model == null ? -1 : GeyserDyeProperties.color(GeyserItemStack.from(this.session, stack));
        this.equipment = ItemData.AIR;
        this.heldItemType = BedrockHeldItemType.NATIVE;
        if (this.model == null && translated != ItemData.AIR
                && translated.getDefinition().getIdentifier().startsWith("minecraft:")) {
            this.model = this.displays.vanilla(this.session, stack);
            if (this.model != null && this.model.appearance(0).shape() == 0) {
                this.equipment = translated;
                this.heldItemType = GeyserHeldItemTypes.resolve(translated, this.session.getItemMappings().getMapping(stack).isBlock());
            }
            if (this.model != null) {
                this.dyeColor = GeyserDyeProperties.color(GeyserItemStack.from(this.session, stack));
            }
        }
    }

    void context(byte value) {
        this.dirty = true;
        this.context = value;
    }

    void billboard(byte value) {
        this.dirty = true;
        this.billboard = value;
    }

    void startInterpolation(int delay) {
        this.transform.start(delay);
    }

    void duration(int ticks) {
        this.transform.duration(ticks);
    }

    void translation(Vector3f value) {
        this.transform.translation(value);
    }

    void scale(Vector3f value) {
        this.transform.scale(value);
    }

    void left(Quaternionf value) {
        this.transform.left(value);
    }

    void right(Quaternionf value) {
        this.transform.right(value);
    }

    void teleportDuration(int ticks) {
        this.teleportDuration = Math.clamp(ticks, 0, 59);
    }

    @Override
    public void updateBedrockMetadata() {
        this.updateDisplay();
        super.updateBedrockMetadata();
    }

    private void updateDisplay() {
        if (!this.dirty && !this.transform.dirty() && this.billboard == 0) {
            return;
        }
        this.dirty = false;
        this.updateEquipment(false);
        GeyserItemDisplays.Appearance appearance = this.model == null ? GeyserItemDisplays.Appearance.EMPTY : this.model.appearance(this.context);
        boolean changeCarrier = this.model != null && this.carrier != this.model.carrier();
        if (changeCarrier) {
            this.carrier = this.model.carrier();
            this.bedrockDefinition = Registries.BEDROCK_ENTITY_DEFINITIONS.get(this.carrier.id());
        }
        DisplayPose pose = this.transform.target();
        boolean interpolationChanged = this.transform.dirty() || changeCarrier || this.sent == null;
        float renderYaw = this.billboard == 1 || this.billboard == 3 ? this.session.getPlayerEntity().getYaw() + 180 : this.yaw;
        float renderPitch = this.billboard == 2 || this.billboard == 3 ? -this.session.getPlayerEntity().getPitch() : this.pitch;
        boolean heldItemChanged = changeCarrier || this.heldItemType != this.sentHeldItemType;
        if (!interpolationChanged && !changeCarrier && !heldItemChanged && pose.equals(this.sent) && appearance == this.sentAppearance && this.dyeColor == this.sentDyeColor && renderYaw == this.sentYaw && renderPitch == this.sentPitch) {
            return;
        }
        DisplayPose previous = changeCarrier ? null : this.sent;
        boolean modelChanged = changeCarrier || appearance != this.sentAppearance;
        boolean dyeChanged = changeCarrier || this.sent == null || this.dyeColor != this.sentDyeColor;
        int interpolation = interpolationChanged ? this.transform.consume(!this.valid || changeCarrier || this.sent == null) : -1;
        this.updatePropertiesBatched(batch -> {
            this.carrier.write(batch, pose, previous, appearance, modelChanged, this.dyeColor, dyeChanged, renderYaw, renderPitch, interpolation, this.transform.duration(), this.transform.delay());
            if (heldItemChanged) {
                this.carrier.heldItemType(batch, this.heldItemType);
            }
        }, false);
        this.sent = pose;
        this.sentAppearance = appearance;
        this.sentHeldItemType = this.heldItemType;
        this.sentDyeColor = this.dyeColor;
        this.sentYaw = renderYaw;
        this.sentPitch = renderPitch;
        if (changeCarrier && this.valid) {
            // 换资源包时客户端实体类型也要切换。保持 Java 实体、运行时 ID 和乘客关系。
            RemoveEntityPacket remove = new RemoveEntityPacket();
            remove.setUniqueEntityId(this.geyserId);
            this.session.sendUpstreamPacket(remove);
            super.spawnEntity();
            this.updateEquipment(true);
            if (this.vehicle != null) {
                this.link(this.vehicle, this);
            }
            for (Entity passenger : this.passengers) {
                this.link(this, passenger);
            }
        }
    }

    @Override
    public void spawnEntity() {
        this.dirty = true;
        this.updateDisplay();
        super.spawnEntity();
        if (this.equipment != ItemData.AIR) {
            this.updateEquipment(true);
        }
    }

    private void updateEquipment(boolean force) {
        if (!this.valid || (!force && this.equipment.equals(this.sentEquipment))) {
            return;
        }
        this.sentEquipment = this.equipment;
        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.geyserId);
        packet.setItem(this.equipment);
        packet.setInventorySlot(0);
        packet.setHotbarSlot(-1);
        packet.setContainerId(0);
        this.session.sendUpstreamPacket(packet);
    }

    private void link(Entity vehicle, Entity passenger) {
        SetEntityLinkPacket link = new SetEntityLinkPacket();
        link.setEntityLink(new EntityLinkData(
                vehicle.geyserId(),
                passenger.geyserId(),
                vehicle.getPassengers().indexOf(passenger) <= 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER,
                true,
                false,
                0
        ));
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
        this.dirty = true;
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
        packet.getMetadata().put(EntityDataTypes.WIDTH, this.getBoundingBoxWidth());
        packet.getMetadata().put(EntityDataTypes.HEIGHT, this.getBoundingBoxHeight());
        packet.setRotation(Vector2f.ZERO);
        packet.setHeadRotation(0);
        packet.setBodyRotation(0);
    }
}
