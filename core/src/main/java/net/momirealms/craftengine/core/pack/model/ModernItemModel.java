package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.List;

public class ModernItemModel {
    private final ItemModel itemModel;
    private final boolean oversizedInGui;
    private final boolean handAnimationOnSwap;

    public ModernItemModel(ItemModel itemModel, boolean handAnimationOnSwap, boolean oversizedInGui) {
        this.handAnimationOnSwap = handAnimationOnSwap;
        this.itemModel = itemModel;
        this.oversizedInGui = oversizedInGui;
    }

    public static ModernItemModel fromJson(JsonObject json) {
        ItemModel model = ItemModels.fromJson(json.getAsJsonObject("model"));
        return new ModernItemModel(
                model,
                !json.has("hand_animation_on_swap") || json.get("hand_animation_on_swap").getAsBoolean(),
                json.has("oversized_in_gui") && json.get("oversized_in_gui").getAsBoolean()
        );
    }

    public JsonObject toJson(MinecraftVersion version) {
        JsonObject json = new JsonObject();
        if (this.oversizedInGui) {
            json.addProperty("oversized_in_gui", true);
        }
        if (!this.handAnimationOnSwap) {
            json.addProperty("hand_animation_on_swap", false);
        }
        json.add("model", this.itemModel.apply(version));
        return json;
    }

    public List<Revision> revisions() {
        return this.itemModel.revisions().stream().distinct().toList();
    }

    public boolean handAnimationOnSwap() {
        return handAnimationOnSwap;
    }

    public ItemModel itemModel() {
        return itemModel;
    }

    public boolean oversizedInGui() {
        return oversizedInGui;
    }
}
