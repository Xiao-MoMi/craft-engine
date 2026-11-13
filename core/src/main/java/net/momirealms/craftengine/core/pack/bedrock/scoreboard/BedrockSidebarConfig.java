package net.momirealms.craftengine.core.pack.bedrock.scoreboard;

import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.util.CollectingConfigReader;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record BedrockSidebarConfig(List<Profile> profiles) {
    public static final BedrockSidebarConfig DEFAULT = new BedrockSidebarConfig(List.of());
    private static final String[] OBJECTIVE_ID = ConfigKeys.of("objective_id");
    private static final String[] TITLE_CONTAINS = ConfigKeys.of("title_contains");
    private static final String[] OFFSET_X = ConfigKeys.of("offset_x");

    public static BedrockSidebarConfig fromConfig(ConfigSection section, CollectingConfigReader config) {
        List<Profile> profiles = config.getList(section.getValue("profiles"), value -> {
            int errors = config.errorCount();
            Profile profile = Profile.fromConfig(value.getAsSection(), config);
            return config.errorCount() == errors ? profile : null;
        });
        return new BedrockSidebarConfig(List.copyOf(profiles));
    }

    public boolean enable() {
        return !this.profiles.isEmpty();
    }

    public record Profile(String objectiveId, String titleContains, int offsetX, List<Element> elements) {

        private static Profile fromConfig(ConfigSection section, CollectingConfigReader config) {
            String objective = config.read(section.getValue(OBJECTIVE_ID), CollectingConfigReader::string, "");
            String title = config.read(section.getValue(TITLE_CONTAINS), CollectingConfigReader::string, "");
            if (objective.isEmpty() && title.isEmpty()) {
                config.error(section.path(), "resource_pack.bedrock.sidebar.match");
            }
            Set<String> ids = new HashSet<>();
            List<Element> elements = config.getList(section.getValue("elements"), value -> {
                ConfigSection element = value.getAsSection();
                Element parsed = Element.fromConfig(element, config);
                if (!parsed.id().isEmpty() && !ids.add(parsed.id())) {
                    throw new KnownResourceException("resource_pack.bedrock.sidebar.duplicate_id", element.assemblePath("id"), parsed.id());
                }
                return parsed;
            });
            return new Profile(objective, title, config.read(section.getValue(OFFSET_X), ConfigValue::getAsInt, 0), List.copyOf(elements));
        }
    }

    public record Element(String id, Key texture, int width, int height, Anchor anchor, int x, int y, int layer, String texturePath) {

        private static Element fromConfig(ConfigSection section, CollectingConfigReader config) {
            String id = config.attempt(section.assemblePath("id"), () -> {
                String parsed = section.getNonEmptyString("id");
                if (!parsed.matches("[a-zA-Z0-9_-]+")) {
                    throw new KnownResourceException("resource_pack.bedrock.sidebar.id", section.assemblePath("id"), parsed);
                }
                return parsed;
            }, "");
            return new Element(id, config.attempt(section.assemblePath("texture"), () -> section.getNonNullIdentifier("texture"), null),
                    config.attempt(section.assemblePath("width"), () -> section.getNonNullValue("width", ConfigConstants.ARGUMENT_INT).getAsInt(1), 1),
                    config.attempt(section.assemblePath("height"), () -> section.getNonNullValue("height", ConfigConstants.ARGUMENT_INT).getAsInt(1), 1),
                    config.attempt(section.assemblePath("anchor"), () -> section.getEnum("anchor", Anchor.class, Anchor.TOP_LEFT), Anchor.TOP_LEFT),
                    config.attempt(section.assemblePath("x"), () -> section.getInt("x", 0), 0), config.attempt(section.assemblePath("y"), () -> section.getInt("y", 0), 0), config.attempt(section.assemblePath("layer"), () -> section.getInt("layer", 1), 1), section.assemblePath("texture"));
        }
    }

    public enum Anchor {
        TOP_LEFT, TOP_MIDDLE, TOP_RIGHT, LEFT_MIDDLE, CENTER, RIGHT_MIDDLE, BOTTOM_LEFT, BOTTOM_MIDDLE, BOTTOM_RIGHT
    }
}
