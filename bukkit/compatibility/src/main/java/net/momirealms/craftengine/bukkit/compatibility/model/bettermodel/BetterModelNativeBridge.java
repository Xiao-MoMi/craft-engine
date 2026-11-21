package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.RunningAnimation;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.event.CloseTrackerEvent;
import kr.toxicity.model.api.event.CreateDummyTrackerEvent;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;
import kr.toxicity.model.api.event.ModelEventListener;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.Tracker;
import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.bukkit.compatibility.model.NativeModelBridge;
import net.momirealms.craftengine.bukkit.compatibility.model.NativeModelInstance;
import net.momirealms.craftengine.bukkit.compatibility.model.NativeModelParts;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class BetterModelNativeBridge extends NativeModelBridge<Tracker, PlatformPlayer> {
    private final List<ModelEventListener> listeners = new ArrayList<>();
    private final Field tint;
    private final Field previousTint;
    private final Field damageTintTicks;

    public BetterModelNativeBridge(BukkitCraftEngine plugin, Plugin owner) throws ReflectiveOperationException {
        super(plugin, owner, "bettermodel");
        this.tint = RenderedBone.class.getDeclaredField("tint");
        this.tint.setAccessible(true);
        this.previousTint = RenderedBone.class.getDeclaredField("previousTint");
        this.previousTint.setAccessible(true);
        this.damageTintTicks = EntityTracker.class.getDeclaredField("damageTint");
        this.damageTintTicks.setAccessible(true);
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CreateEntityTrackerEvent.class, event -> this.register(event.tracker())));
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CreateDummyTrackerEvent.class, event -> this.register(event.tracker())));
        this.listeners.add(BetterModel.eventBus().subscribe(plugin.javaPlugin()::isEnabled, CloseTrackerEvent.class, event -> this.unregister(event.tracker())));
        for (EntityTrackerRegistry registry : EntityTrackerRegistry.registries()) {
            for (EntityTracker tracker : registry.trackers()) {
                this.register(tracker);
            }
        }
    }

    @Override
    protected NativeModelInstance<Tracker, PlatformPlayer> create(Tracker tracker) {
        return new TrackedModel(tracker);
    }

    @Override
    protected PlatformPlayer adaptViewer(Player player) {
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.platformPlayer();
        return bukkitPlayer == null ? null : BukkitAdapter.adapt(bukkitPlayer);
    }

    private final class TrackedModel extends NativeModelInstance<Tracker, PlatformPlayer> {
        private RenderedBone[] bones;
        private List<Integer> displays;
        private BoneState state;
        private int normalTint = -1;

        private TrackedModel(Tracker tracker) {
            super(tracker);
        }

        @Override
        protected String name() {
            return this.source.name();
        }

        @Override
        protected boolean removed() {
            return this.source.isClosed();
        }

        @Override
        protected boolean visible(PlatformPlayer viewer) {
            return viewer != null && this.source.isSpawned(viewer.uuid()) && !this.source.isHide(viewer);
        }

        @Override
        protected NativeModelPayload.Snapshot sample(BedrockModelIntegration mode, NativeModelParts parts, boolean checkVisibility) {
            if (this.bones == null) {
                // BetterModel 在构建 tracker 的 render pipeline 时固定骨骼及其 display；新 tracker 会创建独立缓存。
                this.bones = this.source.bones().toArray(RenderedBone[]::new);
                List<Integer> ids = new ArrayList<>();
                for (RenderedBone bone : this.bones) {
                    if (bone.getDisplay() != null) {
                        ids.add(bone.getDisplay().id());
                    }
                }
                ids.sort(null);
                this.displays = List.copyOf(ids);
            }
            if (mode == BedrockModelIntegration.DISPLAY_ENTITY) {
                return this.displayBones(this.displays);
            }

            if (this.state == null) {
                this.state = new BoneState(this.bones, this.displays.size(), this.source.renderer().animations().keySet());
            }
            this.state.readAnimations(parts);
            if (checkVisibility) {
                this.state.readVisibility();
            }
            EntityTracker entity = this.source instanceof EntityTracker attached ? attached : null;
            boolean hurt = entity != null && BetterModelNativeBridge.this.isHurt(entity);
            int damageColor = hurt ? entity.damageTintValue() & 0xffffff : -1;
            int color = this.state.tintBone == null ? -1 : BetterModelNativeBridge.this.normalTint(this.state.tintBone, this, damageColor);
            PlatformLocation location = this.source.location();
            ModelRotation rotation = this.source.rotation();
            float scale = this.source.scaler().scale(this.source);
            int target = entity == null ? 0 : entity.sourceEntity().id();
            NativeModelPayload.Pose pose = this.pose(location.x(), location.y(), location.z(), rotation.y(), rotation.x());
            return this.nativeState(target, hurt, color, pose, this.scale(scale, scale, scale),
                    this.state.animations, this.state.hiddenBones, this.displays);
        }
    }

    private static final class BoneState {
        private final RenderedBone[] bones;
        private final RunningAnimation[] running;
        private final Set<String> animationNames;
        private List<NativeModelPayload.Animation> animations = List.of();
        private final ModelDisplay[] displays;
        private final String[] displayNames;
        private final boolean[] invisible;
        private List<String> hiddenBones = List.of();
        private final RenderedBone tintBone;

        private BoneState(RenderedBone[] bones, int displayCount, Set<String> animationNames) {
            this.bones = bones;
            this.running = new RunningAnimation[bones.length];
            this.animationNames = animationNames;
            this.displays = new ModelDisplay[displayCount];
            this.displayNames = new String[displayCount];
            this.invisible = new boolean[displayCount];
            RenderedBone first = null;
            int index = 0;
            for (RenderedBone bone : bones) {
                ModelDisplay display = bone.getDisplay();
                if (display != null) {
                    if (first == null) {
                        first = bone;
                    }
                    this.displays[index] = display;
                    this.displayNames[index++] = bone.name().rawName();
                }
            }
            this.tintBone = first;
        }

        private void readAnimations(NativeModelParts parts) {
            boolean changed = false;
            for (int i = 0; i < this.bones.length; i++) {
                RunningAnimation current = this.bones[i].runningAnimation();
                if (this.running[i] != current) {
                    this.running[i] = current;
                    changed = true;
                }
            }
            if (!changed) {
                return;
            }
            for (RunningAnimation current : this.running) {
                if (current != null && this.animationNames.contains(current.name())) {
                    int loop = switch (current.type()) {
                        case PLAY_ONCE -> NativeModelPayload.Animation.ONCE;
                        case LOOP -> NativeModelPayload.Animation.LOOP;
                        case HOLD_ON_LAST -> NativeModelPayload.Animation.HOLD;
                    };
                    parts.animate(current.name(), loop);
                }
            }
            this.animations = parts.animations(this.animations);
        }

        private void readVisibility() {
            boolean changed = false;
            for (int i = 0; i < this.displays.length; i++) {
                boolean hidden = this.displays[i].invisible();
                if (this.invisible[i] != hidden) {
                    this.invisible[i] = hidden;
                    changed = true;
                }
            }
            if (changed) {
                List<String> hidden = new ArrayList<>();
                for (int i = 0; i < this.invisible.length; i++) {
                    if (this.invisible[i]) {
                        hidden.add(this.displayNames[i]);
                    }
                }
                hidden.sort(null);
                this.hiddenBones = List.copyOf(hidden);
            }
        }
    }

    private boolean isHurt(EntityTracker tracker) {
        try {
            return ((AtomicLong) this.damageTintTicks.get(tracker)).get() >= 0;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int normalTint(RenderedBone bone, TrackedModel instance, int damageColor) {
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

    @Override
    protected void close() {
        this.listeners.forEach(ModelEventListener::unregister);
        this.listeners.clear();
    }
}
