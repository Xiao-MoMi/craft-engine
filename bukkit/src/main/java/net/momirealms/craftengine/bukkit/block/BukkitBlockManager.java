package net.momirealms.craftengine.bukkit.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.BukkitInjector;
import net.momirealms.craftengine.bukkit.plugin.network.PacketConsumers;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.properties.Properties;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.event.EventFunctions;
import net.momirealms.craftengine.core.plugin.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

public class BukkitBlockManager extends AbstractBlockManager {
    private static BukkitBlockManager instance;
    private final BukkitCraftEngine plugin;
    private final BlockParser blockParser;

    // A temporary map used to detect whether the same block state corresponds to multiple models.
    private final Map<Integer, Key> tempRegistryIdConflictMap = new HashMap<>();
    // A temporary map that converts the custom block registered on the server to the vanilla block ID.
    private final Map<Integer, Integer> tempBlockAppearanceConvertor = new HashMap<>();
    // A temporary map that stores the model path of a certain vanilla block state
    private final Map<Integer, JsonElement> tempVanillaBlockStateModels = new HashMap<>();

    // The total amount of blocks registered
    private int customBlockCount;
    protected final ImmutableBlockState[] stateId2ImmutableBlockStates;
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
    // Used to store override information of json files
    private final Map<Key, Map<String, JsonElement>> blockStateOverrides = new HashMap<>();
    // for mod, real block id -> state models
    private final Map<Key, JsonElement> modBlockStates = new HashMap<>();
    // Event listeners
    private final BlockEventListener blockEventListener;
    private final FallingBlockRemoveListener fallingBlockRemoveListener;

    private Map<Integer, List<String>> clientBoundTags = Map.of();
    private Map<Integer, List<String>> previousTags = Map.of();
    protected Object cachedUpdateTagsPacket;

    public BukkitBlockManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
        this.blockParser = new BlockParser();
        this.initVanillaRegistry();
        this.loadMappingsAndAdditionalBlocks();
        if (plugin.hasMod() && plugin.requiresRestart()) {
            blockEventListener = null;
            fallingBlockRemoveListener = null;
            stateId2ImmutableBlockStates = new ImmutableBlockState[]{};
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
        if (VersionHelper.isOrAbove1_20_3()) {
            this.fallingBlockRemoveListener = new FallingBlockRemoveListener();
        } else this.fallingBlockRemoveListener = null;
        this.stateId2ImmutableBlockStates = new ImmutableBlockState[customBlockCount];
        Arrays.fill(this.stateId2ImmutableBlockStates, EmptyBlock.INSTANCE.defaultState());
        instance = this;
        this.resetPacketConsumers();
    }

    public List<Key> blockRegisterOrder() {
        return Collections.unmodifiableList(this.blockRegisterOrder);
    }

