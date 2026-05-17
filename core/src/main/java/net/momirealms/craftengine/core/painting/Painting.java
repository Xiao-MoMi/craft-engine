package net.momirealms.craftengine.core.painting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;

public record Painting(int width, int height, Key assetId, Optional<Component> title, Optional<Component> author, boolean showInOpTab) {

    public static Painting fromJson(JsonObject json) {
        int width = json.get("width").getAsInt();
        int height = json.get("height").getAsInt();
        Key assetId = Key.minecraft(json.get("asset_id").getAsString());
        Optional<Component> title = json.has("title") ? Optional.of(AdventureHelper.jsonElementToComponent(json.get("title"))) : Optional.empty();
        Optional<Component> author = json.has("author") ? Optional.of(AdventureHelper.jsonElementToComponent(json.get("author"))) : Optional.empty();
        boolean showInOpTab = json.get("show_in_op_tab").getAsBoolean();
        return new Painting(width, height, assetId, title, author, showInOpTab);
    }

    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("width", this.width);
        json.addProperty("height", this.height);
        json.addProperty("asset_id", this.assetId.toString());
        this.title.ifPresent(it -> json.add("title", AdventureHelper.componentToJsonElement(it)));
        this.author.ifPresent(it -> json.add("author", AdventureHelper.componentToJsonElement(it)));
        json.addProperty("show_in_op_tab", this.showInOpTab);
        return json;
    }
}
