package net.momirealms.craftengine.bukkit.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.BukkitInjector;
import net.momirealms.craftengine.bukkit.plugin.network.PacketConsumers;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.properties.Properties;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.generator.ModelGeneration;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

public class BukkitBlockManager extends AbstractBlockManager {
    private static BukkitBlockManager instance;
    private final BukkitCraftEngine plugin;

    // Used to store override information of json files
    private final Map<Key, Map<String, JsonElement>> blockStateOverrides = new HashMap<>();

    // A temporary map used to detect whether the same block state corresponds to multiple models.
    private final Map<Integer, Key> tempRegistryIdConflictMap = new HashMap<>();
    // A temporary map that converts the custom block registered on the server to the vanilla block ID.
    private final Map<Integer, Integer> tempBlockAppearanceConvertor = new HashMap<>();

    // The total amount of blocks registered
    private int customBlockCount;

    // CraftEngine objects
    private final Map<Key, CustomBlock> id2CraftEngineBlocks = new HashMap<>();
    private final ImmutableBlockState[] stateId2ImmutableBlockStates;

    // Minecraft objects
    // Cached new blocks $ holders
    private ImmutableMap<Key, Integer> internalId2StateId;
    private ImmutableMap<Integer, Object> stateId2BlockHolder;
    // This map is used to change the block states that are not necessarily needed into a certain block state
    private ImmutableMap<Integer, Integer> blockAppearanceMapper;
    // Used to automatically arrange block states for strings such as minecraft:note_block:0
    private ImmutableMap<Key, List<Integer>> blockAppearanceArranger;
    private ImmutableMap<Key, List<Integer>> realBlockArranger;
    // Record the amount of real blocks by block type
    private LinkedHashMap<Key, Integer> registeredRealBlockSlots;
    // A set of blocks that sounds have been removed
    private ImmutableSet<Object> affectedSoundBlocks;
    private ImmutableMap<Key, Key> soundMapper;
    // A list to record the order of registration
    private List<Key> blockRegisterOrder = new ArrayList<>();

    // a reverted mapper
    private final Map<Integer, List<Integer>> appearanceToRealState = new HashMap<>();
    // Cached command suggestions
    private final List<Suggestion> cachedSuggestions = new ArrayList<>();
    // Event listeners
    private final BlockEventListener blockEventListener;
    private final FallingBlockRemoveListener fallingBlockRemoveListener;

