package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemBlocks;

import java.nio.file.Path;

public final class BedrockItemGenerationContext {
    private final Path root;
    private final BedrockItemBlocks itemDisplayBlocks = new BedrockItemBlocks();
    private final BedrockPlayerAnimations playerAnimations = new BedrockPlayerAnimations();

    public BedrockItemGenerationContext(Path root) {
        this.root = root;
    }

    public Path root() {
        return this.root;
    }

    public BedrockItemBlocks itemDisplayBlocks() {
        return this.itemDisplayBlocks;
    }

    public BedrockPlayerAnimations playerAnimations() {
        return this.playerAnimations;
    }
}
