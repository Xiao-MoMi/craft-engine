package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.events.AddModelEvent;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.render.DisplayBone;
import com.ticxo.modelengine.api.model.render.DisplayRenderer;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.bukkit.compatibility.model.NativeModelBridge;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import net.momirealms.craftengine.core.plugin.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelEngineNativeBridge extends NativeModelBridge {
    private final Map<ActiveModel, UUID> models = new ConcurrentHashMap<>();

    public ModelEngineNativeBridge(BukkitCraftEngine plugin, Plugin owner) {
        super(plugin, owner);
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                ModeledEntity modeled = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
                if (modeled != null) {
                    for (ActiveModel model : modeled.getModels().values()) {
                        this.models.put(model, UUID.randomUUID());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModel(AddModelEvent event) {
        this.models.putIfAbsent(event.getModel(), UUID.randomUUID());
    }

    @Override
    protected boolean enabled() {
        return Config.bedrockModelEngineIntegration() == BedrockModelIntegration.CUSTOM_ENTITY;
    }

    @Override
    protected List<ModelState> collect() {
        List<ModelState> result = new ArrayList<>();
        this.models.entrySet().removeIf(entry -> entry.getKey().isDestroyed() || entry.getKey().isRemoved());
        for (Map.Entry<ActiveModel, UUID> entry : this.models.entrySet()) {
            ActiveModel model = entry.getKey();
            ModeledEntity modeled = model.getModeledEntity();
            if (modeled == null || modeled.isDestroyed()) {
                continue;
            }
            Location location = modeled.getBase().getLocation();
            Vector3fc scale = model.getScale();
            List<Integer> displays = new ArrayList<>();
            List<String> hidden = new ArrayList<>();
            if (model.getModelRenderer() instanceof DisplayRenderer renderer) {
                for (DisplayBone bone : renderer.getRendered().values()) {
                    for (DisplayBone.BoneData data : bone.getModel().values()) {
                        displays.add(data.getId());
                    }
                    if (!bone.getVisibility().get()) {
                        hidden.add(bone.getBoneId());
                    }
                }
            }
            int color = (model.isMarkedHurt() ? model.getDamageTint() : model.getDefaultTint()).asRGB();
            List<NativeModelPayload.Animation> animations = new ArrayList<>();
            for (String name : model.getBlueprint().getAnimations().keySet().stream().sorted().toList()) {
                IAnimationProperty property = model.getAnimationHandler().getAnimation(name);
                if (property != null && model.getAnimationHandler().isPlayingAnimation(name)) {
                    int mode = switch (property.getLoopMode()) {
                        case ONCE -> NativeModelPayload.Animation.ONCE;
                        case LOOP -> NativeModelPayload.Animation.LOOP;
                        case HOLD -> NativeModelPayload.Animation.HOLD;
                    };
                    animations.add(new NativeModelPayload.Animation(name, mode));
                }
            }
            NativeModelPayload state = new NativeModelPayload(entry.getValue(), "modelengine:" + model.getBlueprint().getName(), modeled.getBase().getEntityId(), false,
                    location.getX(), location.getY(), location.getZ(), model.getYBodyRot(), model.getXHeadRot(), scale.x(), scale.y(), scale.z(), color,
                    animations, hidden.stream().sorted().toList(), displays.stream().sorted().toList());
            result.add(new ModelState(state, viewer -> {
                CullType cull = modeled.getBase().getData().getTracking().get(viewer.uuid());
                return cull != null && cull != CullType.CULLED;
            }));
        }
        return result;
    }

    @Override
    protected void close() {
        this.models.clear();
    }
}
