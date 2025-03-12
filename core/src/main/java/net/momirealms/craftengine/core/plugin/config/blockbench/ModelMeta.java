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

public class ModelMeta {
    @SerializedName("format_version")
    private String format_version;
    @SerializedName("model_format")
    private String model_format;
    @SerializedName("box_uv")
    private boolean box_uv;

    public String formatVersion() {
        return format_version;
    }

    public String modelFormat() {
        return model_format;
    }

    public boolean boxUV() {
        return box_uv;
    }
}
