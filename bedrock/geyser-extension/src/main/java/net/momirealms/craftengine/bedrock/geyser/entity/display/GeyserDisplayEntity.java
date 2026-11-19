package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.momirealms.craftengine.bedrock.BedrockDisplayStyle;
import net.momirealms.craftengine.bedrock.BedrockHeldItemType;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserDyeProperties;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
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
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

final class GeyserDisplayEntity extends Entity implements Tickable {
    private final GeyserItemDisplays displays;
    private final GeyserHeldItemTypes heldItemTypes;
    private DisplayProperties carrier;
    private GeyserItemDisplays.Model model;
    private ItemData equipment = ItemData.AIR;
    private ItemData sentEquipment = ItemData.AIR;
    private BedrockHeldItemType heldItemType = BedrockHeldItemType.NATIVE;
    private int context;
    private int billboard;
    private boolean hidden;
    private final DisplayInterpolation transform = new DisplayInterpolation();
    private boolean appearanceDirty = true;
    private boolean orientationDirty;
    private DisplayPose sent;
    private GeyserItemDisplays.Appearance sentAppearance;
    private GeyserItemStack displayItem;
    private int sentDyeColor = -1;
    private int sentStyle;
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

    GeyserDisplayEntity(EntitySpawnContext context, GeyserItemDisplays displays, DisplayProperties carrier) {
        super(context);
        this.displays = displays;
        this.heldItemTypes = displays.heldItemTypes(this.session.protocolVersion());
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
        this.appearanceDirty = true;
        ItemData translated = stack == null || stack.getAmount() <= 0 ? ItemData.AIR : ItemTranslator.translateToBedrock(this.session, stack);
        ItemDefinition definition = translated.getDefinition();
        String identifier = definition.getIdentifier();
        this.model = this.displays.resolve(identifier);
        this.displayItem = GeyserItemStack.from(this.session, stack);
        this.equipment = ItemData.AIR;
        this.heldItemType = BedrockHeldItemType.NATIVE;
        if (this.model == null && translated != ItemData.AIR && identifier.startsWith("minecraft:")) {
            ItemMapping mapping = this.session.getItemMappings().getMapping(stack);
            this.model = this.displays.vanilla(mapping);
            if (this.model != null && this.model.appearance(0).shape() == 0) {
                this.equipment = translated;
                this.heldItemType = this.heldItemTypes.resolve(definition, mapping.isBlock());
            }
        }
    }

    void context(byte value) {
        if (this.context != value) {
            this.context = value;
            this.appearanceDirty = true;
        }
    }

    void block(int stateId) {
        this.appearanceDirty = true;
        this.context = 0;
        this.displayItem = null;
        this.equipment = ItemData.AIR;
        this.heldItemType = BedrockHeldItemType.NATIVE;
        BlockState state = BlockState.of(stateId);
        this.model = this.displays.block(state.toString());
        if (this.model == null) {
            Item item = state.block().asItem();
            if (item.javaId() != 0) {
                ItemStack stack = GeyserItemStack.of(this.session, item.javaId(), 1).getItemStack();
                this.equipment = ItemTranslator.translateToBedrock(this.session, stack);
                this.heldItemType = this.heldItemTypes.resolve(this.equipment.getDefinition(), this.session.getItemMappings().getMapping(stack).isBlock());
                this.model = this.displays.heldBlock();
            }
        }
    }

    void billboard(byte value) {
        int mode = value >= 0 && value <= 3 ? value : 0;
        if (this.billboard != mode) {
            this.billboard = mode;
            this.orientationDirty = true;
        }
    }

    void viewRange(float value) {
        boolean hidden = value == 0;
        if (this.hidden != hidden) {
            this.hidden = hidden;
            this.orientationDirty = true;
        }
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
        this.updateDisplay(false);
        super.updateBedrockMetadata();
    }

    private void updateDisplay(boolean spawning) {
        boolean appearanceDirty = this.appearanceDirty;
        if (!spawning && !appearanceDirty && !this.orientationDirty && !this.transform.dirty()) {
            return;
        }
        this.appearanceDirty = false;
        this.orientationDirty = false;
        boolean changeCarrier = this.model != null && this.carrier != this.model.carrier();
        if (changeCarrier) {
            this.carrier = this.model.carrier();
            this.bedrockDefinition = Registries.BEDROCK_ENTITY_DEFINITIONS.get(this.carrier.id());
        }
        if (!spawning && appearanceDirty && !changeCarrier) {
            this.updateEquipment();
        }
        GeyserItemDisplays.Appearance appearance = appearanceDirty
                ? (this.model == null ? GeyserItemDisplays.Appearance.EMPTY : this.model.appearance(this.context))
                : this.sentAppearance;
        int dyeColor = appearanceDirty
                ? (this.model == null ? -1 : GeyserDyeProperties.color(this.displayItem, appearance.customModelDataColorIndex()))
                : this.sentDyeColor;
        DisplayPose pose = this.transform.target();
        boolean fullUpdate = spawning || changeCarrier || this.sent == null;
        boolean modelChanged = fullUpdate || appearance != this.sentAppearance;
        boolean dyeChanged = fullUpdate || dyeColor != this.sentDyeColor;
        boolean interpolationChanged = fullUpdate || this.transform.dirty();
        boolean orientationChanged = fullUpdate || this.yaw != this.sentYaw || this.pitch != this.sentPitch;
        int style = BedrockDisplayStyle.pack(this.billboard, this.hidden, this.heldItemType);
        boolean styleChanged = fullUpdate || style != this.sentStyle;
        if (!modelChanged && !dyeChanged && !interpolationChanged && !orientationChanged && !styleChanged) {
            return;
        }
        DisplayPose previous = fullUpdate ? null : this.sent;
        int interpolation = interpolationChanged ? this.transform.consume(!this.valid || fullUpdate) : -1;
        this.updatePropertiesBatched(batch -> {
            this.carrier.write(batch, pose, previous, appearance, modelChanged, dyeColor, dyeChanged, interpolation, this.transform.duration(), this.transform.delay());
            if (orientationChanged) {
                this.carrier.orientation(batch, this.yaw, this.pitch);
            }
            if (styleChanged) {
                this.carrier.style(batch, style);
            }
        }, false);
        this.sent = pose;
        this.sentAppearance = appearance;
        this.sentStyle = style;
        this.sentDyeColor = dyeColor;
        this.sentYaw = this.yaw;
        this.sentPitch = this.pitch;
        if (changeCarrier && this.valid && !spawning) {
            // 新载体生成后才发装备，保留 Java 实体、运行时 ID 和乘客关系
            RemoveEntityPacket remove = new RemoveEntityPacket();
            remove.setUniqueEntityId(this.geyserId);
            this.session.sendUpstreamPacket(remove);
            this.spawnCarrier();
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
        this.updateDisplay(true);
        this.spawnCarrier();
    }

    private void spawnCarrier() {
        super.spawnEntity();
        this.sentEquipment = ItemData.AIR;
        this.updateEquipment();
    }

    private void updateEquipment() {
        if (!this.valid || this.equipment == this.sentEquipment || this.equipment.equals(this.sentEquipment)) {
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
        this.updateDisplay(false);
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
        this.orientationDirty |= yaw != this.yaw || pitch != this.pitch;
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
