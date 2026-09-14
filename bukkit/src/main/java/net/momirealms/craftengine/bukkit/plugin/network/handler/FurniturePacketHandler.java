package net.momirealms.craftengine.bukkit.plugin.network.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.plugin.network.packet.ClientboundFurnitureUpdatePacket;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullableHolder;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.furniture.FurnitureSnapshotState;
import net.momirealms.craftengine.core.entity.furniture.element.ConditionalFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.TransformableFurnitureElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.List;

public final class FurniturePacketHandler implements EntityPacketHandler, Cullable {
    public final BukkitFurniture furniture;
    private FurnitureSnapshotState appliedSnapshot;
    private FurnitureSnapshotState shownSnapshot;
    // 保存实际通过显示条件的元素，切换后不重新求值旧条件来猜测客户端状态。
    private List<FurnitureElement> visibleElements = List.of();

    public FurniturePacketHandler(BukkitFurniture furniture) {
        this.furniture = furniture;
    }

    public void synchronize(Player player) {
        FurnitureSnapshotState current = this.furniture.clientSnapshot();
        if (current == null) return; // 首包早于行为 onLoad 时，等待完成后补发的生成包。
        if (this.appliedSnapshot != current) {
            if (this.appliedSnapshot == null) {
                this.furniture.controller.onAsyncPlayerTrack(player, current);
            } else {
                this.furniture.controller.onAsyncPlayerVariantChange(player, this.appliedSnapshot, current);
            }
            this.appliedSnapshot = current;
        }
        if (!Config.enableEntityCulling()) {
            this.showSnapshot(player, current);
            return;
        }
        CullableHolder holder = player.getTrackedEntity(this.furniture.entityId());
        if (holder != null && holder.isShown) {
            this.showSnapshot(player, current);
        } else {
            this.hideSnapshot(player);
        }
    }

    private void showSnapshot(Player player, FurnitureSnapshotState current) {
        if (this.shownSnapshot == current) return;
        if (this.shownSnapshot != null) {
            this.shownSnapshot.hideHitboxes(player);
        }
        List<FurnitureElement> visible = this.updateVisibleElements(player, current.elements());
        current.showHitboxes(player);
        this.visibleElements = visible;
        this.shownSnapshot = current;
    }

    private List<FurnitureElement> updateVisibleElements(Player player, List<FurnitureElement> elements) {
        // 单元素首次显示和一对一切换直接比较，不构造匹配 Map 和可变列表。
        if (elements.size() == 1 && this.visibleElements.size() <= 1) {
            FurnitureElement element = elements.getFirst();
            FurnitureElement previous = this.visibleElements.isEmpty() ? null : this.visibleElements.getFirst();
            if (!element.canSee(player)) {
                if (previous != null) previous.hide(player);
                return List.of();
            }
            if (element instanceof TransformableFurnitureElement transformable
                    && previous instanceof TransformableFurnitureElement old
                    && old.entityId() == transformable.entityId()
                    && old.getClass() == transformable.getClass()) {
                transformable.update(player, old);
            } else {
                if (previous != null) previous.hide(player);
                this.showVisibleElement(player, element);
            }
            return List.of(element);
        }
        // 首次显示（含剔除后重新显示）没有旧元素需要匹配。
        if (this.visibleElements.isEmpty()) {
            if (elements.isEmpty()) return List.of();
            List<FurnitureElement> visible = new ObjectArrayList<>(elements.size());
            for (FurnitureElement element : elements) {
                if (!element.canSee(player)) continue;
                this.showVisibleElement(player, element);
                visible.add(element);
            }
            return visible;
        }
        Int2ObjectOpenHashMap<TransformableFurnitureElement> previousElements = new Int2ObjectOpenHashMap<>();
        for (FurnitureElement element : this.visibleElements) {
            if (element instanceof TransformableFurnitureElement transformable) {
                previousElements.put(transformable.entityId(), transformable);
            } else {
                element.hide(player);
            }
        }
        List<FurnitureElement> visible = new ObjectArrayList<>(elements.size());
        for (FurnitureElement element : elements) {
            if (!element.canSee(player)) continue;
            TransformableFurnitureElement previous = element instanceof TransformableFurnitureElement transformable ? previousElements.remove(transformable.entityId()) : null;
            if (previous != null && previous.getClass() == element.getClass()) {
                ((TransformableFurnitureElement) element).update(player, previous);
            } else {
                if (previous != null) {
                    previous.hide(player);
                }
                this.showVisibleElement(player, element);
            }
            visible.add(element);
        }
        for (TransformableFurnitureElement element : previousElements.values()) {
            element.hide(player);
        }
        return visible;
    }

    // 调用方已判断 canSee，条件元素不再重复构造上下文。
    private void showVisibleElement(Player player, FurnitureElement element) {
        if (element instanceof ConditionalFurnitureElement conditional) {
            conditional.showInternal(player);
        } else {
            element.show(player);
        }
    }

    @Override
    public void show(Player player) {
        // 剔除运行在独立线程，只提交通知，不在这里修改玩家已显示的状态
        player.sendCustomPacket(new ClientboundFurnitureUpdatePacket(this.furniture.entityId()));
    }

    @Override
    public void hide(Player player) {
        // 剔除运行在独立线程，只提交通知，不在这里修改玩家已显示的状态
        player.sendCustomPacket(new ClientboundFurnitureUpdatePacket(this.furniture.entityId()));
    }

    private void hideSnapshot(Player player) {
        if (this.shownSnapshot == null) return;
        for (FurnitureElement element : this.visibleElements) {
            element.hide(player);
        }
        this.shownSnapshot.hideHitboxes(player);
        this.visibleElements = List.of();
        this.shownSnapshot = null;
    }

    @Override
    public CullingData cullingData() {
        return this.furniture.cullingData();
    }

    @Override
    public boolean handleEntitiesRemove(Player player, int entityId, IntList entityIds) {
        player.removeTrackedEntity(this.furniture.entityId());
        this.hideSnapshot(player);
        if (this.appliedSnapshot != null) {
            this.furniture.controller.onAsyncPlayerUntrack(player, this.appliedSnapshot);
            this.appliedSnapshot = null;
        }
        return true;
    }

    @Override
    public void handleSyncEntityPosition(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        event.setCancelled(true);
    }

    @Override
    public void handleMove(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        event.setCancelled(true);
    }
}
