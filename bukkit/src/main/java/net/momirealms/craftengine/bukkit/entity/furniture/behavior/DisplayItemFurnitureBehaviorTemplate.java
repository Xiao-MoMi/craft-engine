package net.momirealms.craftengine.bukkit.entity.furniture.behavior;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.entity.data.item.ItemEntityData;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorFactory;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfigs;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.context.InteractEntityContext;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

public final class DisplayItemFurnitureBehaviorTemplate extends FurnitureBehaviorTemplate {
    public static final FurnitureBehaviorFactory<DisplayItemFurnitureBehaviorTemplate> FACTORY = new Factory();
    @NotNull
    public final Map<String, VariantRule> variantRules;
    @Nullable
    public final SoundData putSound;
    @Nullable
    public final SoundData takeSound;
    @Nullable
    public final String customDataKey;

    private DisplayItemFurnitureBehaviorTemplate(FurnitureDefinition furniture,
                                                 @NotNull Map<String, VariantRule> variantRules,
                                                 @Nullable SoundData putSound,
                                                 @Nullable SoundData takeSound,
                                                 @Nullable String customDataKey
    ) {
        super(furniture);
        this.customDataKey = customDataKey;
        this.variantRules = variantRules;
        this.putSound = putSound;
        this.takeSound = takeSound;
    }

    @Override
    public FurnitureController createController(Furniture furniture) {
        return new DisplayItemFurnitureController(furniture, this);
    }

    // 行为处理器
    public static final class DisplayItemFurnitureController extends FurnitureController {
        private static final String DEFAULT_DATA_KEY = "craftengine:display_item";
        private final DisplayItemFurnitureBehaviorTemplate behavior;
        DisplayItemElement displayItemElement;
        Set<FurnitureHitBox> trackedHitboxes;
        @NotNull
        Item savedItem;

        public DisplayItemFurnitureController(Furniture furniture, DisplayItemFurnitureBehaviorTemplate behavior) {
            super(furniture);
            this.behavior = behavior;
            this.savedItem = Item.empty();
        }

        @Override
        public void loadCustomData(CompoundTag data) {
            CompoundTag displayItem = data.getCompound(Optional.ofNullable(behavior.customDataKey).orElse(DEFAULT_DATA_KEY));
            if (displayItem != null) {
                int dataVersion = displayItem.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
                this.savedItem = ItemStackUtils.wrap(ItemStackUtils.parseMinecraftItem(displayItem, dataVersion));
            }
        }

        @Override
        public void saveCustomData(CompoundTag data) {
            if (!this.savedItem.isEmpty()) {
                Tag itemStackAsTag = ItemStackUtils.saveMinecraftItemStackAsTag(this.savedItem.minecraftItem());
                if (itemStackAsTag != null) {
                    data.put(Optional.ofNullable(behavior.customDataKey).orElse(DEFAULT_DATA_KEY), itemStackAsTag);
                }
            }
        }