    public static BukkitBlockManager instance() {
        return instance;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.blockEventListener, plugin.bootstrap());
        if (this.fallingBlockRemoveListener != null) {
            Bukkit.getPluginManager().registerEvents(this.fallingBlockRemoveListener, plugin.bootstrap());
        }
    }

    @Override
    public void unload() {
        super.unload();
        this.clearCache();
        this.appearanceToRealState.clear();
        this.blockStateOverrides.clear();
        this.modBlockStates.clear();
        if (EmptyBlock.STATE != null)
            Arrays.fill(this.stateId2ImmutableBlockStates, EmptyBlock.STATE);
        this.previousTags = this.clientBoundTags;
        this.clientBoundTags = new HashMap<>();
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
        initSuggestions();
        resetPacketConsumers();
        clearCache();
        resendTags();
    }

    private void resendTags() {
        // if there's no change
        if (this.clientBoundTags.equals(this.previousTags)) return;
        List<TagUtils.TagEntry> list = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : this.clientBoundTags.entrySet()) {
            list.add(new TagUtils.TagEntry(entry.getKey(), entry.getValue()));
        }
        Object packet = TagUtils.createUpdateTagsPacket(Map.of(Reflections.instance$Registries$BLOCK, list));
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.plugin.networkManager().sendPacket(player, packet);
        }
        // 如果空，那么新来的玩家就没必要收到更新包了
        if (list.isEmpty()) {
            this.cachedUpdateTagsPacket = null;
        } else {
            this.cachedUpdateTagsPacket = packet;
        }
    }

    private void clearCache() {
        this.tempRegistryIdConflictMap.clear();
        this.tempBlockAppearanceConvertor.clear();
        this.tempVanillaBlockStateModels.clear();
    }

    @Nullable
    public Object getMinecraftBlockHolder(int stateId) {
        return stateId2BlockHolder.get(stateId);
    }

    @NotNull
    public ImmutableBlockState getImmutableBlockStateUnsafe(int stateId) {
        return this.stateId2ImmutableBlockStates[stateId - BlockStateUtils.vanillaStateSize()];
    }

    @Nullable
    public ImmutableBlockState getImmutableBlockState(int stateId) {
        if (!BlockStateUtils.isVanillaBlock(stateId)) {
            return this.stateId2ImmutableBlockStates[stateId - BlockStateUtils.vanillaStateSize()];
        }
        return null;
    }

    @Override
    public Map<Key, JsonElement> modBlockStates() {
        return Collections.unmodifiableMap(this.modBlockStates);
    }

    @Override
    public ConfigParser parser() {
        return this.blockParser;
    }

    @Override
    public Map<Key, Map<String, JsonElement>> blockOverrides() {
        return Collections.unmodifiableMap(this.blockStateOverrides);
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
        BlockRegistryMirror.init(states, new PackedBlockState(Reflections.instance$Blocks$STONE$defaultState, BlockStateUtils.blockStateToId(Reflections.instance$Blocks$STONE$defaultState)));
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
                    Object state = getOnlyBlockState(block);
                    if (BlockStateUtils.isVanillaBlock(state)) {
                        affectedBlocks.add(block);
                    }
                }
            }

            affectedBlocks.remove(Reflections.instance$Blocks$FIRE);
            affectedBlocks.remove(Reflections.instance$Blocks$SOUL_FIRE);

            this.affectedSoundBlocks = ImmutableSet.copyOf(affectedBlocks);

            ImmutableMap.Builder<Key, Key> soundMapperBuilder = ImmutableMap.builder();
            for (Object soundType : affectedSounds) {
                for (Field field : List.of(Reflections.field$SoundType$placeSound, Reflections.field$SoundType$fallSound, Reflections.field$SoundType$hitSound, Reflections.field$SoundType$stepSound, Reflections.field$SoundType$breakSound)) {
                    Object soundEvent = field.get(soundType);
                    Key previousId = Key.of(FastNMS.INSTANCE.field$SoundEvent$location(soundEvent).toString());
                    soundMapperBuilder.put(previousId, Key.of(previousId.namespace(), "replaced." + previousId.value()));
                }
            }

            this.soundMapper = soundMapperBuilder.buildKeepingLast();
        } catch (Throwable e) {
            plugin.logger().warn("Failed to inject blocks.", e);
        }
    }

    public class BlockParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"blocks", "block"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.BLOCK;
        }

        @Override
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            if (id.namespace().equals("minecraft") && Registry.MATERIAL.get(KeyUtils.toNamespacedKey(id)) != null) {
                parseVanillaBlock(pack, path, id, section);
            } else {
                // check duplicated config
                if (byId.containsKey(id)) {
                    throw new LocalizedResourceConfigException("warning.config.block.duplicate");
                }
                parseCustomBlock(pack, path, id, section);
            }
        }

        private void parseCustomBlock(Pack pack, Path path, Key id, Map<String, Object> section) {
            // read block settings
            BlockSettings settings = BlockSettings.fromMap(MiscUtils.castToMap(section.getOrDefault("settings", Map.of()), false));

            // read loot table
            LootTable<ItemStack> lootTable = LootTable.fromMap(MiscUtils.castToMap(section.getOrDefault("loot", Map.of()), false));

            // read states
            Map<String, Property<?>> properties;
            Map<String, Integer> appearances;
            Map<String, VariantState> variants;
            Object stateObj = ResourceConfigUtils.requireNonNullOrThrow(ResourceConfigUtils.get(section, "state", "states"), "warning.config.block.missing_state");
            Map<String, Object> stateSection = MiscUtils.castToMap(stateObj, true);

            // single state
            if (!stateSection.containsKey("properties")) {
                properties = Map.of();
                int internalId = ResourceConfigUtils.getAsInt(stateSection.getOrDefault("id", -1), "id");
                if (internalId < 0) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.missing_real_id");
                }

                Pair<Key, Integer> pair = parseAppearanceSection(id, stateSection);
                if (pair == null) return;

                appearances = Map.of("default", pair.right());
                String internalBlock = pair.left().value() + "_" + internalId;
                Key internalBlockId = Key.of(Key.DEFAULT_NAMESPACE, internalBlock);
                int internalBlockRegistryId = Optional.ofNullable(internalId2StateId.get(internalBlockId)).orElse(-1);
                if (internalBlockRegistryId == -1) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_real_id",
                            internalBlock,
                            String.valueOf(registeredRealBlockSlots.get(pair.left()) - 1));
                }
                variants = Map.of("", new VariantState("default", settings, internalBlockRegistryId));
            } else {
                // properties
                Map<String, Object> propertySection = MiscUtils.castToMap(stateSection.get("properties"), true);
                if (propertySection == null) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.missing_properties");
                }
                properties = parseProperties(propertySection);
                // appearance
                Map<String, Object> appearancesSection = MiscUtils.castToMap(stateSection.get("appearances"), true);
                if (appearancesSection == null) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.missing_appearances");
                }
                appearances = new HashMap<>();
                Map<String, Key> tempTypeMap = new HashMap<>();
                for (Map.Entry<String, Object> appearanceEntry : appearancesSection.entrySet()) {
                    if (appearanceEntry.getValue() instanceof Map<?, ?> appearanceSection) {
                        Pair<Key, Integer> pair = parseAppearanceSection(id, MiscUtils.castToMap(appearanceSection, false));
                        if (pair == null) return;
                        appearances.put(appearanceEntry.getKey(), pair.right());
                        tempTypeMap.put(appearanceEntry.getKey(), pair.left());
                    }
                }
                // variants
                Map<String, Object> variantsSection = MiscUtils.castToMap(stateSection.get("variants"), true);
                if (variantsSection == null) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.missing_variants");
                }
                variants = new HashMap<>();
                for (Map.Entry<String, Object> variantEntry : variantsSection.entrySet()) {
                    if (variantEntry.getValue() instanceof Map<?, ?> variantSection0) {
                        Map<String, Object> variantSection = MiscUtils.castToMap(variantSection0, false);
                        String variantName = variantEntry.getKey();
                        String appearance = (String) variantSection.get("appearance");
                        if (appearance == null) {
                            throw new LocalizedResourceConfigException("warning.config.block.state.variant.missing_appearance", variantName);
                        }
                        if (!appearances.containsKey(appearance)) {
                            throw new LocalizedResourceConfigException("warning.config.block.state.variant.invalid_appearance", variantName, appearance);
                        }
                        int internalId = ResourceConfigUtils.getAsInt(variantSection.getOrDefault("id", -1), "id");
                        Key baseBlock = tempTypeMap.get(appearance);
                        Key internalBlockId = Key.of(Key.DEFAULT_NAMESPACE, baseBlock.value() + "_" + internalId);
                        int internalBlockRegistryId = Optional.ofNullable(internalId2StateId.get(internalBlockId)).orElse(-1);
                        if (internalBlockRegistryId == -1) {
                            throw new LocalizedResourceConfigException("warning.config.block.state.invalid_real_id",
                                    internalBlockId.toString(),
                                    String.valueOf(registeredRealBlockSlots.getOrDefault(baseBlock, 1) - 1));
                        }
                        Map<String, Object> anotherSetting = MiscUtils.castToMap(variantSection.get("settings"), true);
                        variants.put(variantName, new VariantState(appearance, anotherSetting == null ? settings : BlockSettings.ofFullCopy(settings, anotherSetting), internalBlockRegistryId));
                    }
                }
            }

            Object eventsObj = ResourceConfigUtils.get(section, "events", "event");
            EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events = EventFunctions.parseEvents(eventsObj);

            Map<String, Object> behaviors = MiscUtils.castToMap(section.getOrDefault("behavior", Map.of()), false);
            CustomBlock block = BukkitCustomBlock.builder(id)
                    .appearances(appearances)
                    .variantMapper(variants)
                    .lootTable(lootTable)
                    .properties(properties)
                    .settings(settings)
                    .behavior(behaviors)
                    .events(events)
                    .build();

            // bind appearance and real state
            for (ImmutableBlockState state : block.variantProvider().states()) {
                ImmutableBlockState previous = stateId2ImmutableBlockStates[state.customBlockState().registryId() - BlockStateUtils.vanillaStateSize()];
                if (previous != null && !previous.isEmpty()) {
                    TranslationManager.instance().log("warning.config.block.state.bind_failed", path.toString(), id.toString(), state.toString(), previous.toString());
                    continue;
                }
                stateId2ImmutableBlockStates[state.customBlockState().registryId() - BlockStateUtils.vanillaStateSize()] = state;
                tempBlockAppearanceConvertor.put(state.customBlockState().registryId(), state.vanillaBlockState().registryId());
                appearanceToRealState.computeIfAbsent(state.vanillaBlockState().registryId(), k -> new ArrayList<>()).add(state.customBlockState().registryId());
            }

            BukkitBlockManager.this.byId.put(id, block);

            // generate mod assets
            if (Config.generateModAssets()) {
                for (ImmutableBlockState state : block.variantProvider().states()) {
                    Key realBlockId = BlockStateUtils.getBlockOwnerIdFromState(state.customBlockState());
                    modBlockStates.put(realBlockId, tempVanillaBlockStateModels.get(state.vanillaBlockState().registryId()));
                }
            }
        }

        private void parseVanillaBlock(Pack pack, Path path, Key id, Map<String, Object> section) {
            Map<String, Object> settings = MiscUtils.castToMap(section.get("settings"), true);
            if (settings != null) {
                Object clientBoundTags = settings.get("client-bound-tags");
                if (clientBoundTags instanceof List<?> list) {
                    List<String> clientSideTags = MiscUtils.getAsStringList(list).stream().filter(ResourceLocation::isValid).toList();
                    try {
                        Object nmsBlock = Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$BLOCK, KeyUtils.toResourceLocation(id));
                        FastNMS.INSTANCE.method$IdMap$getId(Reflections.instance$BuiltInRegistries$BLOCK, nmsBlock).ifPresent(i ->
                                BukkitBlockManager.this.clientBoundTags.put(i, clientSideTags));
                    } catch (ReflectiveOperationException e) {
                        BukkitBlockManager.this.plugin.logger().warn("Unable to get block " + id, e);
                    }
                }
            }
        }
    }

    private Map<String, Property<?>> parseProperties(Map<String, Object> propertiesSection) {
        Map<String, Property<?>> properties = new HashMap<>();
        for (Map.Entry<String, Object> entry : propertiesSection.entrySet()) {
            Property<?> property = Properties.fromMap(entry.getKey(), MiscUtils.castToMap(entry.getValue(), false));
            properties.put(entry.getKey(), property);
        }
        return properties;
    }

    @Nullable
    private Pair<Key, Integer> parseAppearanceSection(Key id, Map<String, Object> section) {
        // require state non null
        Object vanillaStateString = section.get("state");
        if (vanillaStateString == null) {
            throw new LocalizedResourceConfigException("warning.config.block.state.missing_state");
        }

        // get its registry id
        int vanillaStateRegistryId = parseVanillaStateRegistryId(vanillaStateString.toString());

        // check conflict
        Key ifAny = this.tempRegistryIdConflictMap.get(vanillaStateRegistryId);
        if (ifAny != null && !ifAny.equals(id)) {
            throw new LocalizedResourceConfigException("warning.config.block.state.conflict", BlockStateUtils.fromBlockData(BlockStateUtils.idToBlockState(vanillaStateRegistryId)).getAsString(), ifAny.toString());
        }

        // require models not to be null
        Object models = section.get("models");
        if (models == null) {
            models = section.get("model");
        }
        if (models == null) {
            throw new LocalizedResourceConfigException("warning.config.block.state.missing_model");
        }

        List<JsonObject> variants = new ArrayList<>();
        if (models instanceof Map<?, ?> singleModelSection) {
            loadVariantModel(variants, MiscUtils.castToMap(singleModelSection, false));
        } else if (models instanceof List<?> modelList) {
            for (Object model : modelList) {
                if (model instanceof Map<?,?> singleModelMap) {
                    loadVariantModel(variants, MiscUtils.castToMap(singleModelMap, false));
                }
            }
        }
        if (variants.isEmpty()) return null;

        this.tempRegistryIdConflictMap.put(vanillaStateRegistryId, id);
        String blockState = BlockStateUtils.idToBlockState(vanillaStateRegistryId).toString();
        Key block = Key.of(blockState.substring(blockState.indexOf('{') + 1, blockState.lastIndexOf('}')));
        String propertyData = blockState.substring(blockState.indexOf('[') + 1, blockState.lastIndexOf(']'));
        Map<String, JsonElement> paths = this.blockStateOverrides.computeIfAbsent(block, k -> new HashMap<>());
        if (variants.size() == 1) {
            paths.put(propertyData, variants.get(0));
            this.tempVanillaBlockStateModels.put(vanillaStateRegistryId, variants.get(0));
        } else {
            JsonArray array = new JsonArray();
            for (JsonObject object : variants) {
                array.add(object);
            }
            paths.put(propertyData, array);
            this.tempVanillaBlockStateModels.put(vanillaStateRegistryId, array);
        }
        return Pair.of(block, vanillaStateRegistryId);
    }

    private void loadVariantModel(List<JsonObject> variants, Map<String, Object> singleModelMap) {
        JsonObject json = new JsonObject();
        String modelPath = (String) singleModelMap.get("path");
        if (modelPath == null) {
            throw new LocalizedResourceConfigException("warning.config.block.state.model.missing_path");
        }
        if (!ResourceLocation.isValid(modelPath)) {
            throw new LocalizedResourceConfigException("warning.config.block.state.model.invalid_path", modelPath);
        }
        json.addProperty("model", modelPath);
        if (singleModelMap.containsKey("x")) json.addProperty("x", ResourceConfigUtils.getAsInt(singleModelMap.get("x"), "x"));
        if (singleModelMap.containsKey("y")) json.addProperty("y", ResourceConfigUtils.getAsInt(singleModelMap.get("y"), "y"));
        if (singleModelMap.containsKey("uvlock")) json.addProperty("uvlock", (boolean) singleModelMap.get("uvlock"));
        if (singleModelMap.containsKey("weight")) json.addProperty("weight", ResourceConfigUtils.getAsInt(singleModelMap.get("weight"), "weight"));
        Map<String, Object> generationMap = MiscUtils.castToMap(singleModelMap.get("generation"), true);
        if (generationMap != null) {
            prepareModelGeneration(ModelGeneration.of(Key.of(modelPath), generationMap));
        }
        variants.add(json);
    }

    private int parseVanillaStateRegistryId(String blockState) {
        String[] split = blockState.split(":", 3);
        if (split.length >= 4) {
            throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", blockState);
        }
        int registryId;
        String stateOrId = split[split.length - 1];
        boolean isId = !stateOrId.contains("[") && !stateOrId.contains("]");
        if (isId) {
            if (split.length == 1) {
                throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", blockState);
            }
            Key block = split.length == 2 ? Key.of(split[0]) : Key.of(split[0], split[1]);
            try {
                int id = split.length == 2 ? Integer.parseInt(split[1]) : Integer.parseInt(split[2]);
                if (id < 0) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", blockState);
                }
                List<Integer> arranger = this.blockAppearanceArranger.get(block);
                if (arranger == null) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.unavailable_vanilla", blockState);
                }
                if (id >= arranger.size()) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla_id", blockState, String.valueOf(arranger.size() - 1));
                }
                registryId = arranger.get(id);
            } catch (NumberFormatException e) {
                throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", e, blockState);
            }
        } else {
            try {
                BlockData blockData = Bukkit.createBlockData(blockState);
                registryId = BlockStateUtils.blockDataToId(blockData);
                if (!this.blockAppearanceMapper.containsKey(registryId)) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.unavailable_vanilla", blockState);
                }
            } catch (IllegalArgumentException e) {
                throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", e, blockState);
            }
        }
        return registryId;
    }

    private void loadMappingsAndAdditionalBlocks() {
        this.plugin.logger().info("Loading mappings.yml.");
        File mappingFile = new File(plugin.dataFolderFile(), "mappings.yml");
        YamlDocument mappings = Config.instance().loadOrCreateYamlData("mappings.yml");
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
        YamlDocument additionalYaml = Config.instance().loadOrCreateYamlData("additional-real-blocks.yml");
        this.registeredRealBlockSlots = this.buildRegisteredRealBlockSlots(blockTypeCounter, additionalYaml);
    }

    private void recordVanillaNoteBlocks() {
        try {
            Object resourceLocation = KeyUtils.toResourceLocation(BlockKeys.NOTE_BLOCK);
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
                Optional<Object> optionalHolder = (Optional<Object>) Reflections.method$Registry$getHolder1.invoke(Reflections.instance$BuiltInRegistries$BLOCK, Reflections.method$ResourceKey$create.invoke(null, Reflections.instance$Registries$BLOCK, resourceLocation));
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

            deceiveBukkit(newRealBlock, clientSideBlockType, isNoteBlock);
            order.add(realBlockKey);
            counter++;
        }

        builder3.put(clientSideBlockType, stateIds);
        Object soundType = Reflections.field$BlockBehaviour$soundType.get(clientSideBlock);
        affectSoundTypes.add(soundType);
        return counter;
    }

    private Object createResourceLocation(Key key) {
        return FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath(key.namespace(), key.value());
    }

    private Object getBlockFromRegistry(Object resourceLocation) throws Exception {
        return Reflections.method$Registry$get.invoke(Reflections.instance$BuiltInRegistries$BLOCK, resourceLocation);
    }

    private Key createRealBlockKey(Key replacedBlock, int index) {
        return Key.of(Key.DEFAULT_NAMESPACE, replacedBlock.value() + "_" + index);
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

    @SuppressWarnings("unchecked")
    private void deceiveBukkit(Object newBlock, Key replacedBlock, boolean isNoteBlock) throws IllegalAccessException {
        Map<Object, Material> magicMap = (Map<Object, Material>) Reflections.field$CraftMagicNumbers$BLOCK_MATERIAL.get(null);
        Map<Material, Object> factories = (Map<Material, Object>) Reflections.field$CraftBlockStates$FACTORIES.get(null);
        if (isNoteBlock) {
            magicMap.put(newBlock, Material.STONE);
        } else {
            Material material = org.bukkit.Registry.MATERIAL.get(new NamespacedKey(replacedBlock.namespace(), replacedBlock.value()));
            if (Reflections.clazz$CraftBlockStates$BlockEntityStateFactory.isInstance(factories.get(material))) {
                magicMap.put(newBlock, Material.STONE);
            } else {
                magicMap.put(newBlock, material);
            }
        }
    }
}
