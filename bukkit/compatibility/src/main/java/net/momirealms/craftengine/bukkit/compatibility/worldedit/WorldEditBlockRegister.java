package net.momirealms.craftengine.bukkit.compatibility.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitBlockRegistry;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class WorldEditBlockRegister {
    private static final Field field$BlockType$blockMaterial = ReflectionUtils.getDeclaredField(BlockType.class, "blockMaterial");
    private static boolean init = false;
    private WorldEditBlockRegister() {}

    public static void init(boolean isFAWE) {
        if (init) {
            throw new IllegalStateException("WorldEditBlockRegister has already been initialized");
        }
        init = true;
        WorldEdit.getInstance().getBlockFactory().register(new CEBlockParser(isFAWE));
        if (isFAWE) {
            FastAsyncWorldEditDelegate.init();
        }
    }

    public static boolean checkFAWECompatible(String version) {
        BlockType blockType = BlockType.REGISTRY.get("craftengine:custom_0");
        if (blockType != null) { // 通过其他办法兼容直接返回成功
            return true;
        }
        String cleanVersion = version.split("-")[0];
        String[] parts = cleanVersion.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        return first >= 2 && second >= 13;
    }

    @SuppressWarnings("deprecation")
    public static void register(Key id) throws ReflectiveOperationException {
        String string = id.asString();
        BlockType blockType = new BlockType(string, blockState -> blockState);
        field$BlockType$blockMaterial.set(blockType, LazyReference.from(() -> new BukkitBlockRegistry.BukkitBlockMaterial(null, Material.STONE)));
        if (BlockType.REGISTRY.get(string) != null) { // already registered
            return;
        }
        BlockType.REGISTRY.register(string, blockType);
    }

    private static final class CEBlockParser extends InputParser<BaseBlock> {
        private final boolean isFAWE;

        private CEBlockParser(boolean isFAWE) {
            super(WorldEdit.getInstance());
            this.isFAWE = isFAWE;
        }

        @Override
        public Stream<String> getSuggestions(String input, ParserContext context) {
            Set<String> namespacesInUse = BukkitBlockManager.instance().namespacesInUse();

            if (input.isEmpty() || input.equals(":")) {
                return namespacesInUse.stream().map(namespace -> namespace + ":");
            }

            if (input.startsWith(":")) {
                String term = input.substring(1);
                return BlockStateParser.fillSuggestions(term).stream();
            }

            if (!input.contains(":")) {
                String lowerSearch = input.toLowerCase(Locale.ROOT);
                return Stream.concat(
                        namespacesInUse.stream().filter(n -> n.startsWith(lowerSearch)).map(n -> n + ":"),
                        BlockStateParser.fillSuggestions(input).stream()
                );
            }
            return BlockStateParser.fillSuggestions(input).stream();
        }

        @Override
        public BaseBlock parseFromInput(String input, ParserContext context) {
            if (this.isFAWE) {
                int index = input.indexOf("[");
                if (input.charAt(index + 1) == ']') return null;
            }

            int colonIndex = input.indexOf(':');
            if (colonIndex == -1) return null;

            Set<String> namespacesInUse = BukkitBlockManager.instance().namespacesInUse();
            String namespace = input.substring(0, colonIndex);
            if (!namespacesInUse.contains(namespace)) return null;

            ImmutableBlockState state = BlockStateParser.deserialize(input);
            if (state == null) return null;

            try {
                String id = state.customBlockState().literalObject().toString();
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
