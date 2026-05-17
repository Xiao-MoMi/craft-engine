package net.momirealms.craftengine.core.pack.mcmeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class Overlays {
    private final List<Overlay> overlays;

    public Overlays(JsonObject mcmeta) {
        this.overlays = getOverlays(mcmeta);
    }

    public Overlays(List<Overlay> overlays) {
        this.overlays = overlays;
    }

    private List<Overlay> getOverlays(JsonObject mcmeta) {
        List<Overlay> overlays = new ArrayList<>();
        JsonObject overlaysJson = mcmeta.getAsJsonObject("overlays");
        if (overlaysJson != null) {
            JsonArray entries = overlaysJson.getAsJsonArray("entries");
            if (entries != null) {
                for (JsonElement overlayJson : entries) {
                    if (overlayJson instanceof JsonObject overlayJsonObj) {
                        overlays.add(new Overlay(overlayJsonObj));
                    }
                }
            }
        }
        return overlays;
    }

    public List<Overlay> overlays() {
        return this.overlays;
    }

    public boolean addOverlay(Overlay overlay) {
        if (this.overlays.contains(overlay)) {
            return false;
        }
        return this.overlays.add(overlay);
    }
}