        @Override
        public InteractionResult useOnFurniture(FurnitureHitBox hitBox, InteractEntityContext context) {
            // 如果配置了追踪碰撞箱, 则检查是不是追踪的碰撞箱, 不是则直接PASS不处理;
            // 如果没配置, 则全部碰撞箱都可以.
            boolean hasSpecialHitBoxes = (this.trackedHitboxes != null);
            if (hasSpecialHitBoxes && !this.trackedHitboxes.contains(hitBox)) {
                return InteractionResult.PASS;
            }
            // 检查区域保护权限
            Player player = context.getPlayer();
            if (player.isSneaking()) {
                return InteractionResult.PASS;
            }
            WorldPosition pos = furniture.position();
            Location location = new Location((World) pos.world.platformWorld(), pos.x, pos.y, pos.z);
            if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.OPEN_CONTAINER, location)) {
                return InteractionResult.FAIL;
            }
            // 如果当前不存在物品并且手中有物品, 则放入1个物品进去.
            Item itemInHand = context.getItem();
            if (ItemUtils.isEmpty(this.savedItem) && !ItemUtils.isEmpty(itemInHand)) {
                Item inputItem = itemInHand.copyWithCount(1);
                if (!player.canInstabuild()) {
                    itemInHand.shrink(1);
                }
                this.handlePutDisplayItem(inputItem);
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
            // 如果当前存在物品, 并且手中没有物品, 则取出物品到手中.
            else if (!ItemUtils.isEmpty(this.savedItem) && ItemUtils.isEmpty(itemInHand)) {
                player.setItemInHand(context.getHand(), this.savedItem);
                this.handleTakeDisplayItem();
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
            // 如果交互行为无效, 且交互的是特殊碰撞箱, 则让结果为失败 (不传递给下个行为);
            // 否则继续传递到下一个行为处理.
            return hasSpecialHitBoxes ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        // 破坏家具时, 掉落存储的展示物品.
        @Override
        public void preRemove(Player player) {
            if (!ItemUtils.isEmpty(this.savedItem) && this.displayItemElement != null) {
                this.furniture.world().dropItemNaturally(this.displayItemElement.position, this.savedItem);
            }
        }

        // 处理放入展示物品, 存储刷新并播放音效.
        private void handlePutDisplayItem(Item inputItem) {
            saveDisplayItem(inputItem);
            this.furniture.refreshElements();
            if (this.behavior.putSound != null) {
                this.furniture.world().playSound(this.furniture.position(), this.behavior.putSound.id(), this.behavior.putSound.volume().get(), this.behavior.putSound.pitch().get(), SoundSource.MASTER);
            }
        }

        // 处理取出展示物品逻辑, 存储刷新并播放音效.
        private void handleTakeDisplayItem() {
            saveDisplayItem(null);
            this.furniture.refreshElements();
            if (behavior.takeSound != null) {
                this.furniture.world().playSound(this.furniture.position(), this.behavior.takeSound.id(), this.behavior.takeSound.volume().get(), this.behavior.takeSound.pitch().get(), SoundSource.MASTER);
            }
        }

        // 根据当前家具变体查找对应的展示物品相对坐标
        @Override
        public void gatherElements(Consumer<FurnitureElement> consumer) {
            VariantRule variantRule = this.behavior.variantRules.get(furniture.getCurrentVariant().name());
            if (variantRule != null) {
                this.displayItemElement = new DisplayItemElement(this.furniture, this, variantRule.itemRelative);
                consumer.accept(this.displayItemElement);
            }
        }

        // 根据当前家具变体查找对应的碰撞箱并创建
        @Override
        public void gatherHitboxes(Consumer<FurnitureHitBox> consumer) {
            VariantRule variantRule = this.behavior.variantRules.get(furniture.getCurrentVariant().name());
            if (variantRule != null && !variantRule.hitBoxConfigs.isEmpty()) {
                this.trackedHitboxes = new HashSet<>();
                for (FurnitureHitBoxConfig<? extends FurnitureHitBox> hitBoxConfig : variantRule.hitBoxConfigs) {
                    FurnitureHitBox furnitureHitBox = hitBoxConfig.create(this.furniture);
                    this.trackedHitboxes.add(furnitureHitBox);
                    consumer.accept(furnitureHitBox);
                }
            }
        }

        // 设置存储的物品
        private void saveDisplayItem(@Nullable Item item) {
            if (item != null) {
                this.savedItem = item;
            } else {
                this.savedItem = Item.empty();
            }
            this.furniture.setUnsaved();
        }

        @Override
        public @Nullable Item getItemToPickup(Player player, FurnitureHitBox hitBox) {
            if (ItemUtils.isEmpty(this.savedItem)) return null;
            boolean hasSpecialHitBoxes = (this.trackedHitboxes != null);
            if (hasSpecialHitBoxes) {
                if (this.trackedHitboxes.contains(hitBox)) {
                    return this.savedItem;
                }
                return null;
            }
            return savedItem;
        }
    }

    // 展示元素
    public static final class DisplayItemElement implements FurnitureElement {
        public final Furniture furniture;
        public final DisplayItemFurnitureController furnitureHandler;
        public final WorldPosition position;
        public final int vehicleId;
        public final int passengerId;
        public final Object despawnAllPacket;
        public final Object despawnVehiclePacket;
        public final Object despawnPassengerPacket;
        public final Object spawnVehiclePacket;
        public final Object spawnPassengerPacket;
        public final Object ridePacket;

        public DisplayItemElement(Furniture furniture, DisplayItemFurnitureController furnitureHandler, Vector3f relative) {
            this.furniture = furniture;
            this.furnitureHandler = furnitureHandler;
            WorldPosition furniturePos = furniture.position();
            Vec3d position = Furniture.getRelativePosition(furniturePos, relative);
            this.position = new WorldPosition(furniturePos.world, position.x, position.y, position.z, furniturePos.xRot, furniturePos.yRot);
            this.vehicleId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
            this.passengerId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
            this.spawnVehiclePacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                    vehicleId, UUID.randomUUID(), position.x, position.y, position.z,
                    0, 0, EntityTypeProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
            );
            this.spawnPassengerPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                    passengerId, UUID.randomUUID(), position.x, position.y, position.z,
                    0, 0, EntityTypeProxy.ITEM, 0, Vec3Proxy.ZERO, 0
            );
            this.ridePacket = PacketUtils.createClientboundSetPassengersPacket(this.vehicleId, this.passengerId);
            this.despawnAllPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(),
                    a -> {
                        a.add(this.vehicleId);
                        a.add(this.passengerId);
                    }
            ));
            this.despawnVehiclePacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(this.vehicleId)));
            this.despawnPassengerPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(this.passengerId)));
        }

        @Override
        public void gatherInteractableEntityId(Consumer<Integer> collector) {
        }

        @Override
        public void show(Player player) {
            // 没有物品就不展示
            if (!furnitureHandler.savedItem.isEmpty()) {
                List<Object> list = new ArrayList<>();
                ItemEntityData.Item.addEntityData(this.furnitureHandler.savedItem.minecraftItem(), list);
                Object setEntityDataPacket = ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.passengerId, list);
                player.sendPackets(List.of(
                        this.spawnVehiclePacket,
                        this.spawnPassengerPacket,
                        this.ridePacket,
                        setEntityDataPacket
                ), false);
            }
        }

        @Override
        public void hide(Player player) {
            player.sendPacket(this.despawnAllPacket, false);
        }

        @Override
        public void refresh(Player player) {
            // 没有物品就不展示
            if (furnitureHandler.savedItem.isEmpty()) {
                this.hide(player);
            } else {
                List<Object> list = MiscUtils.init(new ArrayList<>(), it -> {
                    ItemEntityData.Item.addEntityData(this.furnitureHandler.savedItem.minecraftItem(), it);
                    ItemEntityData.NoGravity.addEntityData(true, it);
                });
                Object changeDisplayItemPacket = ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.passengerId, list);
                player.sendPackets(List.of(
                        this.despawnPassengerPacket, this.spawnVehiclePacket, this.spawnPassengerPacket, this.ridePacket, changeDisplayItemPacket
                ), false);
            }
        }
    }

    // 工厂类
    private static class Factory implements FurnitureBehaviorFactory<DisplayItemFurnitureBehaviorTemplate> {
        private static final String[] ITEM_POSITION = new String[] {"item_position", "item-position"};
        private static final String[] DATA_KEY = new String[] {"data_key", "data-key"};

        @Override
        public DisplayItemFurnitureBehaviorTemplate create(FurnitureDefinition furniture, ConfigSection section) {
            // 读取放入取出音效
            ConfigSection soundSection = section.getSection("sounds");
            SoundData inputSound = null;
            SoundData takeSound = null;
            if (soundSection != null) {
                inputSound = soundSection.getValue("put", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.RANGED_0_9_1));
                takeSound = soundSection.getValue("take", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.RANGED_0_9_1));
            }
            // 如果没有配置变体展示规则
            ConfigSection variantsSection = section.getSection("variants");
            Map<String, VariantRule> variantRule;
            if (variantsSection == null) {
                variantRule = Map.of();
            } else {
                // 读取变体展示规则
                variantRule = new HashMap<>();
                for (String variantName : variantsSection.keySet()) {
                    ConfigSection variantSection = variantsSection.getSection(variantName);
                    Vector3f itemRelative = variantSection.getVector3f(ITEM_POSITION, ConfigConstants.ZERO_VECTOR3);
                    List<? extends FurnitureHitBoxConfig<? extends FurnitureHitBox>> hitboxes =
                            variantSection.getList("hitboxes", v -> FurnitureHitBoxConfigs.fromConfig(v.getAsSection()));
                    variantRule.put(variantName, new VariantRule(itemRelative, hitboxes));
                }
            }
            return new DisplayItemFurnitureBehaviorTemplate(furniture, variantRule, inputSound, takeSound, section.getString(DATA_KEY));
        }
    }

    // 变体展示规则
    public record VariantRule(
            Vector3f itemRelative,
            List<? extends FurnitureHitBoxConfig<? extends FurnitureHitBox>> hitBoxConfigs
    ) {}
}
