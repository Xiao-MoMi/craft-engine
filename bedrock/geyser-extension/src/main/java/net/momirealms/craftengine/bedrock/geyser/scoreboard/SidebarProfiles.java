package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record SidebarProfiles(boolean enabled, List<Profile> profiles) {
    public static final SidebarProfiles EMPTY = new SidebarProfiles(false, List.of());

    public static SidebarProfiles parse(JsonObject data) {
        if (data == null || !data.get("enabled").getAsBoolean()) {
            return EMPTY;
        }
        List<Profile> profiles = new ArrayList<>();
        for (JsonElement value : data.getAsJsonArray("profiles")) {
            JsonObject profile = value.getAsJsonObject();
            profiles.add(new Profile(profile.get("index").getAsInt(), text(profile, "objective-id"), text(profile, "title-contains")));
        }
        return new SidebarProfiles(true, List.copyOf(profiles));
    }

    public int match(String objective, String title) {
        for (Profile profile : this.profiles) {
            if (!profile.objective().isEmpty() && profile.objective().equals(objective)) {
                return profile.index();
            }
        }
        for (Profile profile : this.profiles) {
            if (!profile.title().isEmpty() && title.contains(profile.title())) {
                return profile.index();
            }
        }
        return 0;
    }

    private static String text(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : "";
    }

    public record Profile(int index, String objective, String title) {
    }
}
