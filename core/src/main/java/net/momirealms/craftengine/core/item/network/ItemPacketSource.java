package net.momirealms.craftengine.core.item.network;

public final class ItemPacketSource {
    public static final ItemPacketSource GENERIC = builder("generic").requireNetworkTag().build();
    public static final ItemPacketSource MESSAGE = builder("message").build();
    public static final ItemPacketSource ENTITY_DATA = builder("entity_data").skipLore().build();
    public static final ItemPacketSource SET_EQUIPMENT = builder("set_equipment").skipLore().build();
    public static final ItemPacketSource CONTAINER = builder("container").requireNetworkTag().build();
    public static final ItemPacketSource DIALOG = builder("dialog").build();
    public static final ItemPacketSource BLOCK_ENTITY = builder("block_entity").skipLore().build();
    public static final ItemPacketSource PARTICLE = builder("particle").skipLore().skipName().build();
    public static final ItemPacketSource MERCHANT_OFFER = builder("merchant_offer").build();
    public static final ItemPacketSource RECIPE = builder("recipe").build();
    public static final ItemPacketSource ADVANCEMENT = builder("advancement").build();

    public final String name;
    public final boolean canSkipLore;
    public final boolean canSkipName;
    public final boolean requireNetworkTag;

    private ItemPacketSource(Builder builder) {
        this.name = builder.name;
        this.canSkipLore = builder.canSkipLore;
        this.canSkipName = builder.canSkipName;
        this.requireNetworkTag = builder.requireNetworkTag;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private boolean canSkipLore;
        private boolean canSkipName;
        private boolean requireNetworkTag;

        private Builder(String name) {
            this.name = name;
        }
        
        public Builder requireNetworkTag() {
            this.requireNetworkTag = true;
            return this;
        }

        public Builder skipLore() {
            this.canSkipLore = true;
            return this;
        }

        public Builder skipName() {
            this.canSkipName = true;
            return this;
        }

        public ItemPacketSource build() {
            return new ItemPacketSource(this);
        }
    }
}
