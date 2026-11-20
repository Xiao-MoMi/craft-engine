package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.momirealms.craftengine.bedrock.BedrockDisplayStyle;
import net.momirealms.craftengine.bedrock.BedrockHeldItemType;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserModelDisplayManager.Appearance;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserModelDisplayManager.Model;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemTints;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import static net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserModelDisplayManager.DEFAULT_DISPLAY_CONTEXT;

final class GeyserModelDisplayEntity extends Entity implements Tickable {
    private final GeyserModelDisplayManager displays;
    private final GeyserHeldItemTypes heldItemTypes;
    private final ModelDisplayInterpolation interpolation = new ModelDisplayInterpolation();

    private ModelDisplayProperties carrier;
    private Model model;
    private String itemIdentifier;
    private GeyserItemStack displayItem;
    private ItemData equipment = ItemData.AIR;
    private BedrockHeldItemType heldItemType = BedrockHeldItemType.NATIVE;
    private int displayContext;
    private int billboard;
    private boolean hidden;
    private boolean appearanceDirty = true;
    private boolean orientationDirty;

    private DisplayPose sentPose;
    private Appearance sentAppearance;
    private ItemData sentEquipment = ItemData.AIR;
    private int sentDyeColor = -1;
    private int sentStyle;
    private float sentYaw;
    private float sentPitch;

    private int teleportDuration;
    private int movementTicks;
    private Vector3f renderPosition;
    private float renderYaw;
    private float renderPitch;
    private Vector3f seatOffset;

    GeyserModelDisplayEntity(EntitySpawnContext context, GeyserModelDisplayManager displays, ModelDisplayProperties carrier) {
        super(context);
        this.displays = displays;
        this.heldItemTypes = displays.heldItemTypes(this.session.protocolVersion());
        this.carrier = carrier;
        this.renderPosition = this.position;
        this.renderYaw = this.yaw;
        this.renderPitch = this.pitch;
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
        this.itemIdentifier = identifier;
        this.model = this.displays.resolve(identifier, this.session, this.entityId);
        this.displayItem = GeyserItemStack.from(this.session, stack);
        this.clearHeldEquipment();
        if (this.model != null || translated == ItemData.AIR || !identifier.startsWith("minecraft:")) {
            return;
        }
        ItemMapping mapping = this.session.getItemMappings().getMapping(stack);
        this.model = this.displays.vanilla(mapping);
        if (this.model != null && this.model.appearance(DEFAULT_DISPLAY_CONTEXT).shape() == 0) {
            this.setHeldEquipment(translated, mapping);
        }
    }

    void refreshModel() {
        if (this.itemIdentifier != null) {
            Model model = this.displays.resolve(this.itemIdentifier, this.session, this.entityId);
            if (model != null && model != this.model) {
                this.model = model;
                this.appearanceDirty = true;
                this.updateBedrockMetadata();
            }
        }
    }

    void context(byte value) {
        if (this.displayContext != value) {
            this.displayContext = value;
            this.appearanceDirty = true;
        }
    }

    void block(int stateId) {
        this.itemIdentifier = null;
        this.appearanceDirty = true;
        this.displayContext = DEFAULT_DISPLAY_CONTEXT;
        this.displayItem = null;
        this.clearHeldEquipment();
        BlockState state = BlockState.of(stateId);
        this.model = this.displays.block(state.toString());
        if (this.model != null) {
            return;
        }
        int itemId = state.block().asItem().javaId();
        if (itemId == 0) {
            return;
        }
        ItemStack stack = GeyserItemStack.of(this.session, itemId, 1).getItemStack();
        ItemData translated = ItemTranslator.translateToBedrock(this.session, stack);
        this.setHeldEquipment(translated, this.session.getItemMappings().getMapping(stack));
        this.model = this.displays.heldBlock();
    }

    private void clearHeldEquipment() {
        this.equipment = ItemData.AIR;
        this.heldItemType = BedrockHeldItemType.NATIVE;
    }

    private void setHeldEquipment(ItemData equipment, ItemMapping mapping) {
        this.equipment = equipment;
        this.heldItemType = this.heldItemTypes.resolve(equipment.getDefinition(), mapping.isBlock());
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
        this.interpolation.start(delay);
    }

