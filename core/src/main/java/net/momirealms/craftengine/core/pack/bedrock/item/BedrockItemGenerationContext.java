package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.equipment.BedrockAnimalEquipment;
import net.momirealms.craftengine.core.pack.bedrock.item.equipment.BedrockElytraEquipment;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemBlocks;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;

import java.nio.file.Path;

public final class BedrockItemGenerationContext {
    private final Path root;
    private final BedrockAnimalEquipment animalEquipment = new BedrockAnimalEquipment();
    private final BedrockElytraEquipment elytraEquipment = new BedrockElytraEquipment();
    private final BedrockItemBlocks itemDisplayBlocks;
    private final BedrockPlayerAnimations playerAnimations = new BedrockPlayerAnimations();

    public BedrockItemGenerationContext(Path root, BedrockTerrainAtlas atlas) {
        this.root = root;
        this.itemDisplayBlocks = new BedrockItemBlocks(atlas);
    }

    public BedrockAnimalEquipment animalEquipment() {
        return this.animalEquipment;
    }

    public BedrockElytraEquipment elytraEquipment() {
        return this.elytraEquipment;
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
