package net.momirealms.craftengine.bukkit.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.BlockGenerator;
import net.momirealms.craftengine.bukkit.plugin.network.PacketConsumers;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.bukkit.util.TagUtils;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.behavior.EmptyBlockBehavior;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.plugin.config.StringKeyConstructor;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.Sounds;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BukkitBlockManager extends AbstractBlockManager {
    private static BukkitBlockManager instance;
    private final BukkitCraftEngine plugin;

    // The total amount of blocks registered
    private int customBlockCount;
    private ImmutableBlockState[] stateId2ImmutableBlockStates;
    // Minecraft objects
    // Cached new blocks $ holders
    private Map<Integer, Object> stateId2BlockHolder;
    // This map is used to change the block states that are not necessarily needed into a certain block state
    private Map<Integer, Integer> blockAppearanceMapper;
    // Record the amount of real blocks by block type
    private Map<Key, Integer> registeredRealBlockSlots;
    // A set of blocks that sounds have been removed
    private Set<Object> affectedSoundBlocks;
    private Map<Object, Pair<SoundData, SoundData>> affectedOpenableBlockSounds;
    private Map<Key, Key> soundMapper;
    // A list to record the order of registration
    private List<Key> blockRegisterOrder = new ObjectArrayList<>();
    // Event listeners
    private BlockEventListener blockEventListener;
    private FallingBlockRemoveListener fallingBlockRemoveListener;
    // cached tag packet
    private Object cachedUpdateTagsPacket;

    private final List<Tuple<Object, Key, Boolean>> blocksToDeceive = new ArrayList<>();

    public BukkitBlockManager(BukkitCraftEngine plugin) {
        super(plugin);
        instance = this;
        this.plugin = plugin;
        this.initVanillaRegistry();
        this.loadMappingsAndAdditionalBlocks();
        this.registerBlocks();
        this.registerEmptyBlock();
    }

    @Override
    public void init() {
        this.initMirrorRegistry();
        this.deceiveBukkit();
        boolean enableNoteBlocks = this.blockAppearanceArranger.containsKey(BlockKeys.NOTE_BLOCK);
        this.blockEventListener = new BlockEventListener(plugin, this, enableNoteBlocks);
        if (enableNoteBlocks) {
            this.recordVanillaNoteBlocks();
        }
        this.fallingBlockRemoveListener = VersionHelper.isOrAbove1_20_3() ? new FallingBlockRemoveListener() : null;
        this.stateId2ImmutableBlockStates = new ImmutableBlockState[this.customBlockCount];
        Arrays.fill(this.stateId2ImmutableBlockStates, EmptyBlock.INSTANCE.defaultState());
        this.resetPacketConsumers();
    }

    @Override
    public String stateRegistryIdToStateSNBT(int id) {
        return BlockStateUtils.idToBlockState(id).toString();
    }

    public static BukkitBlockManager instance() {
        return instance;
    }

    public List<Key> blockRegisterOrder() {
        return Collections.unmodifiableList(this.blockRegisterOrder);
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.blockEventListener, this.plugin.javaPlugin());
        if (this.fallingBlockRemoveListener != null) {
            Bukkit.getPluginManager().registerEvents(this.fallingBlockRemoveListener, this.plugin.javaPlugin());
        }
    }

    @Override
    public void unload() {
        super.unload();
        if (EmptyBlock.STATE != null)
            Arrays.fill(this.stateId2ImmutableBlockStates, EmptyBlock.STATE);
        for (DelegatingBlock block : this.registeredBlocks.values()) {
            block.behaviorDelegate().bindValue(EmptyBlockBehavior.INSTANCE);
            block.shapeDelegate().bindValue(BukkitBlockShape.STONE);
            DelegatingBlockState state = (DelegatingBlockState) FastNMS.INSTANCE.method$Block$defaultState(block);
            state.setBlockState(null);
        }
    }

    @Override
    public void disable() {
        this.unload();
        HandlerList.unregisterAll(this.blockEventListener);
        if (this.fallingBlockRemoveListener != null) HandlerList.unregisterAll(this.fallingBlockRemoveListener);
    }

    @Override
    public Map<Key, Key> soundMapper() {
        return this.soundMapper;
    }

    @Override
    public void delayedLoad() {
        this.resetPacketConsumers();
        super.delayedLoad();
    }

    @Override
    protected void resendTags() {
        // if there's no change
        if (this.clientBoundTags.equals(this.previousClientBoundTags)) return;
        List<TagUtils.TagEntry> list = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : this.clientBoundTags.entrySet()) {
            list.add(new TagUtils.TagEntry(entry.getKey(), entry.getValue()));
        }
        Object packet = TagUtils.createUpdateTagsPacket(Map.of(MRegistries.BLOCK, list));
        for (BukkitServerPlayer player : this.plugin.networkManager().onlineUsers()) {
            player.sendPacket(packet, false);
        }
        // 如果空，那么新来的玩家就没必要收到更新包了
        if (list.isEmpty()) {
            this.cachedUpdateTagsPacket = null;
        } else {
            this.cachedUpdateTagsPacket = packet;
        }
    }

    @Nullable
    @Override
    public BlockStateWrapper createPackedBlockState(String blockState) {
        ImmutableBlockState state = BlockStateParser.deserialize(blockState);
        if (state != null) {
            return state.customBlockState();
        }
        try {
            BlockData blockData = Bukkit.createBlockData(blockState);
            return BlockStateUtils.toPackedBlockState(blockData);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public Object getMinecraftBlockHolder(int stateId) {
        return this.stateId2BlockHolder.get(stateId);
    }

    @NotNull
    @Override
    public ImmutableBlockState getImmutableBlockStateUnsafe(int stateId) {
        return this.stateId2ImmutableBlockStates[stateId - BlockStateUtils.vanillaStateSize()];
    }

    @Nullable
    @Override
    public ImmutableBlockState getImmutableBlockState(int stateId) {
        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            return this.stateId2ImmutableBlockStates[stateId - BlockStateUtils.vanillaStateSize()];
        }
        return null;
    }

    @Override
    public void addBlockInternal(Key id, CustomBlock customBlock) {
        // bind appearance and real state
        for (ImmutableBlockState state : customBlock.variantProvider().states()) {
            ImmutableBlockState previous = this.stateId2ImmutableBlockStates[state.customBlockState().registryId() - BlockStateUtils.vanillaStateSize()];
            if (previous != null && !previous.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.block.state.bind_failed", state.toString(), previous.toString());
            }
            this.stateId2ImmutableBlockStates[state.customBlockState().registryId() - BlockStateUtils.vanillaStateSize()] = state;
            this.tempBlockAppearanceConvertor.put(state.customBlockState().registryId(), state.vanillaBlockState().registryId());
            this.appearanceToRealState.computeIfAbsent(state.vanillaBlockState().registryId(), k -> new IntArrayList()).add(state.customBlockState().registryId());
        }
        super.addBlockInternal(id, customBlock);
    }

    @Override
    public Key getBlockOwnerId(BlockStateWrapper state) {
        return BlockStateUtils.getBlockOwnerIdFromState(state.handle());
    }

    @Override
    protected Key getBlockOwnerId(int id) {
        return BlockStateUtils.getBlockOwnerIdFromState(BlockStateUtils.idToBlockState(id));
    }

    @Override
    public int availableAppearances(Key blockType) {
        return Optional.ofNullable(this.registeredRealBlockSlots.get(blockType)).orElse(0);
    }

    @NotNull
    public Map<Key, List<Integer>> blockAppearanceArranger() {
        return this.blockAppearanceArranger;
    }

    @NotNull
    public Map<Key, List<Integer>> realBlockArranger() {
        return this.realBlockArranger;
    }

    private void initMirrorRegistry() {
        int size = RegistryUtils.currentBlockRegistrySize();
        BlockStateWrapper[] states = new BlockStateWrapper[size];
        for (int i = 0; i < size; i++) {
            states[i] = BlockStateWrapper.create(BlockStateUtils.idToBlockState(i), i, BlockStateUtils.isVanillaBlock(i));
        }
        BlockRegistryMirror.init(states, BlockStateWrapper.vanilla(MBlocks.STONE$defaultState, BlockStateUtils.blockStateToId(MBlocks.STONE$defaultState)));
    }

    private void registerEmptyBlock() {
        Holder.Reference<CustomBlock> holder = ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).registerForHolder(ResourceKey.create(BuiltInRegistries.BLOCK.key().location(), Key.withDefaultNamespace("empty")));
        EmptyBlock emptyBlock = new EmptyBlock(Key.withDefaultNamespace("empty"), holder);
        holder.bindValue(emptyBlock);
    }

    private void resetPacketConsumers() {
        Map<Integer, Integer> finalMapping = new HashMap<>(this.blockAppearanceMapper);
        int stoneId = BlockStateUtils.blockStateToId(MBlocks.STONE$defaultState);
        for (int custom : this.internalId2StateId.values()) {
            finalMapping.put(custom, stoneId);
        }
        finalMapping.putAll(this.tempBlockAppearanceConvertor);
        PacketConsumers.initBlocks(finalMapping, RegistryUtils.currentBlockRegistrySize());
    }

    private void initVanillaRegistry() {
        int vanillaStateCount = RegistryUtils.currentBlockRegistrySize();
        this.plugin.logger().info("Vanilla block count: " + vanillaStateCount);
        BlockStateUtils.init(vanillaStateCount);
    }

    @Override
    protected CustomBlock.Builder platformBuilder(Key id) {
        return BukkitCustomBlock.builder(id);
    }

    @SuppressWarnings("unchecked")
    private void registerBlocks() {
        this.plugin.logger().info("Registering blocks. Please wait...");
        try {
            ImmutableMap.Builder<Key, Integer> builder1 = ImmutableMap.builder();
            ImmutableMap.Builder<Integer, Object> builder2 = ImmutableMap.builder();
            ImmutableMap.Builder<Key, List<Integer>> builder3 = ImmutableMap.builder();
            ImmutableMap.Builder<Key, DelegatingBlock> builder4 = ImmutableMap.builder();
            Set<Object> affectedBlockSounds = new HashSet<>();
            Map<Object, Pair<SoundData, SoundData>> affectedDoors = new IdentityHashMap<>();
            Set<Object> affectedBlocks = new HashSet<>();
            List<Key> order = new ArrayList<>();

            unfreezeRegistry();

            int counter = 0;
            for (Map.Entry<Key, Integer> baseBlockAndItsCount : this.registeredRealBlockSlots.entrySet()) {
                counter = registerBlockVariants(baseBlockAndItsCount, counter, builder1, builder2, builder3, builder4, affectedBlockSounds, order);
            }

            freezeRegistry();
            this.plugin.logger().info("Registered block count: " + counter);
            this.customBlockCount = counter;
            this.internalId2StateId = builder1.build();
            this.stateId2BlockHolder = builder2.build();
            this.realBlockArranger = builder3.build();
            this.registeredBlocks = builder4.build();
            this.blockRegisterOrder = ImmutableList.copyOf(order);
            if (MCUtils.ceilLog2(BlockStateUtils.vanillaStateSize() + counter) == MCUtils.ceilLog2(BlockStateUtils.vanillaStateSize())) {
                PalettedContainer.NEED_DOWNGRADE = false;
            }
            for (Object block : (Iterable<Object>) MBuiltInRegistries.BLOCK) {
                Object soundType = CoreReflections.field$BlockBehaviour$soundType.get(block);
                if (affectedBlockSounds.contains(soundType)) {
                    Object state = FastNMS.INSTANCE.method$Block$defaultState(block);
                    if (BlockStateUtils.isVanillaBlock(state)) {
                        affectedBlocks.add(block);
                    }
                }
            }

            affectedBlocks.remove(MBlocks.FIRE);
            affectedBlocks.remove(MBlocks.SOUL_FIRE);

            this.affectedSoundBlocks = ImmutableSet.copyOf(affectedBlocks);

            ImmutableMap.Builder<Key, Key> soundMapperBuilder = ImmutableMap.builder();
            for (Object soundType : affectedBlockSounds) {
                for (Field field : List.of(CoreReflections.field$SoundType$placeSound, CoreReflections.field$SoundType$fallSound, CoreReflections.field$SoundType$hitSound, CoreReflections.field$SoundType$stepSound, CoreReflections.field$SoundType$breakSound)) {
                    Object soundEvent = field.get(soundType);
                    Key previousId = Key.of(FastNMS.INSTANCE.field$SoundEvent$location(soundEvent).toString());
                    soundMapperBuilder.put(previousId, Key.of(previousId.namespace(), "replaced." + previousId.value()));
                }
            }

            Predicate<Key> predicate = it -> this.realBlockArranger.containsKey(it);
            Consumer<Key> soundCallback = s -> soundMapperBuilder.put(s, Key.of("replaced." + s.value()));
            BiConsumer<Object, Pair<SoundData, SoundData>> affectedBlockCallback = affectedDoors::put;
            Function<Key, SoundData> soundMapper = (k) -> SoundData.of(k, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f));
            collectDoorSounds(predicate, Sounds.WOODEN_TRAPDOOR_OPEN, Sounds.WOODEN_TRAPDOOR_CLOSE, Sounds.WOODEN_TRAPDOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.NETHER_WOOD_TRAPDOOR_OPEN, Sounds.NETHER_WOOD_TRAPDOOR_CLOSE, Sounds.NETHER_TRAPDOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.BAMBOO_WOOD_TRAPDOOR_OPEN, Sounds.BAMBOO_WOOD_TRAPDOOR_CLOSE, Sounds.BAMBOO_TRAPDOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.CHERRY_WOOD_TRAPDOOR_OPEN, Sounds.CHERRY_WOOD_TRAPDOOR_CLOSE, Sounds.CHERRY_TRAPDOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.COPPER_TRAPDOOR_OPEN, Sounds.COPPER_TRAPDOOR_CLOSE, Sounds.COPPER_TRAPDOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.WOODEN_DOOR_OPEN, Sounds.WOODEN_DOOR_CLOSE, Sounds.WOODEN_DOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.NETHER_WOOD_DOOR_OPEN, Sounds.NETHER_WOOD_DOOR_CLOSE, Sounds.NETHER_DOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.BAMBOO_WOOD_DOOR_OPEN, Sounds.BAMBOO_WOOD_DOOR_CLOSE, Sounds.BAMBOO_DOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.CHERRY_WOOD_DOOR_OPEN, Sounds.CHERRY_WOOD_DOOR_CLOSE, Sounds.CHERRY_DOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.COPPER_DOOR_OPEN, Sounds.COPPER_DOOR_CLOSE, Sounds.COPPER_DOORS, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.WOODEN_FENCE_GATE_OPEN, Sounds.WOODEN_FENCE_GATE_CLOSE, Sounds.WOODEN_FENCE_GATES, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.NETHER_WOOD_FENCE_GATE_OPEN, Sounds.NETHER_WOOD_FENCE_GATE_CLOSE, Sounds.NETHER_FENCE_GATES, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.BAMBOO_WOOD_FENCE_GATE_OPEN, Sounds.BAMBOO_WOOD_FENCE_GATE_CLOSE, Sounds.BAMBOO_FENCE_GATES, soundMapper, soundCallback, affectedBlockCallback);
            collectDoorSounds(predicate, Sounds.CHERRY_WOOD_FENCE_GATE_OPEN, Sounds.CHERRY_WOOD_FENCE_GATE_CLOSE, Sounds.CHERRY_FENCE_GATES, soundMapper, soundCallback, affectedBlockCallback);
            this.affectedOpenableBlockSounds = ImmutableMap.copyOf(affectedDoors);
            this.soundMapper = soundMapperBuilder.buildKeepingLast();
        } catch (Throwable e) {
            plugin.logger().warn("Failed to inject blocks.", e);
        }
    }

    private void collectDoorSounds(Predicate<Key> isUsedForCustomBlock,
                                   Key openSound,
                                   Key closeSound,
                                   List<Key> doors,
                                   Function<Key, SoundData> soundMapper,
                                   Consumer<Key> soundCallback,
                                   BiConsumer<Object, Pair<SoundData, SoundData>> affectedBlockCallback) {
        for (Key d : doors) {
            if (isUsedForCustomBlock.test(d)) {
                soundCallback.accept(openSound);
                soundCallback.accept(closeSound);
                for (Key door : doors) {
                    Object block = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK, KeyUtils.toResourceLocation(door));
                    if (block != null) {
                        affectedBlockCallback.accept(block, Pair.of(soundMapper.apply(Key.of("replaced." + openSound.value())), soundMapper.apply(Key.of("replaced." + closeSound.value()))));
                    }
                }
                break;
            }
        }
    }

    public Object cachedUpdateTagsPacket() {
        return cachedUpdateTagsPacket;
    }

    private void loadMappingsAndAdditionalBlocks() {
        this.plugin.logger().info("Loading mappings.yml.");
        Path mappingsFile = this.plugin.dataFolderPath().resolve("mappings.yml");
        if (!Files.exists(mappingsFile)) {
            this.plugin.saveResource("mappings.yml");
        }
        Path additionalFile = this.plugin.dataFolderPath().resolve("additional-real-blocks.yml");
        if (!Files.exists(additionalFile)) {
            this.plugin.saveResource("additional-real-blocks.yml");
        }
        Yaml yaml = new Yaml(new StringKeyConstructor(mappingsFile, new LoaderOptions()));
        Map<Key, Integer> blockTypeCounter = new LinkedHashMap<>();
        try (InputStream is = Files.newInputStream(mappingsFile)) {
            Map<String, String> blockStateMappings = loadBlockStateMappings(yaml.load(is));
            this.validateBlockStateMappings(mappingsFile, blockStateMappings);
            Map<Integer, String> stateMap = new Int2ObjectOpenHashMap<>();
            Map<Integer, Integer> appearanceMapper = new Int2IntOpenHashMap();
            Map<Key, List<Integer>> appearanceArranger = new HashMap<>();
            for (Map.Entry<String, String> entry : blockStateMappings.entrySet()) {
                this.processBlockStateMapping(mappingsFile, entry, stateMap, blockTypeCounter, appearanceMapper, appearanceArranger);
            }
            this.blockAppearanceMapper = ImmutableMap.copyOf(appearanceMapper);
            this.blockAppearanceArranger = ImmutableMap.copyOf(appearanceArranger);
            this.plugin.logger().info("Freed " + this.blockAppearanceMapper.size() + " block state appearances.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to init mappings.yml", e);
        }
        try (InputStream is = Files.newInputStream(additionalFile)) {
            this.registeredRealBlockSlots = this.buildRegisteredRealBlockSlots(blockTypeCounter, yaml.load(is));
        } catch (IOException e) {
            throw new RuntimeException("Failed to init additional-real-blocks.yml", e);
        }
    }

    private void recordVanillaNoteBlocks() {
        try {
            Object resourceLocation = KeyUtils.toResourceLocation(BlockKeys.NOTE_BLOCK);
            Object block = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK, resourceLocation);
            Object stateDefinition = CoreReflections.field$Block$StateDefinition.get(block);
            @SuppressWarnings("unchecked")
            ImmutableList<Object> states = (ImmutableList<Object>) CoreReflections.field$StateDefinition$states.get(stateDefinition);
            for (Object state : states) {
                BlockStateUtils.CLIENT_SIDE_NOTE_BLOCKS.put(state, new Object());
            }
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to init vanilla note block", e);
        }
    }

    @Nullable
    public Key replaceSoundIfExist(Key id) {
        return this.soundMapper.get(id);
    }

    public boolean isBlockSoundRemoved(Object block) {
        return this.affectedSoundBlocks.contains(block);
    }

    public boolean isOpenableBlockSoundRemoved(Object block) {
        return this.affectedOpenableBlockSounds.containsKey(block);
    }

    public SoundData getRemovedOpenableBlockSound(Object block, boolean open) {
        return open ? this.affectedOpenableBlockSounds.get(block).left() : this.affectedOpenableBlockSounds.get(block).right();
    }

    private Map<String, String> loadBlockStateMappings(Map<String, Object> mappings) {
        Map<String, String> blockStateMappings = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : mappings.entrySet()) {
            if (entry.getValue() instanceof String afterValue) {
                blockStateMappings.put(entry.getKey(), afterValue);
            }
        }
        return blockStateMappings;
    }

    private void validateBlockStateMappings(Path mappingFile, Map<String, String> blockStateMappings) {
        Map<String, String> temp = new HashMap<>(blockStateMappings);
        for (Map.Entry<String, String> entry : temp.entrySet()) {
            String state = entry.getValue();
            if (blockStateMappings.containsKey(state)) {
                String after = blockStateMappings.remove(state);
                plugin.logger().warn(mappingFile, "'" + state + ": " + after + "' is invalid because '" + state + "' has already been used as a base block.");
            }
        }
    }

    private void processBlockStateMapping(Path mappingFile,
                                          Map.Entry<String, String> entry,
                                          Map<Integer, String> stateMap,
                                          Map<Key, Integer> counter,
                                          Map<Integer, Integer> mapper,
                                          Map<Key, List<Integer>> arranger) {
        Object before = createBlockState(mappingFile, entry.getKey());
        Object after = createBlockState(mappingFile, entry.getValue());
        if (before == null || after == null) return;

        int beforeId = BlockStateUtils.blockStateToId(before);
        int afterId = BlockStateUtils.blockStateToId(after);

        Integer previous = mapper.put(beforeId, afterId);
        if (previous == null) {
            Key key = blockOwnerFromString(entry.getKey());
            counter.compute(key, (k, count) -> count == null ? 1 : count + 1);
            stateMap.put(beforeId, entry.getKey());
            stateMap.put(afterId, entry.getValue());
            arranger.computeIfAbsent(key, (k) -> new IntArrayList()).add(beforeId);
        } else {
            String previousState = stateMap.get(previous);
            plugin.logger().warn(mappingFile, "Duplicate entry: '" + previousState + "' equals '" + entry.getKey() + "'");
        }
    }

    private Key blockOwnerFromString(String stateString) {
        int index = stateString.indexOf('[');
        if (index == -1) {
            return Key.of(stateString);
        } else {
            return Key.of(stateString.substring(0, index));
        }
    }

    private Object createBlockState(Path mappingFile, String state) {
        try {
            Object registryOrLookUp = MBuiltInRegistries.BLOCK;
            if (CoreReflections.method$Registry$asLookup != null) {
                registryOrLookUp = CoreReflections.method$Registry$asLookup.invoke(registryOrLookUp);
            }
            Object result = CoreReflections.method$BlockStateParser$parseForBlock.invoke(null, registryOrLookUp, state, false);
            return CoreReflections.method$BlockStateParser$BlockResult$blockState.invoke(result);
        } catch (Exception e) {
            this.plugin.logger().warn(mappingFile, "'" + state + "' is not a valid block state.");
            return null;
        }
    }

    private LinkedHashMap<Key, Integer> buildRegisteredRealBlockSlots(Map<Key, Integer> counter, Map<String, Object> additionalYaml) {
        LinkedHashMap<Key, Integer> map = new LinkedHashMap<>(counter);
        for (Map.Entry<String, Object> entry : additionalYaml.entrySet()) {
            Key blockType = Key.of(entry.getKey());
            if (entry.getValue() instanceof Integer i) {
                int previous = map.getOrDefault(blockType, 0);
                if (previous == 0) {
                    map.put(blockType, i);
                    this.plugin.logger().info("Loaded " + blockType + " with " + i + " real block states");
                } else {
                    map.put(blockType, i + previous);
                    this.plugin.logger().info("Loaded " + blockType + " with " + previous + " appearances and " + (i + previous) + " real block states");
                }
            }
        }
        return map;
    }

    private void unfreezeRegistry() throws IllegalAccessException {
        CoreReflections.field$MappedRegistry$frozen.set(MBuiltInRegistries.BLOCK, false);
        CoreReflections.field$MappedRegistry$unregisteredIntrusiveHolders.set(MBuiltInRegistries.BLOCK, new IdentityHashMap<>());
    }

    private void freezeRegistry() throws IllegalAccessException {
        CoreReflections.field$MappedRegistry$frozen.set(MBuiltInRegistries.BLOCK, true);
    }

    private int registerBlockVariants(Map.Entry<Key, Integer> blockWithCount,
                                      int counter,
                                      ImmutableMap.Builder<Key, Integer> builder1,
                                      ImmutableMap.Builder<Integer, Object> builder2,
                                      ImmutableMap.Builder<Key, List<Integer>> builder3,
                                      ImmutableMap.Builder<Key, DelegatingBlock> builder4,
                                      Set<Object> affectSoundTypes,
                                      List<Key> order) throws Exception {
        Key clientSideBlockType = blockWithCount.getKey();
        boolean isNoteBlock = clientSideBlockType.equals(BlockKeys.NOTE_BLOCK);
        Object clientSideBlock = getBlockFromRegistry(createResourceLocation(clientSideBlockType));
        int amount = blockWithCount.getValue();

        List<Integer> stateIds = new IntArrayList();

        for (int i = 0; i < amount; i++) {
            Key realBlockKey = createRealBlockKey(clientSideBlockType, i);
            Object blockProperties = createBlockProperties(realBlockKey);

            Object newRealBlock;
            Object newBlockState;
            Object blockHolder;
            Object resourceLocation = createResourceLocation(realBlockKey);

            try {
                newRealBlock = BlockGenerator.generateBlock(clientSideBlockType, clientSideBlock, blockProperties);
            } catch (Throwable throwable) {
                this.plugin.logger().warn("Failed to generate dynamic block class", throwable);
                continue;
            }

            blockHolder = CoreReflections.method$Registry$registerForHolder.invoke(null, MBuiltInRegistries.BLOCK, resourceLocation, newRealBlock);
            CoreReflections.method$Holder$Reference$bindValue.invoke(blockHolder, newRealBlock);
            CoreReflections.field$Holder$Reference$tags.set(blockHolder, Set.of());

            newBlockState = FastNMS.INSTANCE.method$Block$defaultState(newRealBlock);
            CoreReflections.method$IdMapper$add.invoke(CoreReflections.instance$Block$BLOCK_STATE_REGISTRY, newBlockState);

            if (isNoteBlock) {
                BlockStateUtils.CLIENT_SIDE_NOTE_BLOCKS.put(newBlockState, new Object());
            }

            int stateId = BlockStateUtils.vanillaStateSize() + counter;

            builder1.put(realBlockKey, stateId);
            builder2.put(stateId, blockHolder);
            builder4.put(realBlockKey, (DelegatingBlock) newRealBlock);
            stateIds.add(stateId);

            this.blocksToDeceive.add(Tuple.of(newRealBlock, clientSideBlockType, isNoteBlock));
            order.add(realBlockKey);
            counter++;
        }

        builder3.put(clientSideBlockType, stateIds);
        Object soundType = CoreReflections.field$BlockBehaviour$soundType.get(clientSideBlock);
        affectSoundTypes.add(soundType);
        return counter;
    }

    private Object createResourceLocation(Key key) {
        return FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath(key.namespace(), key.value());
    }

    private Object getBlockFromRegistry(Object resourceLocation) {
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK, resourceLocation);
    }

    private Key createRealBlockKey(Key replacedBlock, int index) {
        return Key.of(Key.DEFAULT_NAMESPACE, replacedBlock.value() + "_" + index);
    }

    private Object createBlockProperties(Key realBlockKey) throws Exception {
        Object blockProperties = CoreReflections.method$BlockBehaviour$Properties$of.invoke(null);
        Object realBlockResourceLocation = createResourceLocation(realBlockKey);
        Object realBlockResourceKey = CoreReflections.method$ResourceKey$create.invoke(null, MRegistries.BLOCK, realBlockResourceLocation);
        if (CoreReflections.field$BlockBehaviour$Properties$id != null) {
            CoreReflections.field$BlockBehaviour$Properties$id.set(blockProperties, realBlockResourceKey);
        }
        return blockProperties;
    }

    @SuppressWarnings("unchecked")
    private void deceiveBukkit() {
        try {
            Map<Object, Material> magicMap = (Map<Object, Material>) CraftBukkitReflections.field$CraftMagicNumbers$BLOCK_MATERIAL.get(null);
            Map<Material, Object> factories = (Map<Material, Object>) CraftBukkitReflections.field$CraftBlockStates$FACTORIES.get(null);
            for (Tuple<Object, Key, Boolean> tuple : this.blocksToDeceive) {
                deceiveBukkit(tuple.left(), tuple.mid(), tuple.right(), magicMap, factories);
            }
            this.blocksToDeceive.clear();
        } catch (ReflectiveOperationException e) {
            this.plugin.logger().warn("Failed to deceive bukkit", e);
        }
    }

    private void deceiveBukkit(Object newBlock, Key replacedBlock, boolean isNoteBlock, Map<Object, Material> magicMap, Map<Material, Object> factories) {
        if (isNoteBlock) {
            magicMap.put(newBlock, Material.STONE);
        } else {
            Material material = org.bukkit.Registry.MATERIAL.get(new NamespacedKey(replacedBlock.namespace(), replacedBlock.value()));
            if (CraftBukkitReflections.clazz$CraftBlockStates$BlockEntityStateFactory.isInstance(factories.get(material))) {
                magicMap.put(newBlock, Material.STONE);
            } else {
                magicMap.put(newBlock, material);
            }
        }
    }

    @Override
    protected int getBlockRegistryId(Key id) {
        Object block = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK, KeyUtils.toResourceLocation(id));
        return FastNMS.INSTANCE.method$IdMap$getId(MBuiltInRegistries.BLOCK, block).orElseThrow(() -> new IllegalStateException("Block " + id + " not found"));
    }

    @Override
    protected boolean isVanillaBlock(Key id) {
        if (!id.namespace().equals("minecraft")) {
            return false;
        }
        if (id.value().equals("air")) {
            return true;
        }
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK, KeyUtils.toResourceLocation(id)) != MBlocks.AIR;
    }
}