    void duration(int ticks) {
        this.interpolation.duration(ticks);
    }

    void translation(Vector3f value) {
        this.interpolation.translation(value);
    }

    void scale(Vector3f value) {
        this.interpolation.scale(value);
    }

    void left(Quaternionf value) {
        this.interpolation.left(value);
    }

    void right(Quaternionf value) {
        this.interpolation.right(value);
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
        if (!spawning && !appearanceDirty && !this.orientationDirty && !this.interpolation.dirty()) {
            return;
        }
        this.appearanceDirty = false;
        this.orientationDirty = false;
        boolean carrierChanged = this.updateCarrierDefinition();
        if (!spawning && appearanceDirty && !carrierChanged) {
            this.updateEquipment();
        }
        Appearance appearance;
        int dyeColor;
        if (!appearanceDirty) {
            appearance = this.sentAppearance;
            dyeColor = this.sentDyeColor;
        } else if (this.model == null) {
            appearance = Appearance.EMPTY;
            dyeColor = -1;
        } else {
            appearance = this.model.appearance(this.displayContext);
            dyeColor = GeyserItemTints.color(this.displayItem, appearance.tintSource());
        }

        DisplayPose pose = this.interpolation.target();
        boolean fullUpdate = spawning || carrierChanged || this.sentPose == null;
        boolean appearanceChanged = fullUpdate || appearance != this.sentAppearance;
        boolean dyeChanged = fullUpdate || dyeColor != this.sentDyeColor;
        boolean interpolationChanged = fullUpdate || this.interpolation.dirty();
        boolean orientationChanged = fullUpdate || this.renderYaw != this.sentYaw || this.renderPitch != this.sentPitch;
        int style = BedrockDisplayStyle.pack(this.billboard, this.hidden, this.heldItemType);
        boolean styleChanged = fullUpdate || style != this.sentStyle;
        if (!appearanceChanged && !dyeChanged && !interpolationChanged && !orientationChanged && !styleChanged) {
            return;
        }

        DisplayPose previousPose = fullUpdate ? null : this.sentPose;
        int interpolationSequence = interpolationChanged ? this.interpolation.consume(!this.valid || fullUpdate) : -1;
        this.updatePropertiesBatched(batch -> {
            this.carrier.write(batch, pose, previousPose, appearance, appearanceChanged, dyeColor, dyeChanged,
                    interpolationSequence, this.interpolation.duration(), this.interpolation.delay());
            if (orientationChanged) {
                this.carrier.orientation(batch, this.renderYaw, this.renderPitch);
            }
            if (styleChanged) {
                this.carrier.style(batch, style);
            }
        }, false);
        this.sentPose = pose;
        this.sentAppearance = appearance;
        this.sentStyle = style;
        this.sentDyeColor = dyeColor;
        this.sentYaw = this.renderYaw;
        this.sentPitch = this.renderPitch;
        if (carrierChanged && this.valid && !spawning) {
            this.respawnCarrier();
        }
    }