    public BukkitBlockManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
        this.initVanillaRegistry();
        this.loadMappingsAndAdditionalBlocks();
        if (plugin.hasMod() && plugin.requiresRestart()) {
            blockEventListener = null;
            fallingBlockRemoveListener = null;
            stateId2ImmutableBlockStates = null;
            return;
        }
        this.registerBlocks();
        this.registerEmptyBlock();
        this.initMirrorRegistry();
        boolean enableNoteBlocks = this.blockAppearanceArranger.containsKey(BlockKeys.NOTE_BLOCK);
        this.blockEventListener = new BlockEventListener(plugin, this, enableNoteBlocks);
        if (enableNoteBlocks) {
            this.recordVanillaNoteBlocks();
        }
        if (VersionHelper.isVersionNewerThan1_20_3()) {
            this.fallingBlockRemoveListener = new FallingBlockRemoveListener();
        } else this.fallingBlockRemoveListener = null;
        this.stateId2ImmutableBlockStates = new ImmutableBlockState[customBlockCount];
        Arrays.fill(this.stateId2ImmutableBlockStates, EmptyBlock.INSTANCE.getDefaultState());
        instance = this;
    }

    public static BukkitBlockManager instance() {
        return instance;
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this.blockEventListener, plugin.bootstrap());
        if (this.fallingBlockRemoveListener != null) {
            Bukkit.getPluginManager().registerEvents(this.fallingBlockRemoveListener, plugin.bootstrap());
        }
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this.blockEventListener);
        if (this.fallingBlockRemoveListener != null) HandlerList.unregisterAll(this.fallingBlockRemoveListener);
        super.clearModelsToGenerate();
        this.clearCache();
        this.appearanceToRealState.clear();
        this.id2CraftEngineBlocks.clear();
        this.cachedSuggestions.clear();
        this.blockStateOverrides.clear();
        if (EmptyBlock.INSTANCE != null)
            Arrays.fill(this.stateId2ImmutableBlockStates, EmptyBlock.INSTANCE.getDefaultState());
    }

    @Override
    public void disable() {
        this.unload();
    }

    @Override
    public Map<Key, Key> soundMapper() {
        return this.soundMapper;
    }

    @Override
    public void delayedLoad() {
        initSuggestions();
        resetPacketConsumers();
        clearCache();
    }

    private void clearCache() {
        this.tempRegistryIdConflictMap.clear();
        this.tempBlockAppearanceConvertor.clear();
    }

    public void initFastAsyncWorldEditHook() {
        // do nothing
    }

    public void initWorldEditHook() {
        for (Key newBlockId : this.blockRegisterOrder) {
            WorldEditHook.register(newBlockId);
        }
    }

    @Nullable
    public Object getMinecraftBlockHolder(int stateId) {
        return stateId2BlockHolder.get(stateId);
    }

    @NotNull
    public ImmutableBlockState getImmutableBlockStateUnsafe(int stateId) {
        return stateId2ImmutableBlockStates[stateId - BlockStateUtils.vanillaStateSize()];
    }

    @Nullable
    public ImmutableBlockState getImmutableBlockState(int stateId) {
        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            return stateId2ImmutableBlockStates[stateId - BlockStateUtils.vanillaStateSize()];
        }
        return null;
    }

    @Override
    public Map<Key, Map<String, JsonElement>> blockOverrides() {
        return blockStateOverrides;
    }

    @Override
    public Map<Key, CustomBlock> blocks() {
        return this.id2CraftEngineBlocks;
    }

    @Override
    public Optional<CustomBlock> getBlock(Key key) {
        return Optional.ofNullable(this.id2CraftEngineBlocks.get(key));
    }

    @Override
    public Collection<Suggestion> cachedSuggestions() {
        return this.cachedSuggestions;
    }

    @Override
    public void initSuggestions() {
        this.cachedSuggestions.clear();
        Set<String> states = new HashSet<>();
        for (CustomBlock block : this.id2CraftEngineBlocks.values()) {
            states.add(block.id().toString());
            for (ImmutableBlockState state : block.variantProvider().states()) {
                states.add(state.toString());
            }
        }
        for (String state : states) {
            this.cachedSuggestions.add(Suggestion.suggestion(state));
        }
    }

    public ImmutableMap<Key, List<Integer>> blockAppearanceArranger() {
        return blockAppearanceArranger;
    }

    public ImmutableMap<Key, List<Integer>> realBlockArranger() {
        return realBlockArranger;
    }

    @Nullable
    public List<Integer> appearanceToRealStates(int appearanceStateId) {
        return this.appearanceToRealState.get(appearanceStateId);
    }

    private void initMirrorRegistry() {
        int size = RegistryUtils.currentBlockRegistrySize();
        PackedBlockState[] states = new PackedBlockState[size];
        for (int i = 0; i < size; i++) {
            states[i] = new PackedBlockState(BlockStateUtils.idToBlockState(i), i);
        }
        BlockRegistryMirror.init(states);
    }

    private void registerEmptyBlock() {
        Holder.Reference<CustomBlock> holder = ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).registerForHolder(new ResourceKey<>(BuiltInRegistries.BLOCK.key().location(), Key.withDefaultNamespace("empty")));
        EmptyBlock emptyBlock = new EmptyBlock(Key.withDefaultNamespace("empty"), holder);
        holder.bindValue(emptyBlock);
    }

    private void resetPacketConsumers() {
        Map<Integer, Integer> finalMapping = new HashMap<>(this.blockAppearanceMapper);
        int stoneId = BlockStateUtils.blockStateToId(Reflections.instance$Blocks$STONE$defaultState);
        for (int custom : this.internalId2StateId.values()) {
            finalMapping.put(custom, stoneId);
        }
        finalMapping.putAll(this.tempBlockAppearanceConvertor);
        PacketConsumers.init(finalMapping, RegistryUtils.currentBlockRegistrySize());
    }

    private void initVanillaRegistry() {
        int vanillaStateCount;
        if (plugin.hasMod()) {
            try {
                Class<?> modClass = ReflectionUtils.getClazz(CraftEngine.MOD_CLASS);
                Field amountField = ReflectionUtils.getDeclaredField(modClass, "vanillaRegistrySize");
                vanillaStateCount = (int) amountField.get(null);
            } catch (Exception e) {
                vanillaStateCount = RegistryUtils.currentBlockRegistrySize();
                plugin.logger().severe("Fatal error", e);
            }
        } else {
            vanillaStateCount = RegistryUtils.currentBlockRegistrySize();
        }
        plugin.logger().info("Vanilla block count: " + vanillaStateCount);
        BlockStateUtils.init(vanillaStateCount);
    }

    @SuppressWarnings("unchecked")
    private void registerBlocks() {
        plugin.logger().info("Registering blocks. Please wait...");
        try {
            ImmutableMap.Builder<Key, Integer> builder1 = ImmutableMap.builder();
            ImmutableMap.Builder<Integer, Object> builder2 = ImmutableMap.builder();
            ImmutableMap.Builder<Key, List<Integer>> builder3 = ImmutableMap.builder();
            Set<Object> affectedSounds = new HashSet<>();
            Set<Object> affectedBlocks = new HashSet<>();
            List<Key> order = new ArrayList<>();

            unfreezeRegistry();

            int counter = 0;
            for (Map.Entry<Key, Integer> baseBlockAndItsCount : this.registeredRealBlockSlots.entrySet()) {
                counter = registerBlockVariants(baseBlockAndItsCount, counter, builder1, builder2, builder3, affectedSounds, order);
            }

            freezeRegistry();
            this.plugin.logger().info("Registered block count: " + counter);
            this.customBlockCount = counter;
            this.internalId2StateId = builder1.build();
            this.stateId2BlockHolder = builder2.build();
            this.realBlockArranger = builder3.build();
            this.blockRegisterOrder = ImmutableList.copyOf(order);

            for (Object block : (Iterable<Object>) Reflections.instance$BuiltInRegistries$BLOCK) {
                Object soundType = Reflections.field$BlockBehaviour$soundType.get(block);
                if (affectedSounds.contains(soundType)) {
                    affectedBlocks.add(block);
                }
            }

            this.affectedSoundBlocks = ImmutableSet.copyOf(affectedBlocks);

            ImmutableMap.Builder<Key, Key> soundMapperBuilder = ImmutableMap.builder();
            for (Object soundType : affectedSounds) {
                for (Field field : List.of(Reflections.field$SoundType$placeSound, Reflections.field$SoundType$fallSound, Reflections.field$SoundType$hitSound, Reflections.field$SoundType$stepSound, Reflections.field$SoundType$breakSound)) {
                    Object soundEvent = field.get(soundType);
                    Key previousId = Key.of(Reflections.field$SoundEvent$location.get(soundEvent).toString());
                    soundMapperBuilder.put(previousId, Key.of(previousId.namespace(), "replaced." + previousId.value()));
                }
            }

            this.soundMapper = soundMapperBuilder.build();
        } catch (Throwable e) {
            plugin.logger().warn("Failed to inject blocks.", e);
        }
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        // read block settings
        BlockSettings settings = BlockSettings.fromMap(MiscUtils.castToMap(section.getOrDefault("settings", Map.of()), false));
        // read loot table
        LootTable<ItemStack> lootTable = LootTable.fromMap(MiscUtils.castToMap(section.getOrDefault("loot", Map.of()), false));
        // read states
        Map<String, Property<?>> properties;
        Map<String, Integer> appearances;
        Map<String, VariantState> variants;
        Map<String, Object> stateSection = MiscUtils.castToMap(section.get("state"), true);
        if (stateSection != null) {
            properties = Map.of();
            int internalId = MiscUtils.getAsInt(stateSection.getOrDefault("id", -1));
            if (PreConditions.runIfTrue(internalId < 0, () -> plugin.logger().warn(path, "No state id configured for block " + id))) return;
            Pair<Key, Integer> pair = parseAppearanceSection(path, stateSection, id);
            if (pair == null) return;
            appearances = Map.of("default", pair.right());
            Key internalBlockId = Key.of(CraftEngine.NAMESPACE, pair.left().value() + "_" + internalId);
            int internalBlockRegistryId = MiscUtils.getAsInt(this.internalId2StateId.getOrDefault(internalBlockId, -1));
            if (internalBlockRegistryId == -1) {
                plugin.logger().warn(path, "Failed to register " + id + " because id " + internalId + " is not a value between 0~" + (MiscUtils.getAsInt(this.registeredRealBlockSlots.get(pair.left()))-1) +
                        ". Consider editing additional-real-blocks.yml if the number of real block IDs is insufficient while there are still available appearances");
                return;
            }
            variants = Map.of("", new VariantState("default", settings, internalBlockRegistryId));
        } else {
            Map<String, Object> statesSection = MiscUtils.castToMap(section.get("states"), true);
            if (statesSection == null) {
                plugin.logger().warn(path, "No states configured for block " + id);
                return;
            }
            Map<String, Object> propertySection = MiscUtils.castToMap(statesSection.get("properties"), true);
            if (PreConditions.isNull(propertySection, () -> plugin.logger().warn(path, "No properties configured for block " + id))) return;
            properties = parseProperties(path, propertySection);
            Map<String, Object> appearancesSection = MiscUtils.castToMap(statesSection.get("appearances"), true);
            if (PreConditions.isNull(appearancesSection, () -> plugin.logger().warn(path, "No appearances configured for block " + id))) return;
            appearances = new HashMap<>();
            Map<String, Key> tempTypeMap = new HashMap<>();
            for (Map.Entry<String, Object> appearanceEntry : appearancesSection.entrySet()) {
                if (appearanceEntry.getValue() instanceof Map<?, ?> appearanceSection) {
                    Pair<Key, Integer> pair = parseAppearanceSection(path, MiscUtils.castToMap(appearanceSection, false), id);
                    if (pair == null) return;
                    appearances.put(appearanceEntry.getKey(), pair.right());
                    tempTypeMap.put(appearanceEntry.getKey(), pair.left());
                }
            }
            Map<String, Object> variantsSection = MiscUtils.castToMap(statesSection.get("variants"), true);
            if (PreConditions.isNull(variantsSection, () -> plugin.logger().warn(path, "No variants configured for block " + id))) return;
            variants = new HashMap<>();
            for (Map.Entry<String, Object> variantEntry : variantsSection.entrySet()) {
                if (variantEntry.getValue() instanceof Map<?, ?> variantSection0) {
                    Map<String, Object> variantSection = MiscUtils.castToMap(variantSection0, false);
                    String variantName = variantEntry.getKey();
                    String appearance = (String) variantSection.get("appearance");
                    if (appearance == null) {
                        plugin.logger().warn(path, "No appearance configured for variant " + variantName);
                        return;
                    }
                    if (!appearances.containsKey(appearance)) {
                        plugin.logger().warn(path, appearance + " is not a valid appearance for block " + id);
                        return;
                    }
                    int internalId = MiscUtils.getAsInt(variantSection.getOrDefault("id", -1));
                    Key baseBlock = tempTypeMap.get(appearance);
                    Key internalBlockId = Key.of(CraftEngine.NAMESPACE, baseBlock.value() + "_" + internalId);
                    int internalBlockRegistryId = MiscUtils.getAsInt(this.internalId2StateId.getOrDefault(internalBlockId, -1));
                    if (internalBlockRegistryId == -1) {
                        plugin.logger().warn(path, "Failed to register " + id + " because id " + internalId + " is not a value between 0~" + (MiscUtils.getAsInt(this.registeredRealBlockSlots.getOrDefault(baseBlock, 1))-1) +
                                ". Consider editing additional-real-blocks.yml if the number of real block IDs is insufficient while there are still available appearances");
                        return;
                    }
                    Map<String, Object> anotherSetting = MiscUtils.castToMap(variantSection.get("settings"), true);
                    variants.put(variantName, new VariantState(appearance, anotherSetting == null ? settings : BlockSettings.ofFullCopy(settings, anotherSetting), internalBlockRegistryId));
                }
            }
        }
        // create or get block holder
        Holder.Reference<CustomBlock> holder = BuiltInRegistries.BLOCK.get(id).orElseGet(() ->
                ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).registerForHolder(new ResourceKey<>(BuiltInRegistries.BLOCK.key().location(), id)));
        // create block
        Map<String, Object> behaviorSection = MiscUtils.castToMap(section.getOrDefault("behavior", Map.of()), false);

        CustomBlock block = new BukkitCustomBlock(id, holder, properties, appearances, variants, settings, behaviorSection, lootTable);

        // bind appearance
        bindAppearance(block);
        this.id2CraftEngineBlocks.put(id, block);
    }

    private void bindAppearance(CustomBlock block) {
        for (ImmutableBlockState state : block.variantProvider().states()) {
            ImmutableBlockState previous = this.stateId2ImmutableBlockStates[state.customBlockState().registryId() - BlockStateUtils.vanillaStateSize()];
            if (previous != null && !previous.isEmpty()) {
                this.plugin.logger().severe("[Fatal] Failed to bind real block state for " + state + ": the state is already occupied by " + previous);
                continue;
            }
            this.stateId2ImmutableBlockStates[state.customBlockState().registryId() - BlockStateUtils.vanillaStateSize()] = state;
            this.tempBlockAppearanceConvertor.put(state.customBlockState().registryId(), state.vanillaBlockState().registryId());
            this.appearanceToRealState.computeIfAbsent(state.vanillaBlockState().registryId(), k -> new ArrayList<>()).add(state.customBlockState().registryId());
        }
    }

    private Map<String, Property<?>> parseProperties(Path path, Map<String, Object> propertiesSection) {
        Map<String, Property<?>> properties = new HashMap<>();
        for (Map.Entry<String, Object> entry : propertiesSection.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> params) {
                Property<?> property = Properties.fromMap(entry.getKey(), MiscUtils.castToMap(params, false));
                properties.put(entry.getKey(), property);
            } else {
                this.plugin.logger().warn(path, "Invalid property format: " + entry.getKey());
            }
        }
        return properties;
    }

    @Nullable
    private Pair<Key, Integer> parseAppearanceSection(Path path, Map<String, Object> section, Key id) {
        String vanillaStateString = (String) section.get("state");
        if (PreConditions.isNull(vanillaStateString,
                () -> this.plugin.logger().warn(path, "No block state found for: " + id))) return null;
        int vanillaStateRegistryId;
        try {
            vanillaStateRegistryId = parseVanillaStateRegistryId(vanillaStateString);
        } catch (BlockStateArrangeException e) {
            this.plugin.logger().warn(path, "Failed to load " + id + " - " + e.getMessage(), e);
            return null;
        }
        Key ifAny = this.tempRegistryIdConflictMap.get(vanillaStateRegistryId);
        if (ifAny != null && !ifAny.equals(id)) {
            this.plugin.logger().warn(path, "Can't use " + BlockStateUtils.idToBlockState(vanillaStateRegistryId) + " as the base block for " + id + " because the state has already been used by " + ifAny);
            return null;
        }
        Object models = section.getOrDefault("models", section.get("model"));
        List<JsonObject> variants = new ArrayList<>();
        if (models instanceof Map<?, ?> singleModelSection) {
            loadVariantModel(variants, MiscUtils.castToMap(singleModelSection, false));
        } else if (models instanceof List<?> modelList) {
            for (Object model : modelList) {
                if (model instanceof Map<?,?> singleModelMap) {
                    loadVariantModel(variants, MiscUtils.castToMap(singleModelMap, false));
                }
            }
        } else {
            this.plugin.logger().warn(path, "No model set for " + id);
            return null;
        }
        if (variants.isEmpty()) return null;
        this.tempRegistryIdConflictMap.put(vanillaStateRegistryId, id);
        String blockState = BlockStateUtils.idToBlockState(vanillaStateRegistryId).toString();
        Key block = Key.of(blockState.substring(blockState.indexOf('{') + 1, blockState.lastIndexOf('}')));
        String propertyData = blockState.substring(blockState.indexOf('[') + 1, blockState.lastIndexOf(']'));
        Map<String, JsonElement> paths = this.blockStateOverrides.computeIfAbsent(block, k -> new HashMap<>());
        if (variants.size() == 1) {
            paths.put(propertyData, variants.get(0));
        } else {
            JsonArray array = new JsonArray();
            for (JsonObject object : variants) {
                array.add(object);
            }
            paths.put(propertyData, array);
        }
        return Pair.of(block, vanillaStateRegistryId);
    }

    private void loadVariantModel(List<JsonObject> variants, Map<String, Object> singleModelMap) {
        JsonObject json = new JsonObject();
        String modelPath = (String) singleModelMap.get("path");
        json.addProperty("model", modelPath);
        if (singleModelMap.containsKey("x")) json.addProperty("x", MiscUtils.getAsInt(singleModelMap.get("x")));
        if (singleModelMap.containsKey("y")) json.addProperty("y", MiscUtils.getAsInt(singleModelMap.get("y")));
        if (singleModelMap.containsKey("uvlock")) json.addProperty("uvlock", (boolean) singleModelMap.get("uvlock"));
        if (singleModelMap.containsKey("weight")) json.addProperty("weight", MiscUtils.getAsInt(singleModelMap.get("weight")));
        Map<String, Object> generationMap = MiscUtils.castToMap(singleModelMap.get("generation"), true);
        if (generationMap != null) {
            prepareModelGeneration(new ModelGeneration(Key.of(modelPath), generationMap));
        }
        variants.add(json);
    }

    private int parseVanillaStateRegistryId(String blockState) throws BlockStateArrangeException {
        String[] split = blockState.split(":", 3);
        PreConditions.runIfTrue(split.length >= 4, () -> {
            throw new BlockStateArrangeException("Invalid vanilla block state: " + blockState);
        });
        int registryId;
        // minecraft:xxx:0
        // xxx:0
        String stateOrId = split[split.length - 1];
        boolean isId = !stateOrId.contains("[") && !stateOrId.contains("]");
        if (isId) {
            if (split.length == 1) {
                throw new BlockStateArrangeException("Invalid vanilla block state: " + blockState);
            }
            Key block = split.length == 2 ? Key.of(split[0]) : Key.of(split[0], split[1]);
            try {
                int id = split.length == 2 ? Integer.parseInt(split[1]) : Integer.parseInt(split[2]);
                PreConditions.runIfTrue(id < 0, () -> {
                    throw new BlockStateArrangeException("Invalid block state: " + blockState);
                });
                List<Integer> arranger = this.blockAppearanceArranger.get(block);
                if (arranger == null) {
                    throw new BlockStateArrangeException("No freed block state is available for block " + block);
                }
                if (id >= arranger.size()) {
                    throw new BlockStateArrangeException(blockState + " is not a valid block state because " + id + " is outside of the range (0~" + (arranger.size() - 1) + ")");
                }
                registryId = arranger.get(id);
            } catch (NumberFormatException e) {
                throw new BlockStateArrangeException("Invalid block state: " + blockState);
            }
        } else {
            try {
                BlockData blockData = Bukkit.createBlockData(blockState);
                registryId = BlockStateUtils.blockDataToId(blockData);
            } catch (IllegalArgumentException e) {
                throw new BlockStateArrangeException("Invalid block state: " + blockState);
            }
        }
        return registryId;
    }

    private void loadMappingsAndAdditionalBlocks() {
        this.plugin.logger().info("Loading mappings.yml.");
        File mappingFile = new File(plugin.dataFolderFile(), "mappings.yml");
        YamlDocument mappings = ConfigManager.instance().loadOrCreateYamlData("mappings.yml");
        Map<String, String> blockStateMappings = loadBlockStateMappings(mappings);
        this.validateBlockStateMappings(mappingFile, blockStateMappings);
        Map<Integer, String> stateMap = new HashMap<>();
        Map<Key, Integer> blockTypeCounter = new LinkedHashMap<>();
        Map<Integer, Integer> appearanceMapper = new HashMap<>();
        Map<Key, List<Integer>> appearanceArranger = new HashMap<>();
        for (Map.Entry<String, String> entry : blockStateMappings.entrySet()) {
            this.processBlockStateMapping(mappingFile, entry, stateMap, blockTypeCounter, appearanceMapper, appearanceArranger);
        }
        this.blockAppearanceMapper = ImmutableMap.copyOf(appearanceMapper);
        this.blockAppearanceArranger = ImmutableMap.copyOf(appearanceArranger);
        this.plugin.logger().info("Freed " + this.blockAppearanceMapper.size() + " block state appearances.");
        YamlDocument additionalYaml = ConfigManager.instance().loadOrCreateYamlData("additional-real-blocks.yml");
        this.registeredRealBlockSlots = this.buildRegisteredRealBlockSlots(blockTypeCounter, additionalYaml);
    }

    private void recordVanillaNoteBlocks() {
        try {
            Object resourceLocation = Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, BlockKeys.NOTE_BLOCK.namespace(), BlockKeys.NOTE_BLOCK.value());
            Object block = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$BLOCK, resourceLocation);
            Object stateDefinition = Reflections.field$Block$StateDefinition.get(block);
            @SuppressWarnings("unchecked")
            ImmutableList<Object> states = (ImmutableList<Object>) Reflections.field$StateDefinition$states.get(stateDefinition);
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

    private Map<String, String> loadBlockStateMappings(YamlDocument mappings) {
        Map<String, String> blockStateMappings = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : mappings.getStringRouteMappedValues(false).entrySet()) {
            if (entry.getValue() instanceof String afterValue) {
                blockStateMappings.put(entry.getKey(), afterValue);
            }
        }
        return blockStateMappings;
    }

    private void validateBlockStateMappings(File mappingFile, Map<String, String> blockStateMappings) {
        Map<String, String> temp = new HashMap<>(blockStateMappings);
        for (Map.Entry<String, String> entry : temp.entrySet()) {
            String state = entry.getValue();
            if (blockStateMappings.containsKey(state)) {
                String after = blockStateMappings.remove(state);
                plugin.logger().warn(mappingFile, "'" + state + ": " + after + "' is invalid because '" + state + "' has already been used as a base block.");
            }
        }
    }

    private void processBlockStateMapping(File mappingFile,
                                          Map.Entry<String, String> entry,
                                          Map<Integer, String> stateMap,
                                          Map<Key, Integer> counter,
                                          Map<Integer, Integer> mapper,
                                          Map<Key, List<Integer>> arranger) {
        BlockData before = createBlockData(mappingFile, entry.getKey());
        BlockData after = createBlockData(mappingFile, entry.getValue());
        if (before == null || after == null) return;

        int beforeId = BlockStateUtils.blockDataToId(before);
        int afterId = BlockStateUtils.blockDataToId(after);

        Integer previous = mapper.put(beforeId, afterId);
        if (previous == null) {
            Key key = KeyUtils.namespacedKey2Key(before.getMaterial().getKey());
            counter.compute(key, (k, count) -> count == null ? 1 : count + 1);
            stateMap.put(beforeId, entry.getKey());
            stateMap.put(afterId, entry.getValue());
            arranger.computeIfAbsent(key, (k) -> new ArrayList<>()).add(beforeId);
        } else {
            String previousState = stateMap.get(previous);
            plugin.logger().warn(mappingFile, "Duplicate entry: '" + previousState + "' equals '" + entry.getKey() + "'");
        }
    }

    private BlockData createBlockData(File mappingFile, String blockState) {
        try {
            return Bukkit.createBlockData(blockState);
        } catch (IllegalArgumentException e) {
            plugin.logger().warn(mappingFile, "'" + blockState + "' is not a valid block state.");
            return null;
        }
    }

    private LinkedHashMap<Key, Integer> buildRegisteredRealBlockSlots(Map<Key, Integer> counter, YamlDocument additionalYaml) {
        LinkedHashMap<Key, Integer> map = new LinkedHashMap<>();
        for (Map.Entry<Key, Integer> entry : counter.entrySet()) {
            String id = entry.getKey().toString();
            int additionalStates = additionalYaml.getInt(id, 0);
            int internalIds = entry.getValue() + additionalStates;
            plugin.logger().info("Loaded " + id + " with " + entry.getValue() + " appearances and " + internalIds + " real block states");
            map.put(entry.getKey(), internalIds);
        }
        return map;
    }

    private void unfreezeRegistry() throws IllegalAccessException {
        Reflections.field$MappedRegistry$frozen.set(Reflections.instance$BuiltInRegistries$BLOCK, false);
        Reflections.field$MappedRegistry$unregisteredIntrusiveHolders.set(Reflections.instance$BuiltInRegistries$BLOCK, new IdentityHashMap<>());
    }

    private void freezeRegistry() throws IllegalAccessException {
        Reflections.field$MappedRegistry$frozen.set(Reflections.instance$BuiltInRegistries$BLOCK, true);
    }

    private int registerBlockVariants(Map.Entry<Key, Integer> blockWithCount,
                                      int counter,
                                      ImmutableMap.Builder<Key, Integer> builder1,
                                      ImmutableMap.Builder<Integer, Object> builder2,
                                      ImmutableMap.Builder<Key, List<Integer>> builder3,
                                      Set<Object> affectSoundTypes,
                                      List<Key> order) throws Exception {
        Key clientSideBlockType = blockWithCount.getKey();
        boolean isNoteBlock = clientSideBlockType.equals(BlockKeys.NOTE_BLOCK);
        Object clientSideBlock = getBlockFromRegistry(createResourceLocation(clientSideBlockType));
        int amount = blockWithCount.getValue();

        List<Integer> stateIds = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            Key realBlockKey = createRealBlockKey(clientSideBlockType, i);
            Object blockProperties = createBlockProperties(realBlockKey);

            Object newRealBlock;
            Object newBlockState;
            Object blockHolder;
            Object resourceLocation = createResourceLocation(realBlockKey);

            if (plugin.hasMod()) {
                newRealBlock = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$BLOCK, resourceLocation);
                newBlockState = getOnlyBlockState(newRealBlock);
                @SuppressWarnings("unchecked")
                Optional<Object> optionalHolder = (Optional<Object>) Reflections.method$Registry$getHolder0.invoke(Reflections.instance$BuiltInRegistries$BLOCK, resourceLocation);
                blockHolder = optionalHolder.get();
            } else {
                try {
                    newRealBlock = BukkitInjector.generateBlock(clientSideBlockType, clientSideBlock, blockProperties);
                } catch (Throwable throwable) {
                    plugin.logger().warn("Failed to generate dynamic block class", throwable);
                    continue;
                }

                blockHolder = Reflections.method$Registry$registerForHolder.invoke(null, Reflections.instance$BuiltInRegistries$BLOCK, resourceLocation, newRealBlock);
                Reflections.method$Holder$Reference$bindValue.invoke(blockHolder, newRealBlock);
                Reflections.field$Holder$Reference$tags.set(blockHolder, Set.of());

                newBlockState = getOnlyBlockState(newRealBlock);
                Reflections.method$IdMapper$add.invoke(Reflections.instance$BLOCK_STATE_REGISTRY, newBlockState);
            }

            if (isNoteBlock) {
                BlockStateUtils.CLIENT_SIDE_NOTE_BLOCKS.put(newBlockState, new Object());
            }

            int stateId = BlockStateUtils.vanillaStateSize() + counter;

            builder1.put(realBlockKey, stateId);
            builder2.put(stateId, blockHolder);
            stateIds.add(stateId);

            deceiveBukkit(newRealBlock, clientSideBlockType);
            order.add(realBlockKey);
            counter++;
        }

        builder3.put(clientSideBlockType, stateIds);
        Object soundType = Reflections.field$BlockBehaviour$soundType.get(clientSideBlock);
        affectSoundTypes.add(soundType);
        return counter;
    }

    private Object createResourceLocation(Key key) throws Exception {
        return Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, key.namespace(), key.value());
    }

    private Object getBlockFromRegistry(Object resourceLocation) throws Exception {
        return Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$BLOCK, resourceLocation);
    }

    private Key createRealBlockKey(Key replacedBlock, int index) {
        return Key.of(CraftEngine.NAMESPACE, replacedBlock.value() + "_" + index);
    }

    private Object createBlockProperties(Key realBlockKey) throws Exception {
        Object blockProperties = Reflections.method$BlockBehaviour$Properties$of.invoke(null);
        Object realBlockResourceLocation = createResourceLocation(realBlockKey);
        Object realBlockResourceKey = Reflections.method$ResourceKey$create.invoke(null, Reflections.instance$Registries$BLOCK, realBlockResourceLocation);
        if (Reflections.field$BlockBehaviour$Properties$id != null) {
            Reflections.field$BlockBehaviour$Properties$id.set(blockProperties, realBlockResourceKey);
        }
        return blockProperties;
    }

    private Object getOnlyBlockState(Object newBlock) throws IllegalAccessException {
        Object stateDefinition = Reflections.field$Block$StateDefinition.get(newBlock);
        @SuppressWarnings("unchecked")
        ImmutableList<Object> states = (ImmutableList<Object>) Reflections.field$StateDefinition$states.get(stateDefinition);
        return states.get(0);
    }

    private void deceiveBukkit(Object newBlock, Key replacedBlock) throws IllegalAccessException {
        @SuppressWarnings("unchecked")
        Map<Object, Material> magicMap = (Map<Object, Material>) Reflections.field$CraftMagicNumbers$BLOCK_MATERIAL.get(null);
        magicMap.put(newBlock, org.bukkit.Registry.MATERIAL.get(Objects.requireNonNull(NamespacedKey.fromString(replacedBlock.toString()))));
    }
}
