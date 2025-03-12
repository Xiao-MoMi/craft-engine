package net.momirealms.craftengine.core.plugin.config.blockbench;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class OutLiner {
    @SerializedName("name")
    private String name;
    @SerializedName("origin")
    private double[] origin;
    @SerializedName("color")
    private int color;
    @SerializedName("uuid")
    private UUID uuid;
    @SerializedName("export")
    private boolean export;
    @SerializedName("locked")
    private boolean locked;
    @SerializedName("visibility")
    private boolean visibility;
    @SerializedName("mirror_uv")
    private boolean mirror_uv;
    @SerializedName("autouv")
    private boolean autouv;
    @SerializedName("isOpen")
    private boolean is_open;
    @SerializedName("children")
    private OutLiner[] children;

    public String name() {
        return name;
    }

    public double[] origin() {
        return origin;
    }

    public int color() {
        return color;
    }

    public UUID uuid() {
        return uuid;
    }

    public boolean export() {
        return export;
    }

    public boolean locked() {
        return locked;
    }

    public boolean visibility() {
        return visibility;
    }

    public boolean mirrorUV() {
        return mirror_uv;
    }

    public boolean autoUV() {
        return autouv;
    }

    public boolean isOpen() {
        return is_open;
    }

    public OutLiner[] children() {
        return children;
    }
}
