package net.momirealms.craftengine.core.plugin.config.blockbench;

import com.google.gson.annotations.SerializedName;

public class Resolution {
    @SerializedName("width")
    private int width;
    @SerializedName("height")
    private int height;

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
