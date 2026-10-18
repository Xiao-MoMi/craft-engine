package net.momirealms.craftengine.bedrock;

import java.util.List;
import java.util.Map;

public record BedrockPackMappings(int formatVersion, String javaVersion, String sourceSha1, List<Item> items) {
    public static final int FORMAT_VERSION = 5;
    public static final String PATH = "craftengine/bedrock-mappings.json";
    public static final String CROSSBOW_TAG = "craftengine:crossbow";

    public BedrockPackMappings {
        items = List.copyOf(items);
    }

    public record Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier, String icon, DisplayBlock displayBlock,
                       boolean displayHandheld, ItemVariant fishingRodCast, CrossbowCharge crossbowCharge) {

        public Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier, String icon, DisplayBlock displayBlock,
                    boolean displayHandheld, ItemVariant fishingRodCast) {
            this(id, javaItem, javaItemModel, bedrockIdentifier, icon, displayBlock, displayHandheld, fishingRodCast, null);
        }

        public Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier, String icon, DisplayBlock displayBlock, boolean displayHandheld) {
            this(id, javaItem, javaItemModel, bedrockIdentifier, icon, displayBlock, displayHandheld, null);
        }

        public Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier, String icon, DisplayBlock displayBlock) {
            this(id, javaItem, javaItemModel, bedrockIdentifier, icon, displayBlock, false);
        }

        public Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier, String icon) {
            this(id, javaItem, javaItemModel, bedrockIdentifier, icon, null);
        }

        public Item(String id, String javaItem, String javaItemModel, String bedrockIdentifier) {
            this(id, javaItem, javaItemModel, bedrockIdentifier, "stick");
        }
    }

    public record ItemVariant(String bedrockIdentifier, String icon, DisplayBlock displayBlock) {
    }

    public record CrossbowCharge(ItemVariant arrow, ItemVariant rocket) {
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
