package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ModernItemModel {
    private final ItemModel model;
    private final boolean oversizedInGui;
    private final boolean handAnimationOnSwap;
    private final float swapAnimationScale;

    public ModernItemModel(@NotNull ItemModel model, boolean handAnimationOnSwap, boolean oversizedInGui, float swapAnimationScale) {
        this.handAnimationOnSwap = handAnimationOnSwap;
        this.model = model;
        this.oversizedInGui = oversizedInGui;
        this.swapAnimationScale = swapAnimationScale;
    }

    public static ModernItemModel fromJson(JsonObject json) {
        ItemModel model = ItemModels.fromJson(json.getAsJsonObject("model"));
        return new ModernItemModel(
                model,
                GsonHelper.getAsBoolean(json.get("hand_animation_on_swap"), true),
                GsonHelper.getAsBoolean(json.get("oversized_in_gui"), false),
                GsonHelper.getAsFloat(json.get("swap_animation_scale"), 1.0f)
        );
    }

    @NotNull
    public JsonObject toJson(MinecraftVersion version) {
        return toJson(version, version);
    }

    @NotNull
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        if (this.oversizedInGui) { // 1.21.6
            json.addProperty("oversized_in_gui", true);
        }
        if (!this.handAnimationOnSwap) {
            json.addProperty("hand_animation_on_swap", false);
        }
        if (this.swapAnimationScale != 1.0f) { // 1.21.11
            json.addProperty("swap_animation_scale", this.swapAnimationScale);
        }
        json.add("model", this.model.toJson(min, max));
        return json;
    }

    @NotNull
    public List<Revision> revisions() {
        List<Revision> revisions = new ArrayList<>(4);
        this.model.gatherRevisions(revisions::add);
        return revisions.stream().distinct().toList();
    }

    @NotNull
    public ItemModel model() {
        return this.model;
    }

    public boolean handAnimationOnSwap() {
        return this.handAnimationOnSwap;
    }

    public boolean oversizedInGui() {
        return this.oversizedInGui;
    }

    public float swapAnimationScale() {
        return this.swapAnimationScale;
    }
}
