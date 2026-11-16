package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.render.BedrockAnimalEquipment;
import net.momirealms.craftengine.core.pack.bedrock.item.render.BedrockElytraEquipment;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemDisplayBlocks;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;

import java.nio.file.Path;

public final class BedrockItemGenerationContext {
    private final Path root;
    private final BedrockAnimalEquipment animalEquipment = new BedrockAnimalEquipment();
    private final BedrockElytraEquipment elytraEquipment = new BedrockElytraEquipment();
    private final BedrockItemDisplayBlocks itemDisplayBlocks;
    private final BedrockPlayerAnimations playerAnimations = new BedrockPlayerAnimations();

    public BedrockItemGenerationContext(Path root, BedrockTerrainAtlas atlas, JsonObject blockDefinitions) {
        this.root = root;
        this.itemDisplayBlocks = new BedrockItemDisplayBlocks(atlas, blockDefinitions);
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

    public BedrockItemDisplayBlocks itemDisplayBlocks() {
        return this.itemDisplayBlocks;
    }

    public BedrockPlayerAnimations playerAnimations() {
        return this.playerAnimations;
    }
}
