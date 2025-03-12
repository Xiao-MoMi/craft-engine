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
