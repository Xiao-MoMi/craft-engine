package net.momirealms.craftengine.bedrock.network;

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

public record NativeModelPayload(UUID instance, String model, int target, boolean removed,
                                 double x, double y, double z, float yaw, float pitch,
                                 float scaleX, float scaleY, float scaleZ, int color,
                                 List<Animation> animations, List<String> hiddenBones, List<Integer> displays) {
    public static final String CHANNEL = "craftengine:native_model";
    public static final PacketCodec<NativeModelPayload> CODEC = PacketCodec.of(NativeModelPayload::encode, NativeModelPayload::decode);

    public record Animation(String name, int mode) {
        public static final int ONCE = 1;
        public static final int LOOP = 2;
        public static final int HOLD = 3;
    }

    public NativeModelPayload remove() {
        return new NativeModelPayload(this.instance, this.model, this.target, true, 0, 0, 0, 0, 0, 1, 1, 1, -1, List.of(), List.of(), List.of());
    }

    private byte[] encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeLong(this.instance.getMostSignificantBits());
            out.writeLong(this.instance.getLeastSignificantBits());
            writeString(out, this.model);
            out.writeInt(this.target);
            out.writeBoolean(this.removed);
            out.writeDouble(this.x);
            out.writeDouble(this.y);
            out.writeDouble(this.z);
            out.writeFloat(this.yaw);
            out.writeFloat(this.pitch);
            out.writeFloat(this.scaleX);
            out.writeFloat(this.scaleY);
            out.writeFloat(this.scaleZ);
            out.writeInt(this.color);
            out.writeInt(this.animations.size());
            for (Animation animation : this.animations) {
                writeString(out, animation.name());
                out.writeByte(animation.mode());
            }
            out.writeInt(this.hiddenBones.size());
            for (String name : this.hiddenBones) {
                writeString(out, name);
            }
            out.writeInt(this.displays.size());
            for (int id : this.displays) {
                out.writeInt(id);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static NativeModelPayload decode(ByteBuffer buffer) {
        UUID instance = new UUID(buffer.getLong(), buffer.getLong());
        String model = readString(buffer);
        int target = buffer.getInt();
        boolean removed = buffer.get() != 0;
        double x = buffer.getDouble();
        double y = buffer.getDouble();
        double z = buffer.getDouble();
        float yaw = buffer.getFloat();
        float pitch = buffer.getFloat();
        float sx = buffer.getFloat();
        float sy = buffer.getFloat();
        float sz = buffer.getFloat();
        int color = buffer.getInt();
        int animationCount = buffer.getInt();
        List<Animation> animations = new ArrayList<>(animationCount);
        for (int i = 0; i < animationCount; i++) {
            animations.add(new Animation(readString(buffer), buffer.get()));
        }
        List<String> hidden = readStrings(buffer);
        int count = buffer.getInt();
        List<Integer> displays = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            displays.add(buffer.getInt());
        }
        return new NativeModelPayload(instance, model, target, removed, x, y, z, yaw, pitch, sx, sy, sz, color, animations, hidden, displays);
    }

    private static List<String> readStrings(ByteBuffer buffer) {
        int count = buffer.getInt();
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(readString(buffer));
        }
        return result;
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
