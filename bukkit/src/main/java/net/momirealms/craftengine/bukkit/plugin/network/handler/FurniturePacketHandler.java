package net.momirealms.craftengine.bukkit.plugin.network.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.plugin.network.packet.ClientboundFurnitureUpdatePacket;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullableHolder;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.culling.ViewRangeCullable;
import net.momirealms.craftengine.core.entity.furniture.FurnitureSnapshotState;
import net.momirealms.craftengine.core.entity.furniture.element.ConditionalFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.TransformableFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.List;

public final class FurniturePacketHandler implements EntityPacketHandler, Cullable {
    public final BukkitFurniture furniture;
    // 已通知行为控制器的快照，与客户端是否显示无关。
    private FurnitureSnapshotState appliedSnapshot;
    // 记录客户端仍持有的元素，包括视距为 0 的元素；不重新求值旧条件来猜测客户端状态。
    private List<FurnitureElement> trackedElements = List.of();
    // 上次完成客户端同步的快照和家具剔除状态；两组实体更新完后统一写入。
    // 剔除期间展示元素可能仍属于旧变体，其实际对象由 trackedElements 保存。
    private FurnitureSnapshotState clientSnapshot;
    private boolean culled;

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
        boolean shown = true;
        if (Config.enableEntityCulling()) {
            CullableHolder holder = player.getTrackedEntity(this.furniture.entityId());
            shown = holder != null && holder.isShown;
        }
        boolean nextCulled = !shown;
        if (this.clientSnapshot == current && this.culled == nextCulled) return;

        // 两组实体共享剔除状态，但各自决定保留哪些实体。
        // 更新期间字段仍表示旧状态，确保清理旧碰撞组、恢复旧展示元素时判断正确。
        this.updateHitboxes(player, current, nextCulled);
        if (nextCulled) {
            this.cullElements(player);
        } else {
            this.showElements(player, current);
        }
        this.clientSnapshot = current;
        this.culled = nextCulled;
    }

    /**
     * 将客户端碰撞实体调整到本次要求的状态。字段保存旧状态，参数表示本次目标状态。
     */
    private void updateHitboxes(Player player, FurnitureSnapshotState current, boolean culled) {
        boolean keepInvisible = Config.entityCullingKeepInvisibleHitboxes();
        if (this.clientSnapshot != current) {
            // 首次追踪或快照替换：先按旧状态清理，再按目标状态重新生成。
            // 必须先 clearHitboxes 再覆盖字段，否则会丢失清理旧实体所需的快照和保留状态。
            this.clearHitboxes(player);
            for (FurnitureHitBox hitbox : current.hitboxes()) {
                if (!culled) {
                    // 家具未被剔除，需要全部碰撞实体
                    hitbox.show(player);
                } else if (keepInvisible) {
                    // 即使家具从未显示过，也要生成应保留的隐身碰撞实体。
                    // showCulled 负责“只生成隐身部分”，不是把整个碰撞组生成后再隐藏。
                    hitbox.showCulled(player);
                }
                // 已剔除且不保留隐身实体时，不生成任何碰撞实体。
            }
            return;
        }
        // 快照和剔除状态都没变，不重复发送包。
        if (this.culled == culled) return;
        // 这里只剩同一快照的剔除/恢复切换，可以复用已保留的实体。
        for (FurnitureHitBox hitbox : current.hitboxes()) {
            if (culled) {
                // 进入剔除
                if (keepInvisible) {
                    hitbox.cull(player);
                } else {
                    hitbox.hide(player);
                }
            } else {
                // 退出剔除
                if (keepInvisible) {
                    hitbox.restore(player);
                } else {
                    hitbox.show(player);
                }
            }
        }
    }

    // 取消追踪或切换快照时，必须连同剔除期间保留的隐身实体一起移除。
    private void clearHitboxes(Player player) {
        if (this.clientSnapshot == null) return;
        if (!this.culled || Config.entityCullingKeepInvisibleHitboxes()) {
            this.clientSnapshot.hideHitboxes(player);
        }
    }

    private void showElements(Player player, FurnitureSnapshotState current) {
        // 同一快照从剔除恢复时也要重新判断显示条件，不能仅凭快照相同就跳过。
        if (this.culled) {
            for (FurnitureElement element : this.trackedElements) {
                if (element instanceof ViewRangeCullable cullable) {
                    cullable.setCulled(player, false);
                }
            }
        }
        this.trackedElements = this.updateVisibleElements(player, current.elements());
    }

    private List<FurnitureElement> updateVisibleElements(Player player, List<FurnitureElement> elements) {
        // 单元素首次显示和一对一切换直接比较，不构造匹配 Map 和可变列表。
        if (elements.size() == 1 && this.trackedElements.size() <= 1) {
            FurnitureElement element = elements.getFirst();
            FurnitureElement previous = this.trackedElements.isEmpty() ? null : this.trackedElements.getFirst();
            if (!element.canSee(player)) {
                if (previous != null) previous.hide(player);
                return List.of();
            }
            this.updateOrShowElement(player, element, previous);
            return List.of(element);
        }
        // 首次显示（含剔除后重新显示）没有旧元素需要匹配。
        if (this.trackedElements.isEmpty()) {
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
        for (FurnitureElement element : this.trackedElements) {
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
            this.updateOrShowElement(player, element, previous);
            visible.add(element);
        }
        for (TransformableFurnitureElement element : previousElements.values()) {
            element.hide(player);
        }
        return visible;
    }

    // 调用方已确认新元素可见。只有类型和实体 ID 都一致时，才能在原实体上更新。
    private void updateOrShowElement(Player player, FurnitureElement element, FurnitureElement previous) {
        if (element instanceof TransformableFurnitureElement current
                && previous instanceof TransformableFurnitureElement old
                && current.entityId() == old.entityId()
                && current.getClass() == old.getClass()) {
            current.update(player, old);
            return;
        }
        if (previous != null) previous.hide(player);
        this.showVisibleElement(player, element);
    }

    // 调用方已判断 canSee，条件元素不再重复构造上下文。
    private void showVisibleElement(Player player, FurnitureElement element) {
        if (element instanceof ConditionalFurnitureElement conditional) {
            conditional.showInternal(player);
        } else {
            element.show(player);
        }
    }

    private void cullElements(Player player) {
        if (this.culled || this.trackedElements.isEmpty()) return;
        if (!Config.entityCullingUpdateDisplayViewRange()) {
            this.hideElements(player);
            return;
        }
        // 只记录真正保留在客户端的展示实体，其余元素已移除，恢复时需要重新生成。
        List<FurnitureElement> retained = new ObjectArrayList<>(this.trackedElements.size());
        for (FurnitureElement element : this.trackedElements) {
            if (element instanceof ViewRangeCullable display) {
                display.setCulled(player, true);
                retained.add(element);
            } else {
                element.hide(player);
            }
        }
        this.trackedElements = retained;
    }

    private void hideElements(Player player) {
        for (FurnitureElement element : this.trackedElements) {
            element.hide(player);
        }
        this.trackedElements = List.of();
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

    @Override
    public CullingData cullingData() {
        return this.furniture.cullingData();
    }

    @Override
    public boolean handleEntitiesRemove(Player player, int entityId, IntList entityIds) {
        player.removeTrackedEntity(this.furniture.entityId());
        this.hideElements(player);
        this.clearHitboxes(player);
        this.clientSnapshot = null;
        this.culled = false;
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
