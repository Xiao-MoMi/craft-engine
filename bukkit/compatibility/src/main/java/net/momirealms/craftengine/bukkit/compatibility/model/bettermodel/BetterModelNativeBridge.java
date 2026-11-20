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
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public final class BetterModelNativeBridge extends NativeModelBridge {
    // BetterModel compares trackers by model name, but each spawned tracker needs its own instance ID.
    private final Map<Tracker, TrackerState> trackers = Collections.synchronizedMap(new IdentityHashMap<>());
    private final List<ModelEventListener> listeners = new ArrayList<>();
    private final Field tint;
    private final Field previousTint;
    private final Field damageTintTicks;

    public BetterModelNativeBridge(BukkitCraftEngine plugin, Plugin owner) throws ReflectiveOperationException {
        super(plugin, owner);
        this.tint = RenderedBone.class.getDeclaredField("tint");
        this.tint.setAccessible(true);
        this.previousTint = RenderedBone.class.getDeclaredField("previousTint");
        this.previousTint.setAccessible(true);
        this.damageTintTicks = EntityTracker.class.getDeclaredField("damageTint");
        this.damageTintTicks.setAccessible(true);
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CreateEntityTrackerEvent.class, event -> this.trackers.put(event.tracker(), new TrackerState())));
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CreateDummyTrackerEvent.class, event -> this.trackers.put(event.tracker(), new TrackerState())));
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CloseTrackerEvent.class, event -> this.trackers.remove(event.tracker())));
        for (EntityTrackerRegistry registry : EntityTrackerRegistry.registries()) {
            for (EntityTracker tracker : registry.trackers()) {
                this.trackers.putIfAbsent(tracker, new TrackerState());
            }
        }
    }

    @Override
    protected boolean enabled() {
        return Config.bedrockEntities().uses("bettermodel", BedrockModelIntegration.CUSTOM_ENTITY) || Config.bedrockEntities().uses("bettermodel", BedrockModelIntegration.DISPLAY_ENTITY);
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
        for (Map.Entry<Tracker, TrackerState> entry : this.trackers.entrySet()) {
            Tracker tracker = entry.getKey();
            TrackerState instance = entry.getValue();
            Key id = Key.of("bettermodel", tracker.name());
            BedrockModelIntegration integration = Config.bedrockEntities().mode(id);
            if (integration == BedrockModelIntegration.DISABLED) {
                continue;
            }
            Predicate<Player> visible = viewer -> tracker.isSpawned(viewer.uuid()) && !tracker.isHide(BukkitAdapter.adapt((org.bukkit.entity.Player) viewer.platformPlayer()));
            if (integration == BedrockModelIntegration.DISPLAY_ENTITY) {
                List<Integer> displays = new ArrayList<>();
                for (RenderedBone bone : tracker.bones()) {
                    if (bone.getDisplay() != null) {
                        displays.add(bone.getDisplay().id());
                    }
                }
                result.add(new ModelState(new NativeModelPayload.DisplayBones(instance.id, id.asString(), displays.stream().sorted().toList()), visible));
                continue;
            }
            PlatformLocation location = tracker.location();
            ModelRotation rotation = tracker.rotation();
            float scale = tracker.scaler().scale(tracker);
            Map<String, NativeModelPayload.Animation> animations = new TreeMap<>();
            List<String> hidden = new ArrayList<>();
            List<Integer> displays = new ArrayList<>();
            EntityTracker entity = tracker instanceof EntityTracker attached ? attached : null;
            boolean hurt = entity != null && this.isHurt(entity);
            int damageColor = hurt ? entity.damageTintValue() & 0xffffff : -1;
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
                        color = this.normalTint(bone, instance, damageColor);
                    }
                }
            }
            int target = entity != null ? entity.sourceEntity().id() : 0;
            NativeModelPayload.NativeState state = new NativeModelPayload.NativeState(instance.id, id.asString(), target, hurt,
                    new NativeModelPayload.Pose(location.x(), location.y(), location.z(), rotation.y(), rotation.x()),
                    new NativeModelPayload.Scale(scale, scale, scale), color,
                    List.copyOf(animations.values()), hidden.stream().sorted().toList(), displays.stream().sorted().toList());
            result.add(new ModelState(state, visible));
        }
        return result;
    }

    private boolean isHurt(EntityTracker tracker) {
        try {
            return ((AtomicLong) this.damageTintTicks.get(tracker)).get() >= 0;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int normalTint(RenderedBone bone, TrackerState instance, int damageColor) {
        try {
            int current = this.tint.getInt(bone) & 0xffffff;
            // BetterModel 替换骨骼颜色时会保存原色。两种颜色相同时它会跳过写入，此时 previousTint 可能属于更早的一次染色。
            if (current == damageColor && instance.normalTint != damageColor) {
                current = this.previousTint.getInt(bone) & 0xffffff;
            }
            instance.normalTint = current;
            return current;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class TrackerState {
        private final UUID id = UUID.randomUUID();
        private int normalTint = -1;
    }

    @Override
    protected void close() {
        this.listeners.forEach(ModelEventListener::unregister);
        this.trackers.clear();
    }
}
