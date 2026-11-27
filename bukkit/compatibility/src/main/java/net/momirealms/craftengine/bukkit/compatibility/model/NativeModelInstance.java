package net.momirealms.craftengine.bukkit.compatibility.model;

import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.bedrock.BedrockEntitySettings;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class NativeModelInstance<T, V> {
    protected final T source;
    final UUID id = UUID.randomUUID();
    private String identifier;
    NativeModelPayload.Snapshot snapshot;
    private Key key;
    int visibilityPhase;
    int visibilityInterval;

    protected NativeModelInstance(T source) {
        this.source = source;
    }

    protected abstract String name();

    protected abstract boolean removed();

    protected boolean prepare() {
        return true;
    }

    protected abstract boolean visible(V viewer);

    protected Set<UUID> trackedViewers() {
        return null;
    }

    protected abstract void nativeDisplays(Player player, V viewer, List<Integer> previous, List<Integer> current);

    protected abstract NativeModelPayload.Snapshot sample(BedrockModelIntegration mode, NativeModelParts parts, boolean checkVisibility);

    final BedrockModelIntegration mode(String namespace, BedrockEntitySettings settings) {
        String name = this.name();
        if (this.key == null || !name.equals(this.key.value())) {
            this.key = Key.of(namespace, name);
            this.identifier = this.key.asString();
        }
        return settings.mode(this.key);
    }

    protected final NativeModelPayload.DisplayBones displayBones(List<Integer> displays) {
        if (this.snapshot instanceof NativeModelPayload.DisplayBones old && old.model().equals(this.identifier) && old.displays() == displays) {
            return old;
        }
        return new NativeModelPayload.DisplayBones(this.id, this.identifier, displays);
    }

    protected final NativeModelPayload.Pose pose(double x, double y, double z, float yaw, float pitch) {
        if (this.snapshot instanceof NativeModelPayload.NativeState previous) {
            NativeModelPayload.Pose pose = previous.pose();
            if (Double.compare(pose.x(), x) == 0
                    && Double.compare(pose.y(), y) == 0
                    && Double.compare(pose.z(), z) == 0
                    && Float.compare(pose.yaw(), yaw) == 0
                    && Float.compare(pose.pitch(), pitch) == 0) {
                return pose;
            }
        }
        return new NativeModelPayload.Pose(x, y, z, yaw, pitch);
    }

    protected final NativeModelPayload.Scale scale(float x, float y, float z) {
        if (this.snapshot instanceof NativeModelPayload.NativeState previous) {
            NativeModelPayload.Scale scale = previous.scale();
            if (Float.compare(scale.x(), x) == 0 && Float.compare(scale.y(), y) == 0 && Float.compare(scale.z(), z) == 0) {
                return scale;
            }
        }
        return new NativeModelPayload.Scale(x, y, z);
    }

    protected final NativeModelPayload.NativeState nativeState(NativeModelParts parts,
                                                               int target,
                                                               boolean hurt,
                                                               int color,
                                                               NativeModelPayload.Pose pose,
                                                               NativeModelPayload.Scale scale,
                                                               List<String> hiddenBones,
                                                               List<Integer> displays,
                                                               List<UUID> anchors) {
        NativeModelPayload.NativeState old = this.snapshot instanceof NativeModelPayload.NativeState state ? state : null;
        List<NativeModelPayload.Animation> playing = parts.animations(old == null ? List.of() : old.animations());
        return this.nativeState(target, hurt, color, pose, scale, playing, hiddenBones, displays, anchors);
    }

    protected final NativeModelPayload.NativeState nativeState(int target,
                                                               boolean hurt,
                                                               int color,
                                                               NativeModelPayload.Pose pose,
                                                               NativeModelPayload.Scale scale,
                                                               List<NativeModelPayload.Animation> playing,
                                                               List<String> hiddenBones,
                                                               List<Integer> displays,
                                                               List<UUID> anchors) {
        NativeModelPayload.NativeState old = this.snapshot instanceof NativeModelPayload.NativeState state ? state : null;
        if (old != null
                && old.model().equals(this.identifier)
                && old.target() == target
                && old.hurt() == hurt
                && old.color() == color
                && old.pose() == pose
                && old.scale() == scale
                && old.animations() == playing
                && old.hiddenBones() == hiddenBones
                && old.displays() == displays
                && old.anchors() == anchors) {
            return old;
        }
        return new NativeModelPayload.NativeState(this.id, this.identifier, target, hurt, pose, scale, color, playing, hiddenBones, displays, anchors);
    }
}
