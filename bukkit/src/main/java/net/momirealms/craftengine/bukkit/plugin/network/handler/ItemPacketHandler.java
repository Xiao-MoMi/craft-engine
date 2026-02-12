package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.data.ItemEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.world.score.BukkitTeamManager;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemSettings;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.text.minimessage.CustomTagResolver;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.ArrayUtils;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.LegacyChatFormatter;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class ItemPacketHandler implements EntityPacketHandler {
    public static final ItemPacketHandler INSTANCE = new ItemPacketHandler();

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean changed = false;
        List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
        Component nameToShow = null;
        LegacyChatFormatter glowColor = null;
        for (int i = 0; i < packedItems.size(); i++) {
            Object packedItem = packedItems.get(i);
            int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
            if (entityDataId == ItemEntityData.Item.id()) {
                Object nmsItemStack = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(nmsItemStack);

                // 转换为客户端侧物品
                Optional<ItemStack> optional = BukkitItemManager.instance().s2c(itemStack, user);
                if (optional.isPresent()) {
                    changed = true;
                    itemStack = optional.get();
                    Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                    packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(entityDataId, serializer, FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack)));
                }

                // 处理 drop-display 物品设置
                // 一定要处理经历过客户端侧组件修改的物品
                Item<ItemStack> wrappedItem = BukkitItemManager.instance().wrap(itemStack);
                Optional<CustomItem<ItemStack>> optionalCustomItem = wrappedItem.getCustomItem();
                String showName = null;
                if (optionalCustomItem.isPresent()) {
                    ItemSettings settings = optionalCustomItem.get().settings();
                    showName = settings.dropDisplay();
                    glowColor = settings.glowColor();
                } else if (Config.enableDefaultDropDisplay()) {
                    showName = Config.defaultDropDisplayFormat();
                }

                // 如果设定了自定义展示名
                if (showName != null) {
                    PlayerOptionalContext context = NetworkTextReplaceContext.of(user, ContextHolder.builder()
                            .withParameter(DirectContextParameters.COUNT, itemStack.getAmount()));
                    Optional<Component> optionalHoverComponent = wrappedItem.hoverNameComponent();
                    Component hoverComponent;
                    if (optionalHoverComponent.isPresent()) {
                        hoverComponent = optionalHoverComponent.get();
                    } else {
                        hoverComponent = Component.translatable(itemStack.translationKey());
                    }
                    // 展示名称为空，则显示其hover name
                    if (showName.isEmpty()) {
                        nameToShow = hoverComponent;
                    }
                    // 显示自定义格式的名字
                    else {
                        nameToShow = AdventureHelper.miniMessage().deserialize(
                                showName,
                                ArrayUtils.appendElementToArrayTail(context.tagResolvers(), new CustomTagResolver("name", hoverComponent))
                        );
                    }
                }
                break;
            }
        }
        if (glowColor != null) {
            Object teamByColor = BukkitTeamManager.instance().getTeamByColor(glowColor);
            if (teamByColor != null) {
                changed = true;
                outer: {
                    for (int i = 0; i < packedItems.size(); i++) {
                        Object packedItem = packedItems.get(i);
                        int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
                        if (entityDataId == BaseEntityData.SharedFlags.id()) {
                            byte flags = (Byte) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                            flags |= (byte) 0x40;
                            packedItems.set(i, BaseEntityData.SharedFlags.createEntityData(flags));
                            break outer;
                        }
                    }
                    packedItems.add(BaseEntityData.SharedFlags.createEntityData((byte) 0x40));
                }
                Object entityLookup = FastNMS.INSTANCE.method$ServerLevel$getEntityLookup(user.clientSideWorld().serverWorld());
                Object entity = FastNMS.INSTANCE.method$EntityLookup$get(entityLookup, id);
                if (entity != null) {
                    user.sendPacket(FastNMS.INSTANCE.method$ClientboundSetPlayerTeamPacket$createMultiplePlayerPacket(teamByColor, List.of(FastNMS.INSTANCE.method$Entity$getUUID(entity).toString()), true), false);
                }
            }
        }
        // 添加自定义显示名
        if (nameToShow != null) {
            changed = true;
            packedItems.add(ItemEntityData.CustomNameVisible.createEntityData(true));
            packedItems.add(ItemEntityData.CustomName.createEntityData(Optional.of(ComponentUtils.adventureToMinecraft(nameToShow))));
        }
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(id);
            FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(packedItems, buf);
        }
    }
}
