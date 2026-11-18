package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.RunningAnimation;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.event.CloseTrackerEvent;
import kr.toxicity.model.api.event.CreateDummyTrackerEvent;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;
import kr.toxicity.model.api.event.ModelEventListener;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.Tracker;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.bukkit.compatibility.model.NativeModelBridge;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import net.momirealms.craftengine.core.plugin.config.Config;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;

public final class BetterModelNativeBridge extends NativeModelBridge {
    // BetterModel compares trackers by model name, but each spawned tracker needs its own instance ID.
    private final Map<Tracker, UUID> trackers = Collections.synchronizedMap(new IdentityHashMap<>());
    private final List<ModelEventListener> listeners = new ArrayList<>();
    private final Field tint;

    public BetterModelNativeBridge(BukkitCraftEngine plugin, Plugin owner) throws ReflectiveOperationException {
        super(plugin, owner);
        this.tint = RenderedBone.class.getDeclaredField("tint");
        this.tint.setAccessible(true);
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CreateEntityTrackerEvent.class, event -> this.trackers.put(event.tracker(), UUID.randomUUID())));
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CreateDummyTrackerEvent.class, event -> this.trackers.put(event.tracker(), UUID.randomUUID())));
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CloseTrackerEvent.class, event -> this.trackers.remove(event.tracker())));
        for (EntityTrackerRegistry registry : EntityTrackerRegistry.registries()) {
            for (EntityTracker tracker : registry.trackers()) {
                this.trackers.putIfAbsent(tracker, UUID.randomUUID());
            }
        }
    }

    @Override
    protected boolean enabled() {
        return Config.bedrockBetterModelIntegration() == BedrockModelIntegration.CUSTOM_ENTITY;
    }

    @Override
    protected List<ModelState> collect() {
        synchronized (this.trackers) {
            return this.collectTrackers();
        }
    }

    private List<ModelState> collectTrackers() {
        List<ModelState> result = new ArrayList<>();
        this.trackers.entrySet().removeIf(entry -> entry.getKey().isClosed());
        for (Map.Entry<Tracker, UUID> entry : this.trackers.entrySet()) {
            Tracker tracker = entry.getKey();
            PlatformLocation location = tracker.location();
            ModelRotation rotation = tracker.rotation();
            float scale = tracker.scaler().scale(tracker);
            Map<String, NativeModelPayload.Animation> animations = new TreeMap<>();
            List<String> hidden = new ArrayList<>();
            List<Integer> displays = new ArrayList<>();
            int color = -1;
            for (RenderedBone bone : tracker.bones()) {
                RunningAnimation running = bone.runningAnimation();
                if (running != null && tracker.renderer().animations().containsKey(running.name())) {
                    int mode = switch (running.type()) {
                        case PLAY_ONCE -> NativeModelPayload.Animation.ONCE;
                        case LOOP -> NativeModelPayload.Animation.LOOP;
                        case HOLD_ON_LAST -> NativeModelPayload.Animation.HOLD;
                    };
                    animations.putIfAbsent(running.name(), new NativeModelPayload.Animation(running.name(), mode));
                }
                if (bone.getDisplay() != null) {
                    displays.add(bone.getDisplay().id());
                    if (bone.getDisplay().invisible()) {
                        hidden.add(bone.name().rawName());
                    }
                    if (color == -1) {
                        try {
                            color = this.tint.getInt(bone) & 0xffffff;
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException(exception);
                        }
                    }
                }
            }
            int target = tracker instanceof EntityTracker entity ? entity.sourceEntity().id() : 0;
            NativeModelPayload state = new NativeModelPayload(entry.getValue(), "bettermodel:" + tracker.name(), target, false,
                    location.x(), location.y(), location.z(), rotation.y(), rotation.x(), scale, scale, scale, color,
                    List.copyOf(animations.values()), hidden.stream().sorted().toList(), displays.stream().sorted().toList());
            result.add(new ModelState(state, viewer -> tracker.isSpawned(viewer.uuid()) && !tracker.isHide(BukkitAdapter.adapt((org.bukkit.entity.Player) viewer.platformPlayer()))));
        }
        return result;
    }

    @Override
    protected void close() {
        this.listeners.forEach(ModelEventListener::unregister);
        this.trackers.clear();
    }
}
