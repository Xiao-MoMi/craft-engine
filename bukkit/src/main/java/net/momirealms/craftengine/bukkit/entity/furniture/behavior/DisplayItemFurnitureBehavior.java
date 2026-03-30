package net.momirealms.craftengine.bukkit.entity.furniture.behavior;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.entity.data.ItemEntityData;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehavior;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorFactory;
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

public final class DisplayItemFurnitureBehavior extends FurnitureBehavior<DisplayItemFurnitureBehavior.Data> {
    public static final FurnitureBehaviorFactory<DisplayItemFurnitureBehavior> FACTORY = new Factory();
    private static final String DISPLAY_ITEM_TAG = "display_item";
    @NotNull
    private final Map<String, VariantRule> variantRules;
    @Nullable
    private final SoundData putSound;
    @Nullable
    private final SoundData takeSound;

    private DisplayItemFurnitureBehavior(CustomFurniture furniture,
                                         @NotNull Map<String, VariantRule> variantRules,
                                         @Nullable SoundData putSound,
                                         @Nullable SoundData takeSound
    ) {
        super(furniture);
        this.variantRules = variantRules;
        this.putSound = putSound;
        this.takeSound = takeSound;
    }

    @Override
    public void onLoad(Furniture furniture, Data data) {
        CompoundTag displayItem = (CompoundTag) furniture.persistentData.getCustomData(DISPLAY_ITEM_TAG);
        if (displayItem != null) {
            int dataVersion = displayItem.getInt(DISPLAY_ITEM_TAG, Config.itemDataFixerUpperFallbackVersion());
            data.displayItem = ItemStackUtils.wrap(ItemStackUtils.parseMinecraftItem(displayItem, dataVersion));
        }
    }

