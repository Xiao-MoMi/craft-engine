package net.momirealms.craftengine.bukkit.compatibility.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitBlockRegistry;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.momirealms.craftengine.core.block.AbstractBlockManager;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Stream;

public class WorldEditBlockRegister {
    private final Field field$BlockType$blockMaterial;
    private final AbstractBlockManager manager;
    private final boolean isFAWE;

    public WorldEditBlockRegister(AbstractBlockManager manager, boolean isFAWE) {
        this.field$BlockType$blockMaterial = ReflectionUtils.getDeclaredField(BlockType.class, "blockMaterial");
        this.manager = manager;
        this.isFAWE = isFAWE;
        CEBlockParser blockParser = new CEBlockParser(WorldEdit.getInstance());
        WorldEdit.getInstance().getBlockFactory().register(blockParser);
        if (isFAWE) {
            FastAsyncWorldEditDelegate.init();
        }
    }

    @SuppressWarnings("deprecation")
    public void register(Key id) throws ReflectiveOperationException {
        BlockType blockType = new BlockType(id.toString(), blockState -> blockState);
        this.field$BlockType$blockMaterial.set(blockType, LazyReference.from(() -> new BukkitBlockRegistry.BukkitBlockMaterial(null, Material.STONE)));
        BlockType.REGISTRY.register(id.toString(), blockType);
    }

    private final class CEBlockParser extends InputParser<BaseBlock> {

        private CEBlockParser(WorldEdit worldEdit) {
            super(worldEdit);
        }

        @Override
        @SuppressWarnings("deprecation")
        public Stream<String> getSuggestions(String input) {
            Set<String> namespacesInUse = manager.namespacesInUse();

            if (input.isEmpty() || input.equals(":")) {
                return namespacesInUse.stream().map(namespace -> namespace + ":");
            }

            if (input.startsWith(":")) {
                String term = input.substring(1);
                return BlockStateParser.fillSuggestions(term).stream();
            }

            if (!input.contains(":")) {
                String lowerSearch = input.toLowerCase();
                return Stream.concat(
                        namespacesInUse.stream().filter(n -> n.startsWith(lowerSearch)).map(n -> n + ":"),
                        BlockStateParser.fillSuggestions(input).stream()
                );
            }
            return BlockStateParser.fillSuggestions(input).stream();
        }

        @Override
        public BaseBlock parseFromInput(String input, ParserContext context) {
            if (isFAWE) {
                int index = input.indexOf("[");
                if (input.charAt(index+1) == ']') return null;
            }

            int colonIndex = input.indexOf(':');
            if (colonIndex == -1) return null;

            Set<String> namespacesInUse = manager.namespacesInUse();
            String namespace = input.substring(0, colonIndex);
            if (!namespacesInUse.contains(namespace)) return null;

            ImmutableBlockState state = BlockStateParser.deserialize(input);
            if (state == null) return null;

            try {
                String id = state.customBlockState().handle().toString();
                int first = id.indexOf('{');
                int last = id.indexOf('}');
                if (first != -1 && last != -1 && last > first) {
                    String blockId = id.substring(first + 1, last);
                    BlockType blockType = BlockTypes.get(blockId);
                    if (blockType == null) {
                        return null;
                    }
                    return blockType.getDefaultState().toBaseBlock();
                } else {
                    throw new IllegalArgumentException("Invalid block ID format: " + id);
                }
            } catch (NullPointerException e) {
                return null;
            }
        }
    }
}
