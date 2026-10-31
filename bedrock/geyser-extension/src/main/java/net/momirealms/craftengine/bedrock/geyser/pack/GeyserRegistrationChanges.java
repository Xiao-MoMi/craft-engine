package net.momirealms.craftengine.bedrock.geyser.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserRegistrationChanges {
    private static final int MAX_DETAILS = 8;
    private int count;
    private final List<String> details = new ArrayList<>();

    public boolean requiresRestart() {
        return this.count != 0;
    }

    public String summary() {
        return String.join("; ", this.details) + (this.count > this.details.size() ? "; and " + (this.count - this.details.size()) + " more changes" : "");
    }

    public void compare(RegistrationPlanKind kind, JsonObject previous, JsonObject next) {
        String type = kind == RegistrationPlanKind.ITEM ? "item" : "block";
        JsonObject before = previous.deepCopy();
        JsonObject after = next.deepCopy();
        JsonArray oldEntries = before.remove(ENTRIES).getAsJsonArray();
        JsonArray newEntries = after.remove(ENTRIES).getAsJsonArray();
        before.remove(DIAGNOSTICS);
        after.remove(DIAGNOSTICS);
        String header = difference(before, after, "");
        if (header != null) {
            this.add(type + " plan: " + header);
        }
        String key = kind == RegistrationPlanKind.ITEM ? BEDROCK_IDENTIFIER : NAME;
        Map<String, JsonObject> oldById = index(oldEntries, key);
        Map<String, JsonObject> newById = index(newEntries, key);
        // Equal-priority definitions can share a Java model, so keep their registration order significant.
        if (kind == RegistrationPlanKind.ITEM && oldById.keySet().equals(newById.keySet())
                && !new ArrayList<>(oldById.keySet()).equals(new ArrayList<>(newById.keySet()))) {
            this.add("item registration order changed");
        }
        Set<String> identifiers = new LinkedHashSet<>(oldById.keySet());
        identifiers.addAll(newById.keySet());
        for (String id : identifiers) {
            JsonObject oldEntry = oldById.get(id);
            JsonObject newEntry = newById.get(id);
            if (oldEntry == null || newEntry == null) {
                this.add(type + " " + id + (oldEntry == null ? " added" : " removed"));
                continue;
            }
            if (kind == RegistrationPlanKind.ITEM) {
                normalizeItem(oldEntry);
                normalizeItem(newEntry);
            }
            String change = difference(oldEntry, newEntry, "");
            if (change != null) {
                this.add(type + " " + id + ": " + change);
            }
        }
    }

    private void add(String change) {
        this.count++;
        if (this.details.size() < MAX_DETAILS) {
            this.details.add(change);
        }
    }

    private static Map<String, JsonObject> index(JsonArray entries, String key) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonElement entry : entries) {
            JsonObject object = entry.getAsJsonObject();
            String id = object.get(key).getAsString();
            if (result.put(id, object) != null) {
                throw new IllegalArgumentException("Duplicate registration identifier: " + id);
            }
        }
        return result;
    }

    private static void normalizeItem(JsonObject entry) {
        // These fields describe generation, not the definitions or component patches installed in Geyser.
        entry.remove(ITEM_ID);
        if (!entry.has(COMPONENTS)) {
            entry.add(COMPONENTS, new JsonObject());
        }
        for (JsonElement variant : entry.getAsJsonArray(VARIANTS)) {
            JsonObject resources = variant.getAsJsonObject().getAsJsonObject(RESOURCES);
            resources.remove(ATTACHABLE);
            if (resources.has(UNSUPPORTED_REASON)) {
                resources.addProperty(UNSUPPORTED_REASON, true);
            }
        }
    }

    private static String difference(JsonElement before, JsonElement after, String path) {
        if (Objects.equals(before, after)) {
            return null;
        }
        if (before != null && after != null && before.isJsonObject() && after.isJsonObject()) {
            JsonObject oldObject = before.getAsJsonObject();
            JsonObject newObject = after.getAsJsonObject();
            Set<String> keys = new LinkedHashSet<>(oldObject.keySet());
            keys.addAll(newObject.keySet());
            for (String key : keys) {
                String changed = difference(oldObject.get(key), newObject.get(key), path.isEmpty() ? key : path + '.' + key);
                if (changed != null) {
                    return changed;
                }
            }
        } else if (before != null && after != null && before.isJsonArray() && after.isJsonArray()) {
            JsonArray oldArray = before.getAsJsonArray();
            JsonArray newArray = after.getAsJsonArray();
            if (oldArray.size() != newArray.size()) {
                return path + " count " + oldArray.size() + " -> " + newArray.size();
            }
            for (int i = 0; i < oldArray.size(); i++) {
                String changed = difference(oldArray.get(i), newArray.get(i), path + '[' + i + ']');
                if (changed != null) {
                    return changed;
                }
            }
        }
        return path + " " + describe(before) + " -> " + describe(after);
    }

    private static String describe(JsonElement value) {
        if (value == null) {
            return "<absent>";
        }
        if (value.isJsonObject()) {
            return "{...}";
        }
        if (value.isJsonArray()) {
            return "[...]";
        }
        String text = value.toString();
        return text.length() > 120 ? text.substring(0, 117) + "..." : text;
    }
}
