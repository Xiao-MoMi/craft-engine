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
import java.util.function.BiConsumer;

public final class ModelEngineNativeBridge extends NativeModelBridge<ActiveModel, UUID> {
    private final List<String> hiddenNames = new ArrayList<>();
    private final IntArrayList displayIds = new IntArrayList();
    private final BiConsumer<Integer, DisplayBone.BoneData> appendDisplayId = (key, data) -> this.displayIds.add(data.getId());

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

    private final class TrackedModel extends NativeModelInstance<ActiveModel, UUID> {
        private int[] displayOrder = new int[0];
        private List<Integer> displays = List.of();
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
        protected NativeModelPayload.Snapshot sample(BedrockModelIntegration mode, NativeModelParts parts, boolean checkVisibility) {
            this.readBones(checkVisibility);
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
                    pose, this.scale(scale.x(), scale.y(), scale.z()), this.hiddenBones, this.displays);
        }

        private void readBones(boolean checkVisibility) {
            IntArrayList ids = ModelEngineNativeBridge.this.displayIds;
            ids.clear();
            List<String> hidden = ModelEngineNativeBridge.this.hiddenNames;
            if (checkVisibility) {
                hidden.clear();
            }
            if (this.source.getModelRenderer() instanceof DisplayRenderer renderer) {
                for (DisplayBone bone : renderer.getRendered().values()) {
                    bone.getModel().forEach(ModelEngineNativeBridge.this.appendDisplayId);
                    if (checkVisibility && !bone.getVisibility().get()) {
                        hidden.add(bone.getBoneId());
                    }
                }
            }
            if (checkVisibility && !hidden.equals(this.hiddenOrder)) {
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
    }
}
