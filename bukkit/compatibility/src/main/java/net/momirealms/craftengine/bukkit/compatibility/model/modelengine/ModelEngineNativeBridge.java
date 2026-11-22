package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.events.AddModelEvent;
import com.ticxo.modelengine.api.events.RemoveModelEvent;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.render.DisplayBone;
import com.ticxo.modelengine.api.model.render.DisplayRenderer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.bukkit.compatibility.model.NativeModelBridge;
import net.momirealms.craftengine.bukkit.compatibility.model.NativeModelInstance;
import net.momirealms.craftengine.bukkit.compatibility.model.NativeModelParts;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.joml.Vector3fc;

import java.util.*;
import java.util.function.Consumer;

public final class ModelEngineNativeBridge extends NativeModelBridge<ActiveModel, UUID> {
    private final List<String> hiddenNames = new ArrayList<>();
    private final IntArrayList displayIds = new IntArrayList();
    private final Consumer<DisplayBone.BoneData> appendDisplayId = data -> this.displayIds.add(data.getId());

    public ModelEngineNativeBridge(BukkitCraftEngine plugin, Plugin owner) {
        super(plugin, owner, "modelengine");
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                ModeledEntity modeled = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
                if (modeled != null) {
                    for (ActiveModel model : modeled.getModels().values()) {
                        this.register(model);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModel(AddModelEvent event) {
        this.register(event.getModel());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemoveModel(RemoveModelEvent event) {
        this.unregister(event.getModel());
    }

    @Override
    protected NativeModelInstance<ActiveModel, UUID> create(ActiveModel model) {
        return new TrackedModel(model);
    }

    @Override
    protected UUID adaptViewer(Player player) {
        return player.uuid();
    }

    private final class TrackedModel extends NativeModelInstance<ActiveModel, UUID> implements Consumer<DisplayBone> {
        private final List<Integer> singleDisplayKeys = new ArrayList<>();
        private int singleDisplayIndex;
        private boolean checkVisibility;
        private int[] displayOrder = new int[0];
        private List<Integer> displays = List.of();
        private List<UUID> anchors = List.of();
        private List<String> hiddenOrder = List.of();
        private List<String> hiddenBones = List.of();
        private ModeledEntity modeled;
        private Map<UUID, CullType> tracking;

        private TrackedModel(ActiveModel model) {
            super(model);
        }

        @Override
        protected String name() {
            return this.source.getBlueprint().getName();
        }

        @Override
        protected boolean removed() {
            return this.source.isDestroyed() || this.source.isRemoved();
        }

        @Override
        protected boolean prepare() {
            this.modeled = this.source.getModeledEntity();
            if (this.modeled == null || this.modeled.isDestroyed()) {
                return false;
            }
            this.tracking = this.modeled.getBase().getData().getTracking();
            return true;
        }

        @Override
        protected boolean visible(UUID viewer) {
            CullType cull = this.tracking.get(viewer);
            return cull != null && cull != CullType.CULLED;
        }

        @Override
        protected void nativeDisplays(Player player, UUID viewer, List<Integer> previous, List<Integer> current) {
            ModelEngineDisplayFilter.update(player.nettyChannel(), previous, current);
        }

        @Override
        protected NativeModelPayload.Snapshot sample(BedrockModelIntegration mode, NativeModelParts parts, boolean checkVisibility) {
            this.readBones(mode, checkVisibility);
            if (mode == BedrockModelIntegration.DISPLAY_ENTITY) {
                return this.displayBones(this.displays);
            }
            AnimationHandler handler = this.source.getAnimationHandler();
            for (String name : this.source.getBlueprint().getAnimations().keySet()) {
                IAnimationProperty property = handler.getAnimation(name);
                if (property != null && handler.isPlayingAnimation(name)) {
                    int loop = switch (property.getLoopMode()) {
                        case ONCE -> NativeModelPayload.Animation.ONCE;
                        case LOOP -> NativeModelPayload.Animation.LOOP;
                        case HOLD -> NativeModelPayload.Animation.HOLD;
                    };
                    parts.animate(name, loop);
                }
            }
            Location location = this.modeled.getBase().getLocation();
            Vector3fc scale = this.source.getScale();
            NativeModelPayload.Pose pose = this.pose(location.getX(), location.getY(), location.getZ(), this.source.getYBodyRot(), this.source.getXHeadRot());
            return this.nativeState(parts, this.modeled.getBase().getEntityId(), this.source.isMarkedHurt(), this.source.getDefaultTint().asRGB(),
                    pose, this.scale(scale.x(), scale.y(), scale.z()), this.hiddenBones, this.displays, this.anchors);
        }

        private void readBones(BedrockModelIntegration mode, boolean checkVisibility) {
            IntArrayList ids = ModelEngineNativeBridge.this.displayIds;
            ids.clear();
            List<String> hidden = ModelEngineNativeBridge.this.hiddenNames;
            if (checkVisibility) {
                hidden.clear();
            }
            if (this.source.getModelRenderer() instanceof DisplayRenderer renderer) {
                if (mode == BedrockModelIntegration.NATIVE_ENTITY) {
                    UUID pivot = renderer.getPivot().getUuid();
                    UUID hitboxPivot = renderer.getHitbox().getPivotUuid();
                    if (this.anchors.isEmpty() || !pivot.equals(this.anchors.get(0)) || !hitboxPivot.equals(this.anchors.get(1))) {
                        this.anchors = List.of(pivot, hitboxPivot);
                    }
                }
                this.singleDisplayIndex = 0;
                this.checkVisibility = checkVisibility;
                renderer.getRendered().values().forEach(this);
            } else {
                this.anchors = List.of();
            }
            if (checkVisibility && !this.sameHiddenOrder(hidden)) {
                this.hiddenOrder = List.copyOf(hidden);
                hidden.sort(null);
                if (!hidden.equals(this.hiddenBones)) {
                    this.hiddenBones = List.copyOf(hidden);
                }
            }
            // 保留上次的渲染器迭代顺序，骨骼数量相同但 ID 被替换时也会重建快照。
            if (!Arrays.equals(this.displayOrder, 0, this.displayOrder.length, ids.elements(), 0, ids.size())) {
                this.displayOrder = ids.toIntArray();
                Arrays.sort(ids.elements(), 0, ids.size());
                if (!ids.equals(this.displays)) {
                    this.displays = List.copyOf(ids);
                }
            }
        }

        private boolean sameHiddenOrder(List<String> hidden) {
            if (this.hiddenOrder.size() != hidden.size()) {
                return false;
            }
            for (int i = 0; i < hidden.size(); i++) {
                if (!this.hiddenOrder.get(i).equals(hidden.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void accept(DisplayBone bone) {
            Map<Integer, DisplayBone.BoneData> models = bone.getModel();
            if (models.size() == 1) {
                this.collectSingleDisplay(models);
            } else if (!models.isEmpty()) {
                models.values().forEach(ModelEngineNativeBridge.this.appendDisplayId);
            }
            if (this.checkVisibility && !bone.getVisibility().get()) {
                ModelEngineNativeBridge.this.hiddenNames.add(bone.getBoneId());
            }
        }

        private void collectSingleDisplay(Map<Integer, DisplayBone.BoneData> models) {
            // 缓存查找键，每轮仍读当前 BoneData。同键替换 display 或骨骼遍历顺序变化时都能取到新 ID。
            int index = this.singleDisplayIndex++;
            if (index == this.singleDisplayKeys.size()) {
                this.singleDisplayKeys.add(null);
            }
            Integer key = this.singleDisplayKeys.get(index);
            DisplayBone.BoneData current = key == null ? null : models.get(key);
            if (current != null) {
                ModelEngineNativeBridge.this.displayIds.add(current.getId());
                return;
            }
            for (Map.Entry<Integer, DisplayBone.BoneData> entry : models.entrySet()) {
                this.singleDisplayKeys.set(index, entry.getKey());
                ModelEngineNativeBridge.this.displayIds.add(entry.getValue().getId());
            }
        }
    }
}
