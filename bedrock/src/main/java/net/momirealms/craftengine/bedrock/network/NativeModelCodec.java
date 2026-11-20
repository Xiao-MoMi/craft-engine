package net.momirealms.craftengine.bedrock.network;

import net.momirealms.craftengine.bedrock.network.NativeModelPayload.*;
import net.momirealms.craftengine.bedrock.network.codec.PacketCodec;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class NativeModelCodec implements PacketCodec<NativeModelPayload> {
    private static final int NATIVE_STATE = 0;
    private static final int NATIVE_DELTA = 1;
    private static final int DISPLAY_BONES = 2;
    private static final int REMOVE_NATIVE = 3;
    private static final int REMOVE_DISPLAY = 4;

    private static final int TARGET = 1;
    private static final int HURT = 1 << 1;
    private static final int POSE = 1 << 2;
    private static final int SCALE = 1 << 3;
    private static final int COLOR = 1 << 4;
    private static final int ANIMATIONS = 1 << 5;
    private static final int HIDDEN_BONES = 1 << 6;
    private static final int DISPLAYS = 1 << 7;

    @Override
    public byte[] encode(NativeModelPayload payload) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        try {
            switch (payload) {
                case NativeState state -> {
                    writeHeader(out, NATIVE_STATE, state.instance());
                    writeString(out, state.model());
                    out.writeInt(state.target());
                    out.writeBoolean(state.hurt());
                    writePose(out, state.pose());
                    writeScale(out, state.scale());
                    out.writeInt(state.color());
                    writeAnimations(out, state.animations());
                    writeBones(out, state.hiddenBones());
                    writeDisplays(out, state.displays());
                }
                case Delta delta -> {
                    writeHeader(out, NATIVE_DELTA, delta.instance());
                    writeDelta(out, delta);
                }
                case DisplayBones bones -> {
                    writeHeader(out, DISPLAY_BONES, bones.instance());
                    writeString(out, bones.model());
                    writeDisplays(out, bones.displays());
                }
                case RemoveNative remove -> writeHeader(out, REMOVE_NATIVE, remove.instance());
                case RemoveDisplay remove -> writeHeader(out, REMOVE_DISPLAY, remove.instance());
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public NativeModelPayload decode(ByteBuffer buffer) {
        int type = Byte.toUnsignedInt(buffer.get());
        UUID instance = new UUID(buffer.getLong(), buffer.getLong());
        return switch (type) {
            case NATIVE_STATE -> new NativeState(instance, readString(buffer), buffer.getInt(), buffer.get() != 0,
                    readPose(buffer), readScale(buffer), buffer.getInt(), readAnimations(buffer), readBones(buffer), readDisplays(buffer));
            case NATIVE_DELTA -> readDelta(buffer, instance);
            case DISPLAY_BONES -> new DisplayBones(instance, readString(buffer), readDisplays(buffer));
            case REMOVE_NATIVE -> new RemoveNative(instance);
            case REMOVE_DISPLAY -> new RemoveDisplay(instance);
            default -> throw new IllegalArgumentException("Unknown model message type: " + type);
        };
    }

    private static void writeHeader(DataOutputStream out, int type, UUID instance) throws IOException {
        out.writeByte(type);
        out.writeLong(instance.getMostSignificantBits());
        out.writeLong(instance.getLeastSignificantBits());
    }

    private static void writeDelta(DataOutputStream out, Delta delta) throws IOException {
        int mask = (delta.target() == null ? 0 : TARGET) | (delta.hurt() == null ? 0 : HURT)
                | (delta.pose() == null ? 0 : POSE) | (delta.scale() == null ? 0 : SCALE)
                | (delta.color() == null ? 0 : COLOR) | (delta.animations() == null ? 0 : ANIMATIONS)
                | (delta.hiddenBones() == null ? 0 : HIDDEN_BONES) | (delta.displays() == null ? 0 : DISPLAYS);
        out.writeByte(mask);
        if ((mask & TARGET) != 0) {
            out.writeInt(delta.target());
        }
        if ((mask & HURT) != 0) {
            out.writeBoolean(delta.hurt());
        }
        if ((mask & POSE) != 0) {
            writePose(out, delta.pose());
        }
        if ((mask & SCALE) != 0) {
            writeScale(out, delta.scale());
        }
        if ((mask & COLOR) != 0) {
            out.writeInt(delta.color());
        }
        if ((mask & ANIMATIONS) != 0) {
            writeAnimations(out, delta.animations());
        }
        if ((mask & HIDDEN_BONES) != 0) {
            writeBones(out, delta.hiddenBones());
        }
        if ((mask & DISPLAYS) != 0) {
            writeDisplays(out, delta.displays());
        }
    }

    private static Delta readDelta(ByteBuffer buffer, UUID instance) {
        int mask = Byte.toUnsignedInt(buffer.get());
        return new Delta(instance,
                (mask & TARGET) == 0 ? null : buffer.getInt(),
                (mask & HURT) == 0 ? null : buffer.get() != 0,
                (mask & POSE) == 0 ? null : readPose(buffer),
                (mask & SCALE) == 0 ? null : readScale(buffer),
                (mask & COLOR) == 0 ? null : buffer.getInt(),
                (mask & ANIMATIONS) == 0 ? null : readAnimations(buffer),
                (mask & HIDDEN_BONES) == 0 ? null : readBones(buffer),
                (mask & DISPLAYS) == 0 ? null : readDisplays(buffer));
    }

    private static void writePose(DataOutputStream out, Pose pose) throws IOException {
        out.writeDouble(pose.x());
        out.writeDouble(pose.y());
        out.writeDouble(pose.z());
        out.writeFloat(pose.yaw());
        out.writeFloat(pose.pitch());
    }

    private static Pose readPose(ByteBuffer buffer) {
        return new Pose(buffer.getDouble(), buffer.getDouble(), buffer.getDouble(), buffer.getFloat(), buffer.getFloat());
    }

    private static void writeScale(DataOutputStream out, Scale scale) throws IOException {
        out.writeFloat(scale.x());
        out.writeFloat(scale.y());
        out.writeFloat(scale.z());
    }

    private static Scale readScale(ByteBuffer buffer) {
        return new Scale(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
    }

    private static void writeAnimations(DataOutputStream out, List<Animation> animations) throws IOException {
        out.writeInt(animations.size());
        for (Animation animation : animations) {
            writeString(out, animation.name());
            out.writeByte(animation.mode());
        }
    }

    private static List<Animation> readAnimations(ByteBuffer buffer) {
        int count = buffer.getInt();
        List<Animation> animations = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            animations.add(new Animation(readString(buffer), buffer.get()));
        }
        return animations;
    }

    private static void writeBones(DataOutputStream out, List<String> bones) throws IOException {
        out.writeInt(bones.size());
        for (String bone : bones) {
            writeString(out, bone);
        }
    }

    private static List<String> readBones(ByteBuffer buffer) {
        int count = buffer.getInt();
        List<String> bones = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            bones.add(readString(buffer));
        }
        return bones;
    }

    private static void writeDisplays(DataOutputStream out, List<Integer> displays) throws IOException {
        out.writeInt(displays.size());
        for (int id : displays) {
            out.writeInt(id);
        }
    }

    private static List<Integer> readDisplays(ByteBuffer buffer) {
        int count = buffer.getInt();
        List<Integer> displays = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            displays.add(buffer.getInt());
        }
        return displays;
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
