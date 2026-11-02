package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class BlockRegistrationPlanWriter {

    private BlockRegistrationPlanWriter() {
    }

    public static void write(BedrockItemResourceGenerator.Result generated, Path path) throws IOException {
        write(generated, new JsonArray(), path);
    }

    public static void write(BedrockItemResourceGenerator.Result generated, JsonArray world, Path path) throws IOException {
        Map<String, BedrockItemBlocks.DisplayBlock> blocks = new TreeMap<>();
        for (List<GeneratedItemVariant> variants : generated.variants().values()) {
            for (GeneratedItemVariant variant : variants) {
                BedrockItemBlocks.DisplayBlock block = variant.displayBlock();
                if (block != null) {
                    blocks.putIfAbsent(block.name(), block);
                }
            }
        }
        JsonArray entries = new JsonArray();
        for (BedrockItemBlocks.DisplayBlock block : blocks.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty(NAME, block.name());
            entry.addProperty(GEOMETRY, block.geometry());
            JsonObject materials = new JsonObject();
            block.materials().forEach(materials::addProperty);
            entry.add(MATERIALS, materials);
            entries.add(entry);
        }
        JsonObject result = new JsonObject();
        entries.addAll(world);
        result.addProperty(KIND, RegistrationPlanKind.BLOCK.id());
        result.addProperty(FORMAT_VERSION, RegistrationPlanKind.BLOCK.formatVersion());
        result.add(ENTRIES, entries);
        GsonHelper.writeJsonFile(GsonHelper.sortObjectKeys(result), path);
    }
}
