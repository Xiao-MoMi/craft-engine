package net.momirealms.craftengine.core.block;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigs;
import net.momirealms.craftengine.core.block.parser.BlockNbtParser;
import net.momirealms.craftengine.core.block.properties.Properties;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.allocator.BlockStateCandidate;
import net.momirealms.craftengine.core.pack.allocator.IdAllocator;
import net.momirealms.craftengine.core.pack.allocator.VisualBlockStateAllocator;
import net.momirealms.craftengine.core.pack.model.generation.AbstractModelGenerator;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.SectionConfigParser;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public abstract class AbstractBlockManager extends AbstractModelGenerator implements BlockManager {
    private static final JsonElement EMPTY_VARIANT_MODEL = MiscUtils.init(new JsonObject(), o -> o.addProperty("model", "minecraft:block/empty"));
    private static final AABB DEFAULT_BLOCK_ENTITY_AABB = new AABB(-.5, -.5, -.5, .5, .5, .5);
    protected final BlockParser blockParser;
    protected final BlockStateMappingParser blockStateMappingParser;
    // 根据id获取自定义方块
    protected final Map<Key, CustomBlock> byId = new HashMap<>();
    // 缓存的指令建议
    protected final List<Suggestion> cachedSuggestions = new ArrayList<>();
    // 缓存的使用中的命名空间
    protected final Set<String> namespacesInUse = new HashSet<>();
    // Map<方块类型, Map<方块状态NBT,模型>>，用于生成block state json
    protected final Map<Key, Map<String, JsonElement>> blockStateOverrides = new HashMap<>();
    // 用于生成mod使用的block state json
    protected final Map<Key, JsonElement> modBlockStateOverrides = new HashMap<>();
    // 根据外观查找真实状态，用于debug指令
    protected final Map<Integer, List<Integer>> appearanceToRealState = new Int2ObjectOpenHashMap<>();
    // 用于note_block:0这样格式的自动分配
    protected final Map<Key, List<BlockStateWrapper>> blockStateArranger = new HashMap<>();
    // 全方块状态映射文件，用于网络包映射
    protected final int[] blockStateMappings;
    // 原版方块状态数量
    protected final int vanillaBlockStateCount;
    // 注册的大宝贝
    protected final DelegatingBlock[] customBlocks;
    protected final DelegatingBlockState[] customBlockStates;
    protected final Object[] customBlockHolders;
    // 自定义状态列表，会随着重载变化
    protected final ImmutableBlockState[] immutableBlockStates;
    // 倒推缓存
    protected final BlockStateCandidate[] autoVisualBlockStateCandidates;
    // 用于检测单个外观方块状态是否被绑定了不同模型
    protected final JsonElement[] tempVanillaBlockStateModels;
    // 临时存储哪些视觉方块被使用了
    protected final Set<BlockStateWrapper> tempVisualBlockStatesInUse = new HashSet<>();
    protected final Set<Key> tempVisualBlocksInUse = new HashSet<>();
    // 能遮挡视线的方块
    protected final boolean[] viewBlockingBlocks;
    // 声音映射表，和使用了哪些视觉方块有关
    protected Map<Key, Key> soundReplacements = Map.of();
    // 是否使用了透明方块模型
    protected boolean isTransparentModelInUse = false;

    protected AbstractBlockManager(CraftEngine plugin, int vanillaBlockStateCount, int customBlockCount) {
        super(plugin);
        this.vanillaBlockStateCount = vanillaBlockStateCount;
        this.customBlocks = new DelegatingBlock[customBlockCount];
        this.customBlockHolders = new Object[customBlockCount];
        this.customBlockStates = new DelegatingBlockState[customBlockCount];
        this.immutableBlockStates = new ImmutableBlockState[customBlockCount];
        this.blockStateMappings = new int[customBlockCount + vanillaBlockStateCount];
        this.autoVisualBlockStateCandidates = new BlockStateCandidate[vanillaBlockStateCount];
        this.tempVanillaBlockStateModels = new JsonElement[vanillaBlockStateCount];
        this.blockParser = new BlockParser(this.autoVisualBlockStateCandidates);
        this.blockStateMappingParser = new BlockStateMappingParser();
        this.viewBlockingBlocks = new boolean[vanillaBlockStateCount + customBlockCount];
        Arrays.fill(this.blockStateMappings, -1);
    }

    @NotNull
    @Override
    public ImmutableBlockState getImmutableBlockStateUnsafe(int stateId) {
        return this.immutableBlockStates[stateId - this.vanillaBlockStateCount];
    }

    @Nullable
    @Override
    public ImmutableBlockState getImmutableBlockState(int stateId) {
        if (!isVanillaBlockState(stateId)) {
            return this.immutableBlockStates[stateId - this.vanillaBlockStateCount];
        }
        return null;
    }

    @Override
    public void unload() {
        super.clearModelsToGenerate();
        this.clearCache();
        this.cachedSuggestions.clear();
        this.namespacesInUse.clear();
        this.blockStateOverrides.clear();
        this.modBlockStateOverrides.clear();
        this.byId.clear();
        this.blockStateArranger.clear();
        this.appearanceToRealState.clear();
        this.isTransparentModelInUse = false;
        Arrays.fill(this.blockStateMappings, -1);
        Arrays.fill(this.immutableBlockStates, EmptyBlock.STATE);
        Arrays.fill(this.autoVisualBlockStateCandidates, null);
        for (AutoStateGroup autoStateGroup : AutoStateGroup.values()) {
            autoStateGroup.reset();
        }
    }

    @Override
    public void delayedLoad() {
        this.initSuggestions();
        this.updateTags();
        this.processSounds();
        this.clearCache();
    }

    @Override
    public boolean isTransparentModelInUse() {
        return this.isTransparentModelInUse;
    }

    @Override
    public Map<Key, CustomBlock> loadedBlocks() {
        return Collections.unmodifiableMap(this.byId);
    }

    @Override
    public Optional<CustomBlock> blockById(Key id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    public Map<Key, List<BlockStateWrapper>> blockStateArranger() {
        return this.blockStateArranger;
    }

    protected abstract void applyPlatformSettings(CustomBlock block, ImmutableBlockState state);

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[]{this.blockParser, this.blockStateMappingParser};
    }

    @Override
    public Map<Key, JsonElement> modBlockStates() {
        return Collections.unmodifiableMap(this.modBlockStateOverrides);
    }

    @Override
    public Map<Key, Map<String, JsonElement>> blockOverrides() {
        return Collections.unmodifiableMap(this.blockStateOverrides);
    }

    @Override
    public Collection<Suggestion> cachedSuggestions() {
        return Collections.unmodifiableCollection(this.cachedSuggestions);
    }

    @Nullable
    public Key replaceSoundIfExist(Key id) {
        return this.soundReplacements.get(id);
    }

    @Override
    public Map<Key, Key> soundReplacements() {
        return Collections.unmodifiableMap(this.soundReplacements);
    }

    public Set<String> namespacesInUse() {
        return Collections.unmodifiableSet(this.namespacesInUse);
    }

    protected void clearCache() {
        Arrays.fill(this.tempVanillaBlockStateModels, null);
        this.tempVisualBlockStatesInUse.clear();
        this.tempVisualBlocksInUse.clear();
    }

    protected void initSuggestions() {
        this.cachedSuggestions.clear();
        this.namespacesInUse.clear();
        Set<String> states = new HashSet<>();
        for (CustomBlock block : this.byId.values()) {
            states.add(block.id().toString());
            this.namespacesInUse.add(block.id().namespace());
            for (ImmutableBlockState state : block.variantProvider().states()) {
                states.add(state.toString());
            }
        }
        for (String state : states) {
            this.cachedSuggestions.add(Suggestion.suggestion(state));
        }
    }

    @NotNull
    public List<Integer> appearanceToRealStates(int appearanceStateId) {
        return Optional.ofNullable(this.appearanceToRealState.get(appearanceStateId)).orElse(List.of());
    }

    public abstract BlockBehavior createBlockBehavior(CustomBlock customBlock, List<Map<String, Object>> behaviorConfig);

    public boolean isViewBlockingBlock(int stateId) {
        return this.viewBlockingBlocks[stateId];
    }

    protected abstract void updateTags();

    protected abstract boolean isVanillaBlock(Key id);

    protected abstract Key getBlockOwnerId(int id);

    protected abstract void setVanillaBlockTags(Key id, List<String> tags);

    protected abstract int vanillaBlockStateCount();

    protected abstract void processSounds();

    protected abstract CustomBlock createCustomBlock(@NotNull Holder.Reference<CustomBlock> holder,
                                                     @NotNull BlockStateVariantProvider variantProvider,
                                                     @NotNull Map<EventTrigger, List<Function<Context>>> events,
                                                     @Nullable LootTable<?> lootTable);

    public class BlockStateMappingParser extends SectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[]{"block-state-mappings", "block-state-mapping"};
        private int count;

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.BLOCK_STATE_MAPPING;
        }

        @Override
        public int count() {
            return this.count;
        }

        @Override
        public void preProcess() {
            this.count = 0;
        }

        @Override
        public void parseSection(Pack pack, Path path, Map<String, Object> section) throws LocalizedException {
            ExceptionCollector<LocalizedResourceConfigException> exceptionCollector = new ExceptionCollector<>();
            for (Map.Entry<String, Object> entry : section.entrySet()) {
                String before = entry.getKey();
                String after = entry.getValue().toString();
                // 先解析为唯一的wrapper
                BlockStateWrapper beforeState = createVanillaBlockState(before);
                BlockStateWrapper afterState = createVanillaBlockState(after);
                if (beforeState == null) {
                    exceptionCollector.add(new LocalizedResourceConfigException("warning.config.block_state_mapping.invalid_state", before));
                    continue;
                }
                if (afterState == null) {
                    exceptionCollector.add(new LocalizedResourceConfigException("warning.config.block_state_mapping.invalid_state", after));
                    continue;
                }
                int previous = AbstractBlockManager.this.blockStateMappings[beforeState.registryId()];
                if (previous != -1 && previous != afterState.registryId()) {
                    exceptionCollector.add(new LocalizedResourceConfigException("warning.config.block_state_mapping.conflict",
                            beforeState.toString(),
                            afterState.toString(),
                            BlockRegistryMirror.byId(previous).toString()));
                    continue;
                }
                AbstractBlockManager.this.blockStateMappings[beforeState.registryId()] = afterState.registryId();
                Key blockOwnerId = getBlockOwnerId(beforeState);
                List<BlockStateWrapper> blockStateWrappers = AbstractBlockManager.this.blockStateArranger.computeIfAbsent(blockOwnerId, k -> new ArrayList<>());
                blockStateWrappers.add(beforeState);
                AbstractBlockManager.this.autoVisualBlockStateCandidates[beforeState.registryId()] = createVisualBlockCandidate(beforeState);
                this.count++;
            }
            exceptionCollector.throwIfPresent();
        }

        @Nullable
        public BlockStateCandidate createVisualBlockCandidate(BlockStateWrapper blockState) {
            List<AutoStateGroup> groups = AutoStateGroup.findGroups(blockState);
            if (!groups.isEmpty()) {
                BlockStateCandidate candidate = new BlockStateCandidate(blockState);
                for (AutoStateGroup group : groups) {
                    group.addCandidate(candidate);
                }
                return candidate;
            }
            return null;
        }
    }

    public class BlockParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[]{"blocks", "block"};
        private final IdAllocator internalIdAllocator;
        private final VisualBlockStateAllocator visualBlockStateAllocator;
        private final List<PendingConfigSection> pendingConfigSections = new ArrayList<>();

        public BlockParser(BlockStateCandidate[] candidates) {
            this.internalIdAllocator = new IdAllocator(AbstractBlockManager.this.plugin.dataFolderPath().resolve("cache").resolve("custom-block-states.json"));
            this.visualBlockStateAllocator = new VisualBlockStateAllocator(AbstractBlockManager.this.plugin.dataFolderPath().resolve("cache").resolve("visual-block-states.json"), candidates, AbstractBlockManager.this::createVanillaBlockState);
        }

        @Override
        public int count() {
            return AbstractBlockManager.this.byId.size();
        }

        public void addPendingConfigSection(PendingConfigSection section) {
            this.pendingConfigSections.add(section);
        }

        public IdAllocator internalIdAllocator() {
            return internalIdAllocator;
        }

        public VisualBlockStateAllocator visualBlockStateAllocator() {
            return visualBlockStateAllocator;
        }

        @Override
        public void postProcess() {
            this.internalIdAllocator.processPendingAllocations();
            try {
                this.internalIdAllocator.saveToCache();
            } catch (IOException e) {
                AbstractBlockManager.this.plugin.logger().warn("Error while saving custom block states allocation", e);
            }
            this.visualBlockStateAllocator.processPendingAllocations();
            try {
                this.visualBlockStateAllocator.saveToCache();
            } catch (IOException e) {
                AbstractBlockManager.this.plugin.logger().warn("Error while saving visual block states allocation", e);
            }
        }

        @Override
        public void preProcess() {
            this.internalIdAllocator.reset(0, Config.serverSideBlocks() - 1);
            this.visualBlockStateAllocator.reset();
            try {
                this.visualBlockStateAllocator.loadFromCache();
            } catch (IOException e) {
                AbstractBlockManager.this.plugin.logger().warn("Error while loading visual block states allocation cache", e);
            }
            try {
                this.internalIdAllocator.loadFromCache();
            } catch (IOException e) {
                AbstractBlockManager.this.plugin.logger().warn("Error while loading custom block states allocation cache", e);
            }
            if (!this.pendingConfigSections.isEmpty()) {
                for (PendingConfigSection section : this.pendingConfigSections) {
                    ResourceConfigUtils.runCatching(
                            section.path(),
                            section.node(),
                            () -> parseSection(section.pack(), section.path(), section.node(), section.id(), section.config()),
                            () -> GsonHelper.get().toJson(section.config())
                    );
                }
                this.pendingConfigSections.clear();
            }
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.BLOCK;
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            if (isVanillaBlock(id)) {
                parseVanillaBlock(id, section);
            } else {
                // check duplicated config
                if (AbstractBlockManager.this.byId.containsKey(id)) {
                    throw new LocalizedResourceConfigException("warning.config.block.duplicate");
                }
                parseCustomBlock(path, node, id, section);
            }
        }

        private void parseVanillaBlock(Key id, Map<String, Object> section) {
            Map<String, Object> settings = MiscUtils.castToMap(section.get("settings"), true);
            if (settings != null) {
                Object clientBoundTags = settings.get("client-bound-tags");
                if (clientBoundTags instanceof List<?> list) {
                    List<String> clientSideTags = MiscUtils.getAsStringList(list).stream().filter(ResourceLocation::isValid).toList();
                    AbstractBlockManager.this.setVanillaBlockTags(id, clientSideTags);
                }
            }
        }

        private void parseCustomBlock(Path path, String node, Key id, Map<String, Object> section) {
            // 获取共享方块设置
            BlockSettings settings = BlockSettings.fromMap(id, MiscUtils.castToMap(section.get("settings"), true));
            // 读取states区域
            Map<String, Object> stateSection = MiscUtils.castToMap(ResourceConfigUtils.requireNonNullOrThrow(ResourceConfigUtils.get(section, "state", "states"), "warning.config.block.missing_state"), true);
            boolean singleState = !stateSection.containsKey("properties");
            // 读取方块的property，通过property决定
            Map<String, Property<?>> properties = singleState ? Map.of() : parseBlockProperties(ResourceConfigUtils.getAsMap(ResourceConfigUtils.requireNonNullOrThrow(stateSection.get("properties"), "warning.config.block.state.missing_properties"), "properties"));
            // 注册方块容器
            Holder.Reference<CustomBlock> holder = ((WritableRegistry<CustomBlock>) BuiltInRegistries.BLOCK).getOrRegisterForHolder(ResourceKey.create(BuiltInRegistries.BLOCK.key().location(), id));
            // 先绑定无效方块
            holder.bindValue(new InactiveCustomBlock(holder));

            // 根据properties生成variant provider
            BlockStateVariantProvider variantProvider = new BlockStateVariantProvider(holder, (owner, propertyMap) -> {
                ImmutableBlockState blockState = new ImmutableBlockState(owner, propertyMap);
                blockState.setSettings(settings);
                return blockState;
            }, properties);

            ImmutableList<ImmutableBlockState> states = variantProvider.states();
            List<CompletableFuture<Integer>> internalIdAllocators = new ArrayList<>(states.size());

            // 如果用户指定了起始id
            if (stateSection.containsKey("id")) {
                int startingId = ResourceConfigUtils.getAsInt(stateSection.get("id"), "id");
                int endingId = startingId + states.size() - 1;
                if (startingId < 0 || endingId >= Config.serverSideBlocks()) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_id", startingId + "~" + endingId, String.valueOf(Config.serverSideBlocks() - 1));
                }
                // 先检测范围冲突
                List<Pair<String, Integer>> conflicts = this.internalIdAllocator.getFixedIdsBetween(startingId, endingId);
                if (!conflicts.isEmpty()) {
                    ExceptionCollector<LocalizedResourceConfigException> exceptionCollector = new ExceptionCollector<>();
                    for (Pair<String, Integer> conflict : conflicts) {
                        int internalId = conflict.right();
                        int index = internalId - startingId;
                        exceptionCollector.add(new LocalizedResourceConfigException("warning.config.block.state.id.conflict", states.get(index).toString(), conflict.left(), BlockManager.createCustomBlockKey(internalId).toString()));
                    }
                    exceptionCollector.throwIfPresent();
                }
                // 强行分配id
                for (ImmutableBlockState blockState : states) {
                    String blockStateId = blockState.toString();
                    internalIdAllocators.add(this.internalIdAllocator.assignFixedId(blockStateId, startingId++));
                }
            }
            // 未指定，则使用自动分配
            else {
                for (ImmutableBlockState blockState : states) {
                    String blockStateId = blockState.toString();
                    internalIdAllocators.add(this.internalIdAllocator.requestAutoId(blockStateId));
                }
            }

            CompletableFutures.allOf(internalIdAllocators).whenComplete((v1, t1) -> ResourceConfigUtils.runCatching(path, node, () -> {
                if (t1 != null) {
                    if (t1 instanceof CompletionException e) {
                        Throwable cause = e.getCause();
                        // 这里不会有conflict了，因为之前已经判断过了
                        if (cause instanceof IdAllocator.IdExhaustedException) {
                            throw new LocalizedResourceConfigException("warning.config.block.state.id.exhausted");
                        } else {
                            Debugger.BLOCK.warn(() -> "Unknown error while allocating internal block state id.", cause);
                            return;
                        }
                    }
                    throw new RuntimeException("Unknown error occurred", t1);
                }

                for (int i = 0; i < internalIdAllocators.size(); i++) {
                    CompletableFuture<Integer> future = internalIdAllocators.get(i);
                    try {
                        int internalId = future.get();
                        states.get(i).setCustomBlockState(BlockRegistryMirror.byId(internalId + AbstractBlockManager.this.vanillaBlockStateCount));
                    } catch (InterruptedException | ExecutionException e) {
                        AbstractBlockManager.this.plugin.logger().warn("Interrupted while allocating internal block state for block " + id.asString(), e);
                        return;
                    }
                }

                ExceptionCollector<LocalizedResourceConfigException> eCollector1 = new ExceptionCollector<>();

                Map<EventTrigger, List<Function<Context>>> events;
                try {
                    events = CommonFunctions.parseEvents(ResourceConfigUtils.get(section, "events", "event"));
                } catch (LocalizedResourceConfigException e) {
                    eCollector1.add(e);
                    events = Map.of();
                }
                LootTable<?> lootTable;
                try {
                    lootTable = LootTable.fromMap(ResourceConfigUtils.getAsMapOrNull(section.get("loot"), "loot"));
                } catch (LocalizedResourceConfigException e) {
                    eCollector1.add(e);
                    lootTable = null;
                }
                // 创建自定义方块
                AbstractCustomBlock customBlock = (AbstractCustomBlock) createCustomBlock(holder, variantProvider, events, lootTable);
                BlockBehavior blockBehavior = createBlockBehavior(customBlock, MiscUtils.getAsMapList(ResourceConfigUtils.get(section, "behavior", "behaviors")));

                Map<String, Map<String, Object>> appearanceConfigs;
                Map<String, CompletableFuture<BlockStateWrapper>> futureVisualStates = new HashMap<>();
                if (singleState) {
                    appearanceConfigs = Map.of("", stateSection);
                } else {
                    Map<String, Object> rawAppearancesSection = ResourceConfigUtils.getAsMap(ResourceConfigUtils.requireNonNullOrThrow(stateSection.get("appearances"), "warning.config.block.state.missing_appearances"), "appearances");
                    appearanceConfigs = new LinkedHashMap<>(4);
                    for (Map.Entry<String, Object> entry : rawAppearancesSection.entrySet()) {
                        appearanceConfigs.put(entry.getKey(), ResourceConfigUtils.getAsMap(entry.getValue(), entry.getKey()));
                    }
                }

                for (Map.Entry<String, Map<String, Object>> entry : appearanceConfigs.entrySet()) {
                    Map<String, Object> appearanceSection = entry.getValue();
                    if (appearanceSection.containsKey("state")) {
                        String appearanceName = entry.getKey();
                        futureVisualStates.put(
                                appearanceName,
                                this.visualBlockStateAllocator.assignFixedBlockState(appearanceName.isEmpty() ? id.asString() : id.asString() + ":" + appearanceName, parsePluginFormattedBlockState(appearanceSection.get("state").toString()))
                        );
                    } else if (appearanceSection.containsKey("auto-state")) {
                        String appearanceName = entry.getKey();
                        Object autoState = appearanceSection.get("auto-state");
                        String autoStateType;
                        String autoStateId;
                        if (autoState instanceof Map<?,?> map) {
                            Map<String, Object> config = ResourceConfigUtils.getAsMap(map, "auto-state");
                            autoStateType = config.getOrDefault("type", "solid").toString();
                            if (map.containsKey("id")) {
                                autoStateId = autoStateType + "[id=" + map.get("id").toString() + "]";
                            } else {
                                autoStateId = appearanceName.isEmpty() ? id.asString() : id.asString() + "[appearance=" + appearanceName + "]";
                            }
                        } else {
                            autoStateType = autoState.toString();
                            autoStateId = appearanceName.isEmpty() ? id.asString() : id.asString() + "[appearance=" + appearanceName + "]";
                        }
                        AutoStateGroup group = AutoStateGroup.byId(autoStateType);
                        if (group == null) {
                            throw new LocalizedResourceConfigException("warning.config.block.state.invalid_auto_state", autoStateType, EnumUtils.toString(AutoStateGroup.values()));
                        }
                        futureVisualStates.put(appearanceName, this.visualBlockStateAllocator.requestAutoState(autoStateId, group));
                    } else {
                        throw new LocalizedResourceConfigException("warning.config.block.state.missing_state");
                    }
                }

                CompletableFutures.allOf(futureVisualStates.values()).whenComplete((v2, t2) -> ResourceConfigUtils.runCatching(path, node, () -> {
                    if (t2 != null) {
                        if (t2 instanceof CompletionException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof VisualBlockStateAllocator.StateExhaustedException exhausted) {
                                throw new LocalizedResourceConfigException("warning.config.block.state.auto_state.exhausted", exhausted.group().id(), String.valueOf(exhausted.group().candidateCount()));
                            } else {
                                Debugger.BLOCK.warn(() -> "Unknown error while allocating visual block state.", cause);
                                return;
                            }
                        }
                        throw new RuntimeException("Unknown error occurred", t2);
                    }

                    BlockStateAppearance anyAppearance = null;
                    Map<String, BlockStateAppearance> appearances = new HashMap<>();
                    for (Map.Entry<String, Map<String, Object>> entry : appearanceConfigs.entrySet()) {
                        String appearanceName = entry.getKey();
                        Map<String, Object> appearanceSection = entry.getValue();
                        BlockStateWrapper visualBlockState;
                        try {
                            visualBlockState = futureVisualStates.get(appearanceName).get();
                        } catch (InterruptedException | ExecutionException e) {
                            AbstractBlockManager.this.plugin.logger().warn("Interrupted while allocating visual block state for block " + id.asString(), e);
                            return;
                        }
                        if (ResourceConfigUtils.getAsBoolean(appearanceSection.getOrDefault("transparent", false), "transparent")) {
                            AbstractBlockManager.this.isTransparentModelInUse = true;
                            this.arrangeModelForStateAndVerify(visualBlockState, EMPTY_VARIANT_MODEL);
                        } else {
                            Object modelConfig = ResourceConfigUtils.get(appearanceSection, "model", "models");
                            if (modelConfig != null) {
                                this.arrangeModelForStateAndVerify(visualBlockState, parseBlockModel(modelConfig));
                            }
                        }
                        BlockStateAppearance blockStateAppearance = new BlockStateAppearance(
                                visualBlockState,
                                parseBlockEntityRender(ResourceConfigUtils.get(appearanceSection, "entity-renderer", "entity-render")),
                                parseCullingData(appearanceSection.get("entity-culling"))
                        );
                        appearances.put(appearanceName, blockStateAppearance);
                        if (anyAppearance == null) {
                            anyAppearance = blockStateAppearance;
                        }
                    }

                    // 至少有一个外观吧
                    Objects.requireNonNull(anyAppearance, "any appearance should not be null");

                    ExceptionCollector<LocalizedResourceConfigException> eCollector2 = new ExceptionCollector<>();
                    if (!singleState) {
                        Map<String, Object> variantsSection = ResourceConfigUtils.getAsMapOrNull(stateSection.get("variants"), "variants");
                        if (variantsSection != null) {
                            for (Map.Entry<String, Object> entry : variantsSection.entrySet()) {
                                Map<String, Object> variantSection = ResourceConfigUtils.getAsMap(entry.getValue(), entry.getKey());
                                String variantNBT = entry.getKey();
                                // 先解析nbt，找到需要修改的方块状态
                                CompoundTag tag = BlockNbtParser.deserialize(variantProvider, variantNBT);
                                if (tag == null) {
                                    eCollector2.add(new LocalizedResourceConfigException("warning.config.block.state.property.invalid_format", variantNBT));
                                    continue;
                                }
                                List<ImmutableBlockState> possibleStates = variantProvider.getPossibleStates(tag);
                                Map<String, Object> anotherSetting = ResourceConfigUtils.getAsMapOrNull(variantSection.get("settings"), "settings");
                                if (anotherSetting != null) {
                                    for (ImmutableBlockState possibleState : possibleStates) {
                                        possibleState.setSettings(BlockSettings.ofFullCopy(possibleState.settings(), anotherSetting));
                                    }
                                }
                                String appearanceName = ResourceConfigUtils.getAsStringOrNull(variantSection.get("appearance"));
                                if (appearanceName != null) {
                                    BlockStateAppearance appearance = appearances.get(appearanceName);
                                    if (appearance == null) {
                                        eCollector2.add(new LocalizedResourceConfigException("warning.config.block.state.variant.invalid_appearance", variantNBT, appearanceName));
                                        continue;
                                    }
                                    for (ImmutableBlockState possibleState : possibleStates) {
                                        possibleState.setVisualBlockState(appearance.blockState());
                                        possibleState.setCullingData(appearance.cullingData());
                                        appearance.blockEntityRenderer().ifPresent(possibleState::setConstantRenderers);
                                    }
                                }
                            }
                        }
                    }

                    // 获取方块实体行为
                    EntityBlockBehavior entityBlockBehavior = blockBehavior.getEntityBehavior();
                    boolean isEntityBlock = entityBlockBehavior != null;

                    // 绑定行为
                    for (ImmutableBlockState state : states) {
                        if (isEntityBlock) {
                            state.setBlockEntityType(entityBlockBehavior.blockEntityType(state));
                        }
                        state.setBehavior(blockBehavior);
                        int internalId = state.customBlockState().registryId();
                        BlockStateWrapper visualState = state.visualBlockState();
                        // 校验，为未绑定外观的强行添加外观
                        if (visualState == null) {
                            visualState = anyAppearance.blockState();
                            state.setVisualBlockState(visualState);
                            state.setCullingData(anyAppearance.cullingData());
                            anyAppearance.blockEntityRenderer().ifPresent(state::setConstantRenderers);
                        }
                        int appearanceId = visualState.registryId();
                        int index = internalId - AbstractBlockManager.this.vanillaBlockStateCount;
                        AbstractBlockManager.this.immutableBlockStates[index] = state;
                        AbstractBlockManager.this.blockStateMappings[internalId] = appearanceId;
                        AbstractBlockManager.this.appearanceToRealState.computeIfAbsent(appearanceId, k -> new IntArrayList()).add(internalId);
                        AbstractBlockManager.this.tempVisualBlockStatesInUse.add(visualState);
                        AbstractBlockManager.this.tempVisualBlocksInUse.add(getBlockOwnerId(visualState));
                        AbstractBlockManager.this.applyPlatformSettings(customBlock, state);
                        // generate mod assets
                        if (Config.generateModAssets()) {
                            AbstractBlockManager.this.modBlockStateOverrides.put(BlockManager.createCustomBlockKey(index), Optional.ofNullable(AbstractBlockManager.this.tempVanillaBlockStateModels[appearanceId])
                                    .orElseGet(() -> {
                                        // 如果未指定模型，说明复用原版模型？但是部分模型是多部位模型，无法使用变体解决问题
                                        // 未来需要靠mod重构彻底解决问题
                                        JsonObject json = new JsonObject();
                                        json.addProperty("model", "minecraft:block/air");
                                        return json;
                                    }));
                        }
                    }

                    // 一定要到最后再绑定
                    customBlock.setBehavior(blockBehavior);
                    holder.bindValue(customBlock);

                    // 添加方块
                    AbstractBlockManager.this.byId.put(customBlock.id(), customBlock);

                    // 抛出次要警告
                    eCollector2.throwIfPresent();
                }, () -> GsonHelper.get().toJson(section)));

                // 抛出次要警告
                eCollector1.throwIfPresent();
            }, () -> GsonHelper.get().toJson(section)));
        }

        private CullingData parseCullingData(Object arguments) {
            if (arguments instanceof Boolean b && !b) return null;
            if (!(arguments instanceof Map)) return new CullingData(DEFAULT_BLOCK_ENTITY_AABB, Config.entityCullingViewDistance(), 0.5, true);
            Map<String, Object> argumentsMap = ResourceConfigUtils.getAsMap(arguments, "entity-culling");
            return new CullingData(
                    ResourceConfigUtils.getAsAABB(argumentsMap.getOrDefault("aabb", 1), "aabb"),
                    ResourceConfigUtils.getAsInt(argumentsMap.getOrDefault("view-distance", Config.entityCullingViewDistance()), "view-distance"),
                    ResourceConfigUtils.getAsDouble(argumentsMap.getOrDefault("aabb-expansion", 0.5), "aabb-expansion"),
                    ResourceConfigUtils.getAsBoolean(argumentsMap.getOrDefault("ray-tracing", true), "ray-tracing")
            );
        }

        @SuppressWarnings("unchecked")
        private Optional<BlockEntityElementConfig<? extends BlockEntityElement>[]> parseBlockEntityRender(Object arguments) {
            if (arguments == null) return Optional.empty();
            List<BlockEntityElementConfig<? extends BlockEntityElement>> blockEntityElementConfigs = ResourceConfigUtils.parseConfigAsList(arguments, BlockEntityElementConfigs::fromMap);
            if (blockEntityElementConfigs.isEmpty()) return Optional.empty();
            return Optional.of(blockEntityElementConfigs.toArray(new BlockEntityElementConfig[0]));
        }

        @NotNull
        private Map<String, Property<?>> parseBlockProperties(Map<String, Object> propertiesSection) {
            Map<String, Property<?>> properties = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : propertiesSection.entrySet()) {
                Property<?> property = Properties.fromMap(entry.getKey(), ResourceConfigUtils.getAsMap(entry.getValue(), entry.getKey()));
                properties.put(entry.getKey(), property);
            }
            return properties;
        }

        @Nullable
        private JsonElement parseBlockModel(Object modelOrModels) {
            if (modelOrModels == null) return null;
            List<JsonObject> variants;
            if (modelOrModels instanceof String model) {
                variants = Collections.singletonList(MiscUtils.init(new JsonObject(), j -> j.addProperty("model", model)));
            } else {
                variants = ResourceConfigUtils.parseConfigAsList(modelOrModels, this::parseAppearanceModelSectionAsJson);
            }
            return variants.isEmpty() ? null : GsonHelper.combine(variants);
        }

        private void arrangeModelForStateAndVerify(BlockStateWrapper blockStateWrapper, JsonElement variant) {
            if (variant == null) return;
            // 拆分方块id与属性
            String blockState = blockStateWrapper.getAsString();
            int firstIndex = blockState.indexOf('[');
            Key blockId = firstIndex == -1 ? Key.of(blockState) : Key.of(blockState.substring(0, firstIndex));
            String propertyNBT = firstIndex == -1 ? "" : blockState.substring(firstIndex + 1, blockState.lastIndexOf(']'));

            Map<String, JsonElement> overrideMap = AbstractBlockManager.this.blockStateOverrides.computeIfAbsent(blockId, k -> new HashMap<>());
            JsonElement previous = overrideMap.get(propertyNBT);
            if (previous != null && !previous.equals(variant)) {
                throw new LocalizedResourceConfigException("warning.config.block.state.model.conflict", GsonHelper.get().toJson(variant), blockState, GsonHelper.get().toJson(previous));
            }
            overrideMap.put(propertyNBT, variant);
            AbstractBlockManager.this.tempVanillaBlockStateModels[blockStateWrapper.registryId()] = variant;
        }

        private JsonObject parseAppearanceModelSectionAsJson(Map<String, Object> section) {
            JsonObject json = new JsonObject();
            String modelPath = ResourceConfigUtils.requireNonEmptyStringOrThrow(section.get("path"), "warning.config.block.state.model.missing_path");
            if (!ResourceLocation.isValid(modelPath)) {
                throw new LocalizedResourceConfigException("warning.config.block.state.model.invalid_path", modelPath);
            }
            json.addProperty("model", modelPath);
            if (section.containsKey("x"))
                json.addProperty("x", ResourceConfigUtils.getAsInt(section.get("x"), "x"));
            if (section.containsKey("y"))
                json.addProperty("y", ResourceConfigUtils.getAsInt(section.get("y"), "y"));
            if (section.containsKey("z"))
                json.addProperty("z", ResourceConfigUtils.getAsInt(section.get("z"), "z"));
            if (section.containsKey("uvlock"))
                json.addProperty("uvlock", ResourceConfigUtils.getAsBoolean(section.get("uvlock"), "uvlock"));
            if (section.containsKey("weight"))
                json.addProperty("weight", ResourceConfigUtils.getAsInt(section.get("weight"), "weight"));
            Map<String, Object> generationMap = MiscUtils.castToMap(section.get("generation"), true);
            if (generationMap != null) {
                prepareModelGeneration(ModelGeneration.of(Key.of(modelPath), generationMap));
            }
            return json;
        }

        // 从方块外观的state里获取其原版方块的state id
        private BlockStateWrapper parsePluginFormattedBlockState(String blockState) {
            // 五种合理情况
            // minecraft:note_block:10
            // note_block:10
            // minecraft:note_block[xxx=xxx]
            // note_block[xxx=xxx]
            // minecraft:barrier
            String[] split = blockState.split(":", 3);
            if (split.length >= 4) {
                throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", blockState);
            }
            BlockStateWrapper wrapper;
            String stateOrId = split[split.length - 1];
            boolean isId = false;
            int arrangerIndex = 0;
            try {
                arrangerIndex = Integer.parseInt(stateOrId);
                if (arrangerIndex < 0) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", blockState);
                }
                isId = true;
            } catch (NumberFormatException ignored) {
            }
            // 如果末尾是id，则至少长度为2
            if (isId) {
                if (split.length == 1) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", blockState);
                }
                // 获取原版方块的id
                Key block = split.length == 2 ? Key.of(split[0]) : Key.of(split[0], split[1]);
                try {
                    List<BlockStateWrapper> arranger = AbstractBlockManager.this.blockStateArranger.get(block);
                    if (arranger == null) {
                        throw new LocalizedResourceConfigException("warning.config.block.state.unavailable_vanilla", blockState);
                    }
                    if (arrangerIndex >= arranger.size()) {
                        throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla_id", blockState, String.valueOf(arranger.size() - 1));
                    }
                    wrapper = arranger.get(arrangerIndex);
                } catch (NumberFormatException e) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", e, blockState);
                }
            } else {
                // 其他情况则是完整的方块
                BlockStateWrapper packedBlockState = createBlockState(blockState);
                if (packedBlockState == null) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", blockState);
                }
                wrapper = packedBlockState;
            }
            return wrapper;
        }
    }

    public boolean isVanillaBlockState(int id) {
        return id < this.vanillaBlockStateCount && id >= 0;
    }

    public BlockParser blockParser() {
        return blockParser;
    }

    public BlockStateMappingParser blockStateMappingParser() {
        return blockStateMappingParser;
    }
}
