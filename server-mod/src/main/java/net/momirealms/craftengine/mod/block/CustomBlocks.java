package net.momirealms.craftengine.mod.block;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.momirealms.craftengine.mod.CraftEnginePlugin;
import net.momirealms.craftengine.mod.util.NoteBlockUtils;
import net.momirealms.craftengine.mod.util.Reflections;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomBlocks {

    public static void register() {
        CraftEnginePlugin.setVanillaRegistrySize(Block.BLOCK_STATE_REGISTRY.size());
        ResourceLocation noteBlock = ResourceLocation.fromNamespaceAndPath("minecraft", "note_block");
        Map<ResourceLocation, Integer> map = loadMappingsAndAdditionalBlocks();
        for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
            ResourceLocation replacedBlockId = entry.getKey();
            boolean isNoteBlock = replacedBlockId.equals(noteBlock);
            try {
                Block replacedBlock = (Block) Reflections.method$DefaultedRegistry$get.invoke(BuiltInRegistries.BLOCK, replacedBlockId);
                for (int i = 0; i < entry.getValue(); i++) {
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath("craftengine", replacedBlockId.getPath() + "_" + i);
                    ResourceKey<Block> resourceKey = ResourceKey.create(Registries.BLOCK, location);
                    BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
                    if (Reflections.field$BlockBehaviour$Properties$id != null) {
                        Reflections.field$BlockBehaviour$Properties$id.set(properties, resourceKey);
                    }
                    if (!replacedBlock.hasCollision) {
                        properties.noCollission();
                    }
                    CraftEngineBlock block = new CraftEngineBlock(properties);
                    if (isNoteBlock) {
                        block.setNoteBlock(true);
                        NoteBlockUtils.CLIENT_SIDE_NOTE_BLOCKS.add(block.defaultBlockState());
                    }
                    Registry.register(BuiltInRegistries.BLOCK, location, block);
                    Block.BLOCK_STATE_REGISTRY.add(block.defaultBlockState());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        NoteBlockUtils.CLIENT_SIDE_NOTE_BLOCKS.addAll(net.minecraft.world.level.block.Blocks.NOTE_BLOCK.getStateDefinition().getPossibleStates());
        if (!map.isEmpty()) {
            CraftEnginePlugin.setIsSuccessfullyRegistered(true);
        }
    }

    private static Map<ResourceLocation, Integer> loadMappingsAndAdditionalBlocks() {
        Path mappingPath = CraftEnginePlugin.getCraftEngineMappingsPath();
        if (!Files.exists(mappingPath)) return Map.of();
        YamlConfiguration mappings = YamlConfiguration.loadConfiguration(mappingPath.toFile());
        Map<String, String> blockStateMappings = loadBlockStateMappings(mappings);
        validateBlockStateMappings(blockStateMappings);
        Map<ResourceLocation, Integer> blockTypeCounter = new LinkedHashMap<>();
        Map<Integer, Integer> appearanceMapper = new HashMap<>();
        for (Map.Entry<String, String> entry : blockStateMappings.entrySet()) {
            processBlockStateMapping(entry, appearanceMapper, blockTypeCounter);
        }
        YamlConfiguration additionalYaml = YamlConfiguration.loadConfiguration(CraftEnginePlugin.getCraftEngineAdditionalBlocksPath().toFile());
        return buildRegisteredRealBlockSlots(blockTypeCounter, additionalYaml);
    }

    private static Map<String, String> loadBlockStateMappings(YamlConfiguration mappings) {
        Map<String, String> blockStateMappings = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : mappings.getValues(false).entrySet()) {
            if (entry.getValue() instanceof String afterValue) {
                blockStateMappings.put(entry.getKey(), afterValue);
            }
        }
        return blockStateMappings;
    }

    private static void validateBlockStateMappings(Map<String, String> blockStateMappings) {
        Map<String, String> temp = new HashMap<>(blockStateMappings);
        for (Map.Entry<String, String> entry : temp.entrySet()) {
            String state = entry.getValue();
            blockStateMappings.remove(state);
        }
    }

    private static LinkedHashMap<ResourceLocation, Integer> buildRegisteredRealBlockSlots(Map<ResourceLocation, Integer> counter, YamlConfiguration additionalYaml) {
        LinkedHashMap<ResourceLocation, Integer> map = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : counter.entrySet()) {
            String id = entry.getKey().toString();
            int additionalStates = additionalYaml.getInt(id, 0);
            int internalIds = entry.getValue() + additionalStates;
            map.put(entry.getKey(), internalIds);
        }
        return map;
    }

    private static void processBlockStateMapping(Map.Entry<String, String> entry, Map<Integer, Integer> mapper, Map<ResourceLocation, Integer> counter) {
        BlockState before = createBlockData(entry.getKey());
        BlockState after = createBlockData(entry.getValue());
        if (before == null || after == null) return;

        int beforeId = Block.BLOCK_STATE_REGISTRY.getId(before);
        int afterId = Block.BLOCK_STATE_REGISTRY.getId(after);

        Integer previous = mapper.put(beforeId, afterId);
        if (previous == null) {
            counter.compute(BuiltInRegistries.BLOCK.getKey(before.getBlock()), (k, count) -> count == null ? 1 : count + 1);
        }
    }

    private static BlockState createBlockData(String blockState) {
        try {
            Object holderLookUp = BuiltInRegistries.BLOCK;
            if (Reflections.method$Registry$asLookup != null) {
                holderLookUp = Reflections.method$Registry$asLookup.invoke(holderLookUp);
            }
            BlockStateParser.BlockResult result = (BlockStateParser.BlockResult) Reflections.method$BlockStateParser$parseForBlock.invoke(null, holderLookUp, blockState, false);
            return result.blockState();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
