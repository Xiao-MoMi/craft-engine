package net.momirealms.craftengine.bedrock.network;

import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

import java.util.List;
import java.util.UUID;

public sealed interface NativeModelPayload {
    String CHANNEL = "craftengine:native_model";
    PacketCodec<NativeModelPayload> CODEC = new NativeModelCodec();

    UUID instance();

    sealed interface Snapshot extends NativeModelPayload {
        String model();

        NativeModelPayload remove();
    }

    record DisplayBones(UUID instance, String model, List<Integer> displays) implements Snapshot {
        @Override
        public RemoveDisplay remove() {
            return new RemoveDisplay(this.instance);
        }
    }

    record RemoveDisplay(UUID instance) implements NativeModelPayload {
    }

    sealed interface NativeMessage extends NativeModelPayload {
    }

    record RemoveNative(UUID instance) implements NativeMessage {
    }

    record Pose(double x, double y, double z, float yaw, float pitch) {
    }

    record Scale(float x, float y, float z) {
    }

    record Animation(String name, int mode) {
        public static final int ONCE = 1;
        public static final int LOOP = 2;
        public static final int HOLD = 3;
    }

    record NativeState(UUID instance, String model, int target, boolean hurt, Pose pose, Scale scale,
                       int color, List<Animation> animations, List<String> hiddenBones, List<Integer> displays, List<UUID> anchors) implements Snapshot, NativeMessage {
        @Override
        public RemoveNative remove() {
            return new RemoveNative(this.instance);
        }

        public Delta changesSince(NativeState previous) {
            return new Delta(this.instance,
                    this.target == previous.target ? null : this.target,
                    this.hurt == previous.hurt ? null : this.hurt,
                    this.pose.equals(previous.pose) ? null : this.pose,
                    this.scale.equals(previous.scale) ? null : this.scale,
                    this.color == previous.color ? null : this.color,
                    this.animations.equals(previous.animations) ? null : this.animations,
                    this.hiddenBones.equals(previous.hiddenBones) ? null : this.hiddenBones,
                    this.displays.equals(previous.displays) ? null : this.displays,
                    this.anchors.equals(previous.anchors) ? null : this.anchors);
        }
    }

    // null 表示沿用上一次的值；空列表表示清除该组的全部条目。
    record Delta(UUID instance, Integer target, Boolean hurt, Pose pose, Scale scale, Integer color,
                 List<Animation> animations, List<String> hiddenBones, List<Integer> displays, List<UUID> anchors) implements NativeMessage {
        public NativeState applyTo(NativeState previous) {
            return new NativeState(
                    this.instance,
                    previous.model,
                    this.target == null ? previous.target : this.target,
                    this.hurt == null ? previous.hurt : this.hurt,
                    this.pose == null ? previous.pose : this.pose,
                    this.scale == null ? previous.scale : this.scale,
                    this.color == null ? previous.color : this.color,
                    this.animations == null ? previous.animations : this.animations,
                    this.hiddenBones == null ? previous.hiddenBones : this.hiddenBones,
                    this.displays == null ? previous.displays : this.displays,
                    this.anchors == null ? previous.anchors : this.anchors
            );
        }
    }
}
