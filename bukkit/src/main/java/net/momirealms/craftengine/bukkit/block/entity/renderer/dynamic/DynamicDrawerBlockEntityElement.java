package net.momirealms.craftengine.bukkit.block.entity.renderer.dynamic;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.block.entity.DrawerBlockEntityController;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DynamicDrawerBlockEntityElement implements BlockEntityElement {
    public final DrawerBlockEntityController controller;
    public final int itemId;
    public final int textId;
    public final UUID itemUUID = UUID.randomUUID();
    public final UUID textUUID = UUID.randomUUID();
    public final Object despawnItemPacket;
    public final Object despawnTextPacket;
    public final Object despawnAllPacket;
    private @NotNull Object spawnItemPacket;
    private @NotNull Object spawnTextPacket;
    private @NotNull Object changeItemDataPacket;
    private @NotNull Object changeTextContentDataPacket;
    private @NotNull Object updateItemPosPacket;
    private @NotNull Object updateTextPosPacket;

    public DynamicDrawerBlockEntityElement(
            @NotNull DrawerBlockEntityController controller,
            @NotNull WorldPosition itemPosition,
            @NotNull WorldPosition textPosition,
            float entityYRot
    ) {
        this.controller = controller;
        this.itemId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        this.textId = EntityProxy.ENTITY_COUNTER.incrementAndGet();
        // 包缓存
        this.despawnItemPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(itemId)));
        this.despawnTextPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> a.add(textId)));
        this.despawnAllPacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), a -> {
                    a.add(itemId);
                    a.add(textId);
        }));
        this.refreshChangeDisplayItemPacket(Item.empty());
        this.refreshChangeTextContentPacket(0);
        this.refreshSpawnItemAndTextPacket(itemPosition, textPosition, entityYRot);
    }

    // 更新展示的物品包
    public void refreshChangeDisplayItemPacket(Item item) {
        this.changeItemDataPacket = ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.itemId, new ArrayList<>() {{
            DisplayData.ItemDisplayData.ItemStack.addEntityData(item.minecraftItem(), this);
            DisplayData.ItemDisplayData.Scale.addEntityData(controller.behavior.itemScale, this);
            DisplayData.ItemDisplayData.ItemTransform.addEntityData(ItemDisplayContext.FIXED.id(), this);
        }});
    }

    // 更新显示的数量包
    public void refreshChangeTextContentPacket(int count) {
        this.changeTextContentDataPacket = ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.textId, new ArrayList<>() {{
            DisplayData.TextDisplayData.Text.addEntityData(ComponentUtils.adventureToMinecraft(Component.text(count)), this);
            DisplayData.TextDisplayData.Scale.addEntityData(controller.behavior.textScale, this);
        }});
    }

    // 更新展示物品的位置
    public void refreshSpawnItemAndTextPacket(WorldPosition itemPosition, WorldPosition textPosition, float entityYRot) {
        this.spawnItemPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                itemId, itemUUID, itemPosition.x, itemPosition.y, itemPosition.z,
                0.0f, entityYRot, EntityTypeProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
        this.spawnTextPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                textId, textUUID, textPosition.x, textPosition.y, textPosition.z,
                0.0f, entityYRot, EntityTypeProxy.TEXT_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
        this.updateItemPosPacket = EntityUtils.createUpdatePosPacket(this.itemId,
                itemPosition.x, itemPosition.y, itemPosition.z,
                entityYRot, 0.0f, true
        );
        this.updateTextPosPacket = EntityUtils.createUpdatePosPacket(this.textId,
                textPosition.x, textPosition.y, textPosition.z,
                entityYRot, 0.0f, true
        );
    }

    @Override
    public void show(@NotNull Player player) {
        if (!this.controller.item().isEmpty()) {
            player.sendPackets(List.of(
                    this.spawnItemPacket,
                    this.spawnTextPacket,
                    this.changeItemDataPacket,
                    this.changeTextContentDataPacket
            ), false);
        }
    }

    @Override
    public void hide(@NotNull Player player) {
        player.sendPacket(this.despawnAllPacket, false);
    }

    // 更新整套展示实体
    public void updateItemAndText(Player player, boolean spawn) {
        if (spawn) {
            player.sendPackets(List.of(
                    this.spawnItemPacket, this.spawnTextPacket,
                    this.changeItemDataPacket, this.changeTextContentDataPacket
            ), false);
        } else {
            player.sendPackets(List.of(
                    this.changeItemDataPacket, this.changeTextContentDataPacket
            ), false);
        }
    }

    // 更新文本展示实体的文本
    public void updateDisplayItem(Player player) {
        player.sendPacket(this.changeItemDataPacket, false);
    }

    // 更新文本展示实体的文本
    public void updateTextContent(Player player) {
        player.sendPacket(this.changeTextContentDataPacket, false);
    }

    // 更新元素展示位置
    public void updateElementPos(Player player) {
        player.sendPackets(List.of(this.updateItemPosPacket, this.updateTextPosPacket), false);
    }
}
