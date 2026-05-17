package net.momirealms.craftengine.core.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.Locale;
import java.util.function.Supplier;

public interface Sound extends Supplier<JsonElement> {

    static SoundPath path(final String path) {
        return new SoundPath(path);
    }

    record SoundPath(String path) implements Sound {

        @Override
        public JsonElement get() {
            return new JsonPrimitive(this.path);
        }
    }

    enum Type {
        FILE,
        EVENT
    }

    class SoundFile implements Sound {
        private final String name;
        private final float volume;
        private final float pitch;
        private final int weight;
        private final boolean stream;
        private final int attenuationDistance;
        private final boolean preload;
        private final Type type;

        public SoundFile(String name, float volume, float pitch, int weight, boolean stream, int attenuationDistance, boolean preload, Type type) {
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
            this.weight = weight;
            this.stream = stream;
            this.attenuationDistance = attenuationDistance;
            this.preload = preload;
            this.type = type;
        }

        private static final String[] ATTENUATION_DISTANCE = new String[] {"attenuation_distance", "attenuation-distance"};

        public static SoundFile fromConfig(ConfigSection section) {
            return new SoundFile.Builder(section.getNonEmptyString("name"))
                    .attenuationDistance(section.getInt(ATTENUATION_DISTANCE, 16))
                    .volume(section.getFloat("volume", 1.0f))
                    .pitch(section.getFloat("pitch", 1.0f))
                    .weight(section.getInt("weight", 1))
                    .stream(section.getBoolean("stream"))
                    .preload(section.getBoolean("preload"))
                    .type(section.getEnum("type", Type.class))
                    .build();
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
            if (this.type != null && this.type != Type.FILE) {
                json.addProperty("type", this.type.name().toLowerCase(Locale.ROOT));
            }
            return json;
        }

        public static final class Builder {
            private final String name;
            private float volume = 1.0f;
            private float pitch = 1.0f;
            private int weight = 1;
            private boolean stream = false;
            private int attenuationDistance = 16;
            private boolean preload = false;
            private Type type = Type.FILE;

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

            public Builder type(Type type) {
                this.type = type;
                return this;
            }

            public SoundFile build() {
                return new SoundFile(
                        this.name,
                        this.volume,
                        this.pitch,
                        this.weight,
                        this.stream,
                        this.attenuationDistance,
                        this.preload,
                        this.type
                );
            }
        }
    }
}
