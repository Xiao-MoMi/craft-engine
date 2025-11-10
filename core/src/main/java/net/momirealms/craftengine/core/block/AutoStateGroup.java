package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.pack.allocator.BlockStateCandidate;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public enum AutoStateGroup {
    NON_TINTABLE_LEAVES(List.of("no_tint_leaves", "leaves_no_tint", "non_tintable_leaves"),
            Set.of(BlockKeys.CHERRY_LEAVES, BlockKeys.PALE_OAK_LEAVES, BlockKeys.AZALEA_LEAVES, BlockKeys.FLOWERING_AZALEA_LEAVES),
            (w) -> !(boolean) w.getProperty("waterlogged")
    ),
    WATERLOGGED_NON_TINTABLE_LEAVES(
            List.of("waterlogged_no_tint_leaves", "waterlogged_leaves_no_tint", "waterlogged_non_tintable_leaves"),
            Set.of(BlockKeys.CHERRY_LEAVES, BlockKeys.PALE_OAK_LEAVES, BlockKeys.AZALEA_LEAVES, BlockKeys.FLOWERING_AZALEA_LEAVES),
            (w) -> w.getProperty("waterlogged")
    ),
    TINTABLE_LEAVES("tintable_leaves",
            Set.of(BlockKeys.OAK_LEAVES, BlockKeys.JUNGLE_LEAVES, BlockKeys.ACACIA_LEAVES, BlockKeys.DARK_OAK_LEAVES, BlockKeys.MANGROVE_LEAVES),
            (w) -> !(boolean) w.getProperty("waterlogged")
    ),
    WATERLOGGED_TINTABLE_LEAVES(
            "waterlogged_tintable_leaves",
            Set.of(BlockKeys.OAK_LEAVES, BlockKeys.JUNGLE_LEAVES, BlockKeys.ACACIA_LEAVES, BlockKeys.DARK_OAK_LEAVES, BlockKeys.MANGROVE_LEAVES),
            (w) -> w.getProperty("waterlogged")
    ),
    LEAVES("leaves",
            Set.of(BlockKeys.OAK_LEAVES, BlockKeys.BIRCH_LEAVES, BlockKeys.JUNGLE_LEAVES, BlockKeys.ACACIA_LEAVES, BlockKeys.DARK_OAK_LEAVES, BlockKeys.MANGROVE_LEAVES,
                    BlockKeys.SPRUCE_LEAVES, BlockKeys.CHERRY_LEAVES, BlockKeys.PALE_OAK_LEAVES, BlockKeys.AZALEA_LEAVES, BlockKeys.FLOWERING_AZALEA_LEAVES),
            (w) -> !(boolean) w.getProperty("waterlogged")
    ),
    WATERLOGGED_LEAVES(
            "waterlogged_leaves",
            Set.of(BlockKeys.OAK_LEAVES, BlockKeys.BIRCH_LEAVES, BlockKeys.JUNGLE_LEAVES, BlockKeys.ACACIA_LEAVES, BlockKeys.DARK_OAK_LEAVES, BlockKeys.MANGROVE_LEAVES,
                    BlockKeys.SPRUCE_LEAVES, BlockKeys.CHERRY_LEAVES, BlockKeys.PALE_OAK_LEAVES, BlockKeys.AZALEA_LEAVES, BlockKeys.FLOWERING_AZALEA_LEAVES),
            (w) -> w.getProperty("waterlogged")
    ),
    LOWER_TRIPWIRE("lower_tripwire", Set.of(BlockKeys.TRIPWIRE), (w) -> w.getProperty("attached")),
    HIGHER_TRIPWIRE("higher_tripwire", Set.of(BlockKeys.TRIPWIRE), (w) -> !(boolean) w.getProperty("attached")),
    NOTE_BLOCK("note_block", Set.of(BlockKeys.NOTE_BLOCK), (w) -> true),
    BROWN_MUSHROOM("brown_mushroom_block", Set.of(BlockKeys.BROWN_MUSHROOM_BLOCK), (w) -> true),
    RED_MUSHROOM("red_mushroom_block", Set.of(BlockKeys.RED_MUSHROOM_BLOCK), (w) -> true),
    MUSHROOM_STEM("mushroom_stem", Set.of(BlockKeys.MUSHROOM_STEM), (w) -> true),
    TRIPWIRE("tripwire", Set.of(BlockKeys.TRIPWIRE), (w) -> true),
    SUGAR_CANE("sugar_cane", Set.of(BlockKeys.SUGAR_CANE), (w) -> true),
    CACTUS("cactus", Set.of(BlockKeys.CACTUS), (w) -> true),
    SAPLING("sapling", Set.of(BlockKeys.OAK_SAPLING, BlockKeys.SPRUCE_SAPLING, BlockKeys.BIRCH_SAPLING, BlockKeys.JUNGLE_SAPLING, BlockKeys.ACACIA_SAPLING, BlockKeys.DARK_OAK_SAPLING, BlockKeys.CHERRY_SAPLING, BlockKeys.PALE_OAK_SAPLING), (w) -> true),
    MUSHROOM("mushroom", Set.of(BlockKeys.BROWN_MUSHROOM_BLOCK, BlockKeys.RED_MUSHROOM_BLOCK, BlockKeys.MUSHROOM_STEM), (w) -> true),
    SOLID("solid", Set.of(BlockKeys.BROWN_MUSHROOM_BLOCK, BlockKeys.RED_MUSHROOM_BLOCK, BlockKeys.MUSHROOM_STEM, BlockKeys.NOTE_BLOCK), (w) -> true);

    private final Set<Key> blocks;
    private final List<String> id;
    private final Predicate<BlockStateWrapper> predicate;
    private final List<BlockStateCandidate> candidates = new ArrayList<>();
    private int pointer;

    AutoStateGroup(String id, Set<Key> blocks, Predicate<BlockStateWrapper> predicate) {
        this.id = List.of(id);
        this.blocks = blocks;
        this.predicate = predicate;
    }

    AutoStateGroup(List<String> id, Set<Key> blocks, Predicate<BlockStateWrapper> predicate) {
        this.id = id;
        this.blocks = blocks;
        this.predicate = predicate;
    }

    public void reset() {
        this.pointer = 0;
        this.candidates.clear();
    }

    public void addCandidate(@NotNull BlockStateCandidate candidate) {
        this.candidates.add(candidate);
    }

    public int candidateCount() {
        return candidates.size();
    }

    @Nullable
    public BlockStateCandidate findNextCandidate() {
        while (this.pointer < this.candidates.size()) {
            final BlockStateCandidate state = this.candidates.get(this.pointer);
            if (!state.isUsed()) {
                return state;
            }
            this.pointer++;
        }
        return null;
    }

    public boolean test(BlockStateWrapper state) {
        if (!this.blocks.contains(state.ownerId())) {
            return false;
        }
        return this.predicate.test(state);
    }

    public Set<Key> blocks() {
        return blocks;
    }

    public String id() {
        return id.getFirst();
    }

    public List<String> ids() {
        return id;
    }

    private static final Map<String, AutoStateGroup> BY_ID = new HashMap<>();
    private static final Map<Key, List<AutoStateGroup>> BY_BLOCKS = new HashMap<>();

    static {
        for (AutoStateGroup group : AutoStateGroup.values()) {
            for (String id : group.ids()) {
                BY_ID.put(id, group);
                for (Key key : group.blocks) {
                    BY_BLOCKS.computeIfAbsent(key, k -> new ArrayList<>(4)).add(group);
                }
            }
        }
    }

    @Nullable
    public static AutoStateGroup byId(String id) {
        return BY_ID.get(id);
    }

    public static List<AutoStateGroup> findGroups(BlockStateWrapper wrapper) {
        return findGroups(wrapper.ownerId(), wrapper);
    }

    public static List<AutoStateGroup> findGroups(Key id, BlockStateWrapper wrapper) {
        List<AutoStateGroup> groups = BY_BLOCKS.get(id);
        if (groups == null) return Collections.emptyList();
        List<AutoStateGroup> result = new ArrayList<>(groups.size());
        for (AutoStateGroup group : groups) {
            if (group.predicate.test(wrapper)) result.add(group);
        }
        return result;
    }
}
