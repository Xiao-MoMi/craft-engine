package net.momirealms.craftengine.mod.mixin;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.momirealms.craftengine.mod.CraftEngineBlock;
import net.momirealms.craftengine.mod.CraftEnginePlugin;
import net.momirealms.craftengine.mod.util.NoteBlockUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(value = Blocks.class)
public abstract class MixinBlocks {

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void onBlocksInit(CallbackInfo ci) {
        CraftEnginePlugin.setVanillaRegistrySize(Block.BLOCK_STATE_REGISTRY.size());
        ResourceLocation noteBlock = ResourceLocation.fromNamespaceAndPath("minecraft", "note_block");
        Map<ResourceLocation, Integer> map = loadMappingsAndAdditionalBlocks();
        for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
            ResourceLocation replacedBlockId = entry.getKey();
            boolean isNoteBlock = replacedBlockId.equals(noteBlock);
            Block replacedBlock = BuiltInRegistries.BLOCK.getValue(replacedBlockId);
            for (int i = 0; i < entry.getValue(); i++) {
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath("craftengine", replacedBlockId.getPath() + "_" + i);
                ResourceKey<Block> resourceKey = ResourceKey.create(Registries.BLOCK, location);
                BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                        .setId(resourceKey);
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
        }
        NoteBlockUtils.CLIENT_SIDE_NOTE_BLOCKS.addAll(Blocks.NOTE_BLOCK.getStateDefinition().getPossibleStates());
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
            StringReader reader = new StringReader(blockState);
            BlockStateParser.BlockResult arg = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, reader, false);
            return arg.blockState();
        } catch (Exception e) {
            return null;
        }
    }
}
