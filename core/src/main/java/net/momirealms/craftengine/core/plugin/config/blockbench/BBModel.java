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