    private boolean updateCarrierDefinition() {
        if (this.model == null || this.carrier == this.model.carrier()) {
            return false;
        }
        this.carrier = this.model.carrier();
        this.bedrockDefinition = Registries.BEDROCK_ENTITY_DEFINITIONS.get(this.carrier.id());
        return true;
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

    private void respawnCarrier() {
        // 复用运行时 ID，先生成新载体并发送装备，再恢复原有骑乘关系
        RemoveEntityPacket remove = new RemoveEntityPacket();
        remove.setUniqueEntityId(this.geyserId);
        this.session.sendUpstreamPacket(remove);
        this.spawnCarrier();
        if (this.vehicle != null) {
            this.sendPassengerLink(this.vehicle, this);
        }
        for (Entity passenger : this.passengers) {
            this.sendPassengerLink(this, passenger);
        }
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

    private void sendPassengerLink(Entity vehicle, Entity passenger) {
        EntityLinkData.Type type = vehicle.getPassengers().indexOf(passenger) <= 0
                ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
        SetEntityLinkPacket link = new SetEntityLinkPacket();
        link.setEntityLink(new EntityLinkData(
                vehicle.geyserId(),
                passenger.geyserId(),
                type,
                true,
                false,
                0
        ));
        this.session.sendUpstreamPacket(link);
    }

    @Override
    public void tick() {
        int ticks = this.movementTicks;
        if (ticks > 0) {
            this.movementTicks = ticks - 1;
            this.sampleMovement(ticks);
        }
        this.updateDisplay(false);
    }

    private void sampleMovement(int ticks) {
        double step = 1.0 / ticks;
        if (!this.renderPosition.equals(this.position)) {
            Vector3f from = this.renderPosition;
            Vector3f to = this.position;
            this.renderPosition = ticks == 1 ? to : Vector3f.from(
                    from.getX() + (to.getX() - (double) from.getX()) * step,
                    from.getY() + (to.getY() - (double) from.getY()) * step,
                    from.getZ() + (to.getZ() - (double) from.getZ()) * step
            );
            if (!this.renderPosition.equals(from)) {
                this.sendMovement(false);
            }
        }
        float yaw = (float) (this.renderYaw + GenericMath.wrapAngleDeg(this.yaw - this.renderYaw) * step);
        float pitch = (float) (this.renderPitch + (this.pitch - (double) this.renderPitch) * step);
        this.renderRotation(yaw, pitch);
    }

    @Override
    public void moveRelativeRaw(double x, double y, double z, float yaw, float pitch, float headYaw, boolean grounded) {
        Vector3f target = x == 0 && y == 0 && z == 0 ? this.position : this.position.add(x, y, z);
        this.moveAbsoluteRaw(target, yaw, pitch, headYaw, grounded, false);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean grounded, boolean teleported) {
        boolean groundChanged = this.onGround != grounded;
        this.setOnGround(grounded);
        // Geyser 用上一包的目标解码相对坐标和缺省朝向，基岩只接收插值后的显示值
        boolean changed = !position.equals(this.position) || yaw != this.yaw || pitch != this.pitch;
        this.setPosition(position);
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.setHeadYaw(yaw);
        if (teleported || this.teleportDuration == 0) {
            this.movementTicks = 0;
            boolean positionChanged = !position.equals(this.renderPosition);
            this.renderPosition = position;
            this.renderRotation(yaw, pitch);
            if (positionChanged || groundChanged) {
                this.sendMovement(teleported);
            }
        } else if (changed) {
            this.movementTicks = this.teleportDuration;
        }
    }

    @Override
    public void updateHeadLookRotation(float headYaw) {
        // 展示实体没有独立的头部朝向
    }

    @Override
    public void setVehicle(Entity vehicle) {
        if (vehicle != this.vehicle) {
            this.seatOffset = null;
        }
        super.setVehicle(vehicle);
    }

    @Override
    public void setRiderSeatPosition(Vector3f offset) {
        this.seatOffset = offset;
        super.setRiderSeatPosition(offset);
    }

    private void renderRotation(float yaw, float pitch) {
        if (yaw != this.renderYaw || pitch != this.renderPitch) {
            this.renderYaw = yaw;
            this.renderPitch = pitch;
            this.orientationDirty = true;
        }
    }

    private void sendMovement(boolean teleported) {
        if (this.valid) {
            MoveEntityAbsolutePacket packet = new MoveEntityAbsolutePacket();
            packet.setRuntimeEntityId(this.geyserId);
            packet.setPosition(this.bedrockPosition());
            packet.setRotation(Vector3f.ZERO);
            packet.setOnGround(this.onGround);
            packet.setTeleported(teleported);
            this.session.sendUpstreamPacket(packet);
        }
    }

    Vector3f renderPosition() {
        return this.renderPosition;
    }

    float renderYaw() {
        return this.renderYaw;
    }

    @Override
    public Vector3f bedrockPosition() {
        float offset = this.getOffset();
        return offset == 0 ? this.renderPosition : this.renderPosition.up(offset);
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
        if (this.vehicle != null && this.seatOffset != null) {
            packet.getMetadata().put(EntityDataTypes.SEAT_OFFSET, this.seatOffset);
        }
        packet.setRotation(Vector2f.ZERO);
        packet.setHeadRotation(0);
        packet.setBodyRotation(0);
    }
}
