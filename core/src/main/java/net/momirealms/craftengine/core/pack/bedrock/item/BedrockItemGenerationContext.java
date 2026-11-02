package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.*;

public final class BedrockItemGenerationContext {
    private final Path root;
    private final Map<Key, ItemRegistrationPlan.Entry> javaSpears = new LinkedHashMap<>();
    private final Set<Key> javaShields = new HashSet<>();
    private final Set<Key> bows = new HashSet<>();
    private final Set<Key> javaBows = new HashSet<>();
    private final Set<Key> javaSwings = new HashSet<>();
    private final BedrockItemBlocks blocks = new BedrockItemBlocks();
    private final BedrockCrossbowPlayerAnimations crossbowPlayerAnimations = new BedrockCrossbowPlayerAnimations();

    public BedrockItemGenerationContext(Path root) {
        this.root = root;
    }

    public Path root() {
        return this.root;
    }

    public Map<Key, ItemRegistrationPlan.Entry> javaSpears() {
        return this.javaSpears;
    }

    public Set<Key> javaShields() {
        return this.javaShields;
    }

    public Set<Key> bows() {
        return this.bows;
    }

    public Set<Key> javaBows() {
        return this.javaBows;
    }

    public Set<Key> javaSwings() {
        return this.javaSwings;
    }

    public BedrockItemBlocks blocks() {
        return this.blocks;
    }

    public BedrockCrossbowPlayerAnimations crossbowPlayerAnimations() {
        return this.crossbowPlayerAnimations;
    }
}
