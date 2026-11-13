package net.momirealms.craftengine.core.pack.bedrock.scoreboard;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record BedrockSidebarConfig(boolean enable, List<Profile> profiles) {
    public static final BedrockSidebarConfig DEFAULT = new BedrockSidebarConfig(true, List.of());
    private static final String[] OBJECTIVE_ID = ConfigKeys.of("objective_id");
    private static final String[] TITLE_CONTAINS = ConfigKeys.of("title_contains");
    private static final String[] OFFSET_X = ConfigKeys.of("offset_x");

    public static BedrockSidebarConfig fromConfig(ConfigSection section) {
        boolean enable = section.getBoolean("enable", true);
        List<Profile> profiles = section.getSectionList("profiles", Profile::fromConfig);
        if (profiles.size() > 9999) {
            throw new KnownResourceException("number.no_greater_than", section.assemblePath("profiles"), Integer.toString(profiles.size()), "9999");
        }
        return new BedrockSidebarConfig(enable, List.copyOf(profiles));
    }

    public record Profile(String objectiveId, String titleContains, int offsetX, List<Element> elements) {

        private static Profile fromConfig(ConfigSection section) {
            String objective = section.getString(OBJECTIVE_ID, "");
            String title = section.getString(TITLE_CONTAINS, "");
            if (objective.isEmpty() && title.isEmpty()) {
                throw new KnownResourceException("resource_pack.bedrock.sidebar.match", section.path());
            }
            Set<String> ids = new HashSet<>();
            List<Element> elements = section.getSectionList("elements", element -> {
                Element parsed = Element.fromConfig(element);
                if (!ids.add(parsed.id())) {
                    throw new KnownResourceException("resource_pack.bedrock.sidebar.duplicate_id", element.assemblePath("id"), parsed.id());
                }
                return parsed;
            });
            return new Profile(objective, title, section.getInt(OFFSET_X, 0), List.copyOf(elements));
        }
    }

    public record Element(String id, Key texture, int width, int height, Anchor anchor, int x, int y, int layer, String texturePath) {

        private static Element fromConfig(ConfigSection section) {
            String id = section.getNonEmptyString("id");
            if (!id.matches("[a-zA-Z0-9_-]+")) {
                throw new KnownResourceException("resource_pack.bedrock.sidebar.id", section.assemblePath("id"), id);
            }
            return new Element(id, section.getNonNullIdentifier("texture"),
                    section.getNonNullValue("width", ConfigConstants.ARGUMENT_INT).getAsInt(1),
                    section.getNonNullValue("height", ConfigConstants.ARGUMENT_INT).getAsInt(1),
                    section.getEnum("anchor", Anchor.class, Anchor.TOP_LEFT),
                    section.getInt("x", 0), section.getInt("y", 0), section.getInt("layer", 1), section.assemblePath("texture"));
        }
    }

    public enum Anchor {
        TOP_LEFT, TOP_MIDDLE, TOP_RIGHT, LEFT_MIDDLE, CENTER, RIGHT_MIDDLE, BOTTOM_LEFT, BOTTOM_MIDDLE, BOTTOM_RIGHT
    }
}
