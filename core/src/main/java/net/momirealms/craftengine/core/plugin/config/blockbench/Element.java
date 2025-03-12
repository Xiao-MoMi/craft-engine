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

import java.util.UUID;

public class Element {

    @SerializedName("type")
    private String type;
    @SerializedName("uuid")
    private UUID uuid;
    @SerializedName("name")
    private String name;
    @SerializedName("box_uv")
    private boolean box_uv;
    @SerializedName("rescale")
    private boolean rescale;
    @SerializedName("locked")
    private boolean locked;
    @SerializedName("light_emission")
    private int light_emission;
    @SerializedName("render_order")
    private String render_order;
    @SerializedName("allow_mirror_modeling")
    private boolean allow_mirror_modeling;
    @SerializedName("from")
    private double[] from;
    @SerializedName("to")
    private double[] to;
    @SerializedName("autouv")
    private int autouv;
    @SerializedName("color")
    private int color;
    @SerializedName("origin")
    private double[] origin;

    public String type() {
        return type;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public boolean box_uv() {
        return box_uv;
    }

    public boolean rescale() {
        return rescale;
    }

    public boolean locked() {
        return locked;
    }

    public int lightEmission() {
        return light_emission;
    }

    public String renderOrder() {
        return render_order;
    }

    public boolean allowMirrorModeling() {
        return allow_mirror_modeling;
    }

    public double[] from() {
        return from;
    }

    public double[] to() {
        return to;
    }

    public int autoUV() {
        return autouv;
    }

    public int color() {
        return color;
    }

    public double[] origin() {
        return origin;
    }
}
