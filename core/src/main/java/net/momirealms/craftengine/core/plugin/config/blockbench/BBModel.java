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

package net.momirealms.craftengine.core.plugin.config.blockbench;

import com.google.gson.annotations.SerializedName;

public class BBModel {
    @SerializedName("name")
    private String name;
    @SerializedName("meta")
    private ModelMeta meta;
    @SerializedName("model_identifier")
    private String model_identifier;
    @SerializedName("visible_box")
    private int[] visible_box;
    @SerializedName("elements")
    private Element[] elements;
    @SerializedName("outliner")
    private OutLiner[] outliner;
    @SerializedName("textures")
    private Texture[] textures;

    public String name() {
        return name;
    }

    public ModelMeta meta() {
        return meta;
    }

    public String modelIdentifier() {
        return model_identifier;
    }

    public int[] visibleBox() {
        return visible_box;
    }

    public Element[] elements() {
        return elements;
    }

    public OutLiner[] outliner() {
        return outliner;
    }

    public Texture[] textures() {
        return textures;
    }
}
