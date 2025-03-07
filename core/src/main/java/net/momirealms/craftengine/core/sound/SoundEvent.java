package net.momirealms.craftengine.core.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.function.Supplier;

public record SoundEvent(Key id, boolean replace, String subTitle, List<Sound> sounds) implements Supplier<JsonObject> {

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        if (this.replace) {
            json.addProperty("replace", true);
        }
        if (this.subTitle != null) {
            json.addProperty("subtitle", this.subTitle);
        }
        JsonArray sounds = new JsonArray();
        for (Sound sound : this.sounds) {
            sounds.add(sound.get());
        }
        json.add("sounds", sounds);
        return json;
    }
}
