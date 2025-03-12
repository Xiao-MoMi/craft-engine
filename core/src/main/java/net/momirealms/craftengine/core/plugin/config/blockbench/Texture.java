package net.momirealms.craftengine.core.plugin.config.blockbench;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class Texture {
    @SerializedName("path")
    private String path;
    @SerializedName("name")
    private String name;
    @SerializedName("folder")
    private String folder;
    @SerializedName("namespace")
    private String namespace;
    @SerializedName("id")
    private String id;
    @SerializedName("group")
    private String group;
    @SerializedName("width")
    private int width;
    @SerializedName("height")
    private int height;
    @SerializedName("uv_width")
    private int uv_width;
    @SerializedName("uv_height")
    private int uv_height;
    @SerializedName("particle")
    private boolean particle;
    @SerializedName("use_as_default")
    private boolean use_as_default;
    @SerializedName("layers_enabled")
    private boolean layers_enabled;
    @SerializedName("sync_to_project")
    private String sync_to_project;
    @SerializedName("render_mode")
    private String render_mode;
    @SerializedName("render_sides")
    private String render_sides;
    @SerializedName("frame_time")
    private int frame_time;
    @SerializedName("frame_order_type")
    private String frame_order_type;
    @SerializedName("frame_order")
    private String frame_order;
    @SerializedName("frame_interpolate")
    private boolean frame_interpolate;
    @SerializedName("visible")
    private boolean visible;
    @SerializedName("internal")
    private boolean internal;
    @SerializedName("saved")
    private boolean saved;
    @SerializedName("uuid")
    private UUID uuid;
    @SerializedName("relative_path")
    private String relative_path;
    @SerializedName("source")
    private String source;

    public String path() {
        return path;
    }

    public String name() {
        return name;
    }

    public String folder() {
        return folder;
    }

    public String namespace() {
        return namespace;
    }

    public String id() {
        return id;
    }

    public String group() {
        return group;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int uvWidth() {
        return uv_width;
    }

    public int uvHeight() {
        return uv_height;
    }

    public boolean particle() {
        return particle;
    }

    public boolean useAsDefault() {
        return use_as_default;
    }

    public boolean layersEnabled() {
        return layers_enabled;
    }

    public String syncToProject() {
        return sync_to_project;
    }

    public String renderMode() {
        return render_mode;
    }

    public String renderSides() {
        return render_sides;
    }

    public int frameTime() {
        return frame_time;
    }

    public String frameOrderType() {
        return frame_order_type;
    }

    public String frameOrder() {
        return frame_order;
    }

    public boolean frameInterpolate() {
        return frame_interpolate;
    }

    public boolean visible() {
        return visible;
    }

    public boolean internal() {
        return internal;
    }

    public boolean saved() {
        return saved;
    }

    public UUID uuid() {
        return uuid;
    }

    public String relativePath() {
        return relative_path;
    }

    public String source() {
        return source;
    }
}
