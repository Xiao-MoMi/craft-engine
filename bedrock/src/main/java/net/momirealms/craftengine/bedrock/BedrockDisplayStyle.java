package net.momirealms.craftengine.bedrock;

public final class BedrockDisplayStyle {
    public static final String PROPERTY = BedrockPackLayout.ID_PREFIX + "display_style";
    public static final int HIDDEN_FLAG = 4;
    public static final int HELD_ITEM_STRIDE = 8;
    public static final int MAX = (BedrockHeldItemType.SPEAR.id() + 1) * HELD_ITEM_STRIDE - 1;

    private BedrockDisplayStyle() {
    }

    public static int pack(int billboard, boolean hidden, BedrockHeldItemType heldItemType) {
        return (billboard & (HIDDEN_FLAG - 1)) | (hidden ? HIDDEN_FLAG : 0) | (heldItemType.id() * HELD_ITEM_STRIDE);
    }
}
