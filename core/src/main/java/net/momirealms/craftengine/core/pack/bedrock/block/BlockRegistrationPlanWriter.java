package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemBlocks.DisplayBlock;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class BlockRegistrationPlanWriter {

    private BlockRegistrationPlanWriter() {
    }

    public static void write(BedrockItemResourceGenerator.Result generatedItems, JsonArray worldBlockEntries,
                             List<WorldBlockSource> worldBlocks, Path blockPlanPath) throws IOException {
        JsonArray entries = new JsonArray();
        for (DisplayBlock block : collectDisplayBlocks(generatedItems)) {
            entries.add(displayBlockToJson(block));
        }
        // 世界方块条目已包含 java_state 和 server_states，按生成顺序原样追加。
        entries.addAll(worldBlockEntries);

        JsonObject plan = new JsonObject();
        plan.addProperty(KIND, RegistrationPlanKind.BLOCK.id());
        plan.addProperty(FORMAT_VERSION, RegistrationPlanKind.BLOCK.formatVersion());
        plan.add(ENTRIES, entries);
        plan.add(WORLD_STATE_MAPPINGS, worldStateMappings(worldBlocks));
        GsonHelper.writeJsonFile(GsonHelper.sortObjectKeys(plan), blockPlanPath);
    }

    private static JsonObject worldStateMappings(List<WorldBlockSource> sources) {
        JsonObject mappings = new JsonObject();
        for (WorldBlockSource source : sources) {
            if (source.visual() == null) {
                continue;
            }
            String visualState = source.visual().getAsString();
            JsonArray serverIds = mappings.getAsJsonArray(visualState);
            if (serverIds == null) {
                serverIds = new JsonArray();
                mappings.add(visualState, serverIds);
            }
            serverIds.add(source.serverStateId());
        }
        return mappings;
    }

    private static Collection<DisplayBlock> collectDisplayBlocks(BedrockItemResourceGenerator.Result generatedItems) {
        // 多个物品变体可以引用同名展示方块。保留首次出现的定义，按名称排序以稳定输出。
        Map<String, DisplayBlock> blocksByName = new TreeMap<>();
        for (List<GeneratedItemVariant> variants : generatedItems.variants().values()) {
            for (GeneratedItemVariant variant : variants) {
                DisplayBlock block = variant.displayBlock();
                if (block != null) {
                    blocksByName.putIfAbsent(block.name(), block);
                }
            }
        }
        return blocksByName.values();
    }

    private static JsonObject displayBlockToJson(DisplayBlock block) {
        JsonObject entry = new JsonObject();
        entry.addProperty(NAME, block.name());
        entry.addProperty(GEOMETRY, block.geometry());
        JsonObject materials = new JsonObject();
        block.materials().forEach(materials::addProperty);
        entry.add(MATERIALS, materials);
        return entry;
    }
}
