package net.momirealms.craftengine.core.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface Sound extends Supplier<JsonElement> {

    static SoundPath path(final String path) {
        return new SoundPath(path);
    }

    static SoundFile.Builder file(final String name) {
        return new SoundFile.Builder(name);
    }

    record SoundPath(String path) implements Sound {

        @Override
        public JsonElement get() {
            return new JsonPrimitive(this.path);
        }
    }

    class SoundFile implements Sound {
        private final String name;
        private final float volume;
        private final float pitch;
        private final int weight;
        private final boolean stream;
        private final int attenuationDistance;
        private final boolean preload;
        private final String type;

        public SoundFile(String name, float volume, float pitch, int weight, boolean stream, int attenuationDistance, boolean preload, String type) {
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
            this.weight = weight;
            this.stream = stream;
            this.attenuationDistance = attenuationDistance;
            this.preload = preload;
            this.type = type;
        }

        public static SoundFile fromMap(Map<String, Object> map) {
            String name = (String) map.get("name");
            if (name == null) throw new IllegalArgumentException("Missing 'name' for sound");
            Builder builder = file(name);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Optional.ofNullable(Builder.MODIFIERS.get(entry.getKey())).ifPresent(modifier -> {
                    modifier.apply(builder, entry.getValue());
                });
            }
            return builder.build();
        }

        @Override
        public JsonElement get() {
            JsonObject json = new JsonObject();
            json.addProperty("name", this.name);
            if (this.volume != 1f) {
                json.addProperty("volume", this.volume);
            }
            if (this.pitch != 1f) {
                json.addProperty("pitch", this.pitch);
            }
            if (this.weight != 1) {
                json.addProperty("weight", this.weight);
            }
            if (this.stream) {
                json.addProperty("stream", true);
            }
            if (this.attenuationDistance != 16) {
                json.addProperty("attenuation_distance", this.attenuationDistance);
            }
            if (this.preload) {
                json.addProperty("preload", true);
            }
            if (this.type != null && !this.type.equals("file")) {
                json.addProperty("type", this.type);
            }
            return json;
        }

        public static class Builder {
            public static final Map<String, Modifier> MODIFIERS = new HashMap<>();

            static {
                MODIFIERS.put("volume", (b, o) -> b.volume(MiscUtils.getAsFloat(o)));
                MODIFIERS.put("pitch", (b, o) -> b.pitch(MiscUtils.getAsFloat(o)));
                MODIFIERS.put("weight", (b, o) -> b.pitch(MiscUtils.getAsInt(o)));
                MODIFIERS.put("stream", (b, o) -> b.stream((boolean) o));
                MODIFIERS.put("attenuation-distance", (b, o) -> b.attenuationDistance(MiscUtils.getAsInt(o)));
                MODIFIERS.put("preload", (b, o) -> b.preload((boolean) o));
                MODIFIERS.put("type", (b, o) -> b.type(o.toString()));
            }

            private final String name;
            private float volume = 1.0f;
            private float pitch = 1.0f;
            private int weight = 1;
            private boolean stream = false;
            private int attenuationDistance = 16;
            private boolean preload = false;
            private String type = "file";

            public Builder(String name) {
                this.name = name;
            }

            public Builder volume(float volume) {
                this.volume = volume;
                return this;
            }

            public Builder pitch(float pitch) {
                this.pitch = pitch;
                return this;
            }

            public Builder weight(int weight) {
                this.weight = weight;
                return this;
            }

            public Builder stream(boolean stream) {
                this.stream = stream;
                return this;
            }

            public Builder attenuationDistance(int attenuation_distance) {
                this.attenuationDistance = attenuation_distance;
                return this;
            }

            public Builder preload(boolean preload) {
                this.preload = preload;
                return this;
            }

            public Builder type(String type) {
                this.type = type;
                return this;
            }

            public SoundFile build() {
                return new SoundFile(name, volume, pitch, weight, stream, attenuationDistance, preload, type);
            }

            @FunctionalInterface
            public interface Modifier {

                void apply(Builder builder, Object value);
            }
        }
    }
}
