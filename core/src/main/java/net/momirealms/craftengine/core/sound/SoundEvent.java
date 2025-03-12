/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