    @Override
    public InteractionResult useOnFurniture(Furniture furniture, FurnitureHitBox hitBox, InteractEntityContext context, Data data) {
        // 如果配置了追踪碰撞箱, 则检查是不是追踪的碰撞箱, 如果没配置则全部碰撞箱都可以.
        Set<FurnitureHitBox> trackedHitboxes = data.trackedHitboxes;
        if (trackedHitboxes != null && !trackedHitboxes.contains(hitBox)) {
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
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        // 如果当前不存在物品并且手中有物品, 则放入1个物品进去.
        Item displayItem = displayItem(data);
        Item itemInHand = context.getItem();
        if (ItemUtils.isEmpty(displayItem) && !ItemUtils.isEmpty(itemInHand)) {
            Item inputItem = itemInHand.copyWithCount(1);
            if (!player.canInstabuild()) {
                itemInHand.shrink(1);
            }
            this.handlePutDisplayItem(furniture, inputItem, data);
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        // 如果当前存在物品, 并且手中没有物品, 则取出物品到手中.
        else if (!ItemUtils.isEmpty(displayItem) && ItemUtils.isEmpty(itemInHand)) {
            player.setItemInHand(context.getHand(), displayItem);
            this.handleTakeDisplayItem(furniture, data);
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        return InteractionResult.PASS;
    }

    // 破坏家具时, 掉落存储的展示物品.
    @Override
    public void onDestroy(Furniture furniture, Data data) {
        Item displayItem = displayItem(data);
        DisplayItemElement displayItemElement = data.displayElement;
        if (!ItemUtils.isEmpty(displayItem) && displayItemElement != null) {
            furniture.world().dropItemNaturally(displayItemElement.position, displayItem);
        }
    }

    // 处理放入展示物品, 存储刷新并播放音效.
    private void handlePutDisplayItem(Furniture furniture, Item inputItem, Data data) {
        saveDisplayItem(furniture, inputItem, data);
        furniture.refreshElements();
        if (putSound != null) {
            furniture.world().playSound(furniture.position(), putSound.id(), putSound.volume().get(), putSound.pitch().get(), SoundSource.MASTER);
        }
    }

    // 处理取出展示物品逻辑, 存储刷新并播放音效.
    private void handleTakeDisplayItem(Furniture furniture, Data data) {
        saveDisplayItem(furniture, null, data);
        furniture.refreshElements();
        if (takeSound != null) {
            furniture.world().playSound(furniture.position(), takeSound.id(), takeSound.volume().get(), takeSound.pitch().get(), SoundSource.MASTER);
        }
    }

    // 根据当前家具变体查找对应的展示物品相对坐标
    @Override
    public void createFurnitureElements(Furniture furniture, Consumer<FurnitureElement> consumer, Data data) {
        VariantRule variantRule = variantRules.get(furniture.getCurrentVariant().name());
        if (variantRule != null) {
            DisplayItemElement displayItemElement = new DisplayItemElement(furniture, variantRule.itemRelative, data);
            data.displayElement = displayItemElement;
            consumer.accept(displayItemElement);
        }
    }

    // 根据当前家具变体查找对应的碰撞箱并创建
    @Override
    public void createFurnitureHitboxes(Furniture furniture, Consumer<FurnitureHitBox> consumer, Data data) {
        VariantRule variantRule = variantRules.get(furniture.getCurrentVariant().name());
        if (variantRule != null && !variantRule.hitBoxConfigs.isEmpty()) {
            HashSet<FurnitureHitBox> trackedHitBoxes = new HashSet<>();
            for (FurnitureHitBoxConfig<? extends FurnitureHitBox> hitBoxConfig : variantRule.hitBoxConfigs) {
                FurnitureHitBox furnitureHitBox = hitBoxConfig.create(furniture);
                trackedHitBoxes.add(furnitureHitBox);
                consumer.accept(furnitureHitBox);
            }
            data.trackedHitboxes = trackedHitBoxes;
        }
    }

    // 获取存储的展示物品
    @NotNull
    private static Item displayItem(Data data) {
        Item displayItem = data.displayItem;
        if (ItemUtils.isEmpty(displayItem)) {
            return ItemStackUtils.wrap(null);
        }
        return displayItem;
    }

    // 设置存储的物品
    private void saveDisplayItem(Furniture furniture, @Nullable Item item, Data data) {
        if (item != null) {
            Tag itemStackAsTag = ItemStackUtils.saveMinecraftItemStackAsTag(item.getMinecraftItem());
            furniture.persistentData.addCustomData(DISPLAY_ITEM_TAG, itemStackAsTag);
            data.displayItem = item;
        } else {
            furniture.persistentData.removeCustomData(DISPLAY_ITEM_TAG);
            data.displayItem = ItemStackUtils.wrap(null);
        }
    }

    @Override
    public @Nullable Item getItemToPickup(Furniture furniture, Player player, FurnitureHitBox hitBox, Data data) {
        Item displayItem = data.displayItem;
        if (ItemUtils.isEmpty(displayItem)) return null;
        Set<FurnitureHitBox> trackedHitboxes = data.trackedHitboxes;
        boolean hasSpecialHitBoxes = (trackedHitboxes != null);
        if (hasSpecialHitBoxes) {
            if (trackedHitboxes.contains(hitBox)) {
                return displayItem;
            }
            return null;
        }
        return displayItem;
    }

    @Override
    public Data createData(Furniture furniture) {
        return new Data();
    }

    public static final class DisplayItemElement implements FurnitureElement {
        public final Furniture furniture;
        public final WorldPosition position;
        public final int vehicleId;
        public final int passengerId;
        public final Object despawnAllPacket;
        public final Object despawnVehiclePacket;
        public final Object despawnPassengerPacket;
        public final Object spawnVehiclePacket;
        public final Object spawnPassengerPacket;
        public final Object ridePacket;
        public final Data data;

        public DisplayItemElement(Furniture furniture, Vector3f relative, Data data) {
            this.furniture = furniture;
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
            this.ridePacket = PacketUtils.createClientboundSetPassengersPacket(vehicleId, passengerId);
            this.despawnAllPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(),
                    a -> {
                        a.add(vehicleId);
                        a.add(passengerId);
                    }
            ));
            this.despawnVehiclePacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(vehicleId)));
            this.despawnPassengerPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(passengerId)));
            this.data = data;
        }

        @Override
        public void collectInteractableEntityId(Consumer<Integer> collector) {
        }

        @Override
        public void show(Player player) {
            List<Object> list = new ArrayList<>();
            ItemEntityData.Item.addEntityData(displayItem(this.data).getMinecraftItem(), list);
            Object setEntityDataPacket = ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.passengerId, list);
            player.sendPackets(List.of(
                    this.spawnVehiclePacket,
                    this.spawnPassengerPacket,
                    this.ridePacket,
                    setEntityDataPacket
            ), false);
        }

        @Override
        public void hide(Player player) {
            player.sendPacket(this.despawnAllPacket, false);
        }

        @Override
        public void refresh(Player player) {
            List<Object> list = MiscUtils.init(new ArrayList<>(), it -> ItemEntityData.Item.addEntityData(displayItem(this.data).getMinecraftItem(), it));
            Object changeDisplayItemPacket = ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.passengerId, list);
            player.sendPackets(List.of(
                    despawnPassengerPacket, spawnPassengerPacket, ridePacket, changeDisplayItemPacket
            ), false);
        }
    }

    private static class Factory implements FurnitureBehaviorFactory<DisplayItemFurnitureBehavior> {
        private static final String[] ITEM_POSITION = new String[] {"item_position", "item-position"};

        @Override
        public DisplayItemFurnitureBehavior create(CustomFurniture furniture, ConfigSection section) {
            // 如果没有配置变体展示规则
            ConfigSection variantsSection = section.getSection("variants");
            if (variantsSection == null) {
                return new DisplayItemFurnitureBehavior(furniture, Map.of(), null, null);
            }
            // 读取变体展示规则
            HashMap<String, VariantRule> variantRule = new HashMap<>();
            for (String variantName : variantsSection.keySet()) {
                ConfigSection variantSection = variantsSection.getSection(variantName);
                Vector3f itemRelative = variantSection.getVector3f(ITEM_POSITION, ConfigConstants.ZERO_VECTOR3);
                List<? extends FurnitureHitBoxConfig<? extends FurnitureHitBox>> hitboxes =
                        variantSection.getList("hitboxes", v -> FurnitureHitBoxConfigs.fromConfig(v.getAsSection()));
                variantRule.put(variantName, new VariantRule(itemRelative, hitboxes));
            }
            // 读取放入取出音效
            ConfigSection soundSection = section.getSection("sounds");
            SoundData inputSound = null;
            SoundData takeSound = null;
            if (soundSection != null) {
                inputSound = soundSection.getValue("put", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.RANGED_0_9_1));
                takeSound = soundSection.getValue("take", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.RANGED_0_9_1));
            }
            return new DisplayItemFurnitureBehavior(furniture, variantRule, inputSound, takeSound);
        }
    }

    // 变体展示规则
    public record VariantRule(
            Vector3f itemRelative,
            List<? extends FurnitureHitBoxConfig<? extends FurnitureHitBox>> hitBoxConfigs
    ) {}

    public static class Data {
        private Item displayItem;
        private Set<FurnitureHitBox> trackedHitboxes;
        private DisplayItemElement displayElement;
    }
}
