package net.momirealms.craftengine.bedrock;

import java.util.List;
import java.util.Map;

public record BedrockPackMappings(int formatVersion, String javaVersion, String sourceSha1, List<Item> items) {
    public static final int FORMAT_VERSION = 3;
    public static final String PATH = "craftengine/bedrock-mappings.json";

    public BedrockPackMappings {
        items = List.copyOf(items);
    }

    public record Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier, String icon, DisplayBlock displayBlock) {

        public Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier, String icon) {
            this(id, javaItem, javaItemModel, bedrockIdentifier, icon, null);
        }

        public Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier) {
            this(id, javaItem, javaItemModel, bedrockIdentifier, "stick");
        }
    }

    public record DisplayBlock(String name, String geometry, String texture, Map<String, String> materials) {

        public DisplayBlock {
            materials = Map.copyOf(materials);
        }

        public DisplayBlock(String name, String geometry, String texture) {
            this(name, geometry, texture, Map.of());
        }
    }
}
