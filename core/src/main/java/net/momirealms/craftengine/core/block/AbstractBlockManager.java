package net.momirealms.craftengine.core.block;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigs;
import net.momirealms.craftengine.core.block.entity.render.element.ConstantBlockEntityElement;
import net.momirealms.craftengine.core.block.parser.BlockNbtParser;
import net.momirealms.craftengine.core.block.property.Properties;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.setting.BlockSettings;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.allocator.BlockStateCandidate;
import net.momirealms.craftengine.core.pack.allocator.IdAllocator;
import net.momirealms.craftengine.core.pack.allocator.VisualBlockStateAllocator;
import net.momirealms.craftengine.core.pack.model.generation.AbstractModelGenerator;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.network.mod.ModPackets;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.VisualBlockStatePacket;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public abstract class AbstractBlockManager extends AbstractModelGenerator implements BlockManager {
    private static final JsonElement EMPTY_VARIANT_MODEL = MiscUtils.init(new JsonObject(), o -> o.addProperty("model", "minecraft:block/empty"));
    private static final AABB DEFAULT_BLOCK_ENTITY_AABB = new AABB(-.5, -.5, -.5, .5, .5, .5);
    protected final IdSectionConfigParser blockParser = new BlockParser();
    protected final SectionConfigParser blockStateMappingParser;
    // 根据id获取自定义方块
    protected final Map<Key, BlockDefinition> byId = new ConcurrentHashMap<>(128, 0.5f);
    // 缓存的指令建议
    protected final List<Suggestion> cachedSuggestions = new ArrayList<>(128);
    // 缓存的使用中的命名空间
    protected final Set<String> namespacesInUse = new HashSet<>();
    // Map<方块类型, Map<方块状态NBT,模型>>，用于生成block state json
    protected final Map<Key, Map<String, JsonElement>> blockStateOverrides = new ConcurrentHashMap<>(128);
    // 用于生成mod使用的block state json
    protected final Map<Integer, JsonElement> modBlockStateOverrides = new ConcurrentHashMap<>(128);
    // 根据外观查找真实状态，用于debug指令
    protected final Map<Integer, List<Integer>> appearanceToRealState = Collections.synchronizedMap(new Int2ObjectOpenHashMap<>());
    // 用于note_block:0这样格式的自动分配
    protected final Map<Key, List<BlockStateWrapper>> blockStateArranger = new ConcurrentHashMap<>();
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
    // 用于检测单个外观方块状态是否被绑定了不同模型，同时也帮助生成mod资源包
    protected final JsonElement[] tempVanillaBlockStateModels;
    // 临时存储哪些视觉方块被使用了
    protected final Set<BlockStateWrapper> tempVisualBlockStatesInUse = ConcurrentHashMap.newKeySet(128);
    protected final Set<Key> tempVisualBlocksInUse = ConcurrentHashMap.newKeySet(128);
    // 能遮挡视线的方块
    protected final boolean[] viewBlockingBlocks;
    // 声音映射表，和使用了哪些视觉方块有关
    protected Map<Key, Key> soundReplacements = Map.of();
    // 是否使用了透明方块模型
    protected boolean isTransparentModelInUse = false;
    // 自动分配
    protected final IdAllocator internalIdAllocator;
    protected final VisualBlockStateAllocator visualBlockStateAllocator;
    // 缓存的VisualBlockStatePacket
    private VisualBlockStatePacket cachedVisualBlockStatePacket;

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
        this.blockStateMappingParser = new BlockStateMappingParser();
        this.viewBlockingBlocks = new boolean[vanillaBlockStateCount + customBlockCount];
        this.internalIdAllocator = new IdAllocator(AbstractBlockManager.this.plugin.dataFolderPath().resolve("cache").resolve("custom_block_states.json"));
        this.visualBlockStateAllocator = new VisualBlockStateAllocator(AbstractBlockManager.this.plugin.dataFolderPath().resolve("cache").resolve("visual_block_states.json"), this.autoVisualBlockStateCandidates, AbstractBlockManager.this::createVanillaBlockState);
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
        Arrays.fill(this.immutableBlockStates, EmptyBlockDefinition.STATE);
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
        this.cachedVisualBlockStatePacket = VisualBlockStatePacket.create();
        for (Player player : CraftEngine.instance().networkManager().onlineUsers()) {
            if (!player.clientModEnabled()) continue;
            ModPackets.sendPacket(player, this.cachedVisualBlockStatePacket);
        }
    }

    @Override
    public VisualBlockStatePacket cachedVisualBlockStatePacket() {
        return this.cachedVisualBlockStatePacket;
    }

    @Override
    public boolean isTransparentModelInUse() {
        return this.isTransparentModelInUse;
    }

    @Override
    public Map<Key, BlockDefinition> loadedBlocks() {
        return Collections.unmodifiableMap(this.byId);
    }

    @Override
    public Optional<BlockDefinition> blockById(Key id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    public Map<Key, List<BlockStateWrapper>> blockStateArranger() {
        return this.blockStateArranger;
    }

    protected abstract void applyPlatformSettings(BlockDefinition block, ImmutableBlockState state);

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[]{this.blockParser, this.blockStateMappingParser};
    }

    @Override
    public Map<Integer, JsonElement> modBlockStates() {
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
        for (BlockDefinition block : this.byId.values()) {
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

    public abstract void registerBlockStatePacketListener();

    public abstract BlockBehavior createFallbackBehavior(BlockDefinition definition);

    public abstract BlockBehavior createBlockBehavior(BlockDefinition blockDefinition, ConfigValue value);

    public boolean isViewBlockingBlock(int stateId) {
        return this.viewBlockingBlocks[stateId];
    }

    protected abstract void updateTags();

    protected abstract boolean isVanillaBlock(Key id);

    protected abstract Key getBlockOwnerId(int id);

    protected abstract void setVanillaBlockTags(Key id, List<Key> tags);

    protected abstract void processSounds();

    protected abstract BlockDefinition createCustomBlock(@NotNull Holder.Reference<BlockDefinition> holder,
                                                         @NotNull BlockStateVariantProvider variantProvider,
                                                         @NotNull Map<EventTrigger, List<Function<Context>>> events,
                                                         @Nullable Loot loot);

    private final class BlockStateMappingParser extends SectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[]{"block-state-mappings", "block-state-mapping", "block_state_mappings", "block_state_mapping"};
        private int count;

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
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
        public LoadingStage loadingStage() {
            return LoadingStages.BLOCK_STATE_MAPPING;
        }

        @Override
        public void parseSection(Pack pack, Path path, ConfigSection section) {
            for (String before : section.keySet()) {
                String after = section.getNonEmptyString(before);
                BlockStateWrapper beforeState = createVanillaBlockState(before);
                BlockStateWrapper afterState = createVanillaBlockState(after);
                if (beforeState == null) {
                    error(new KnownResourceException(path, "resource.argument.parser.blockstate", section.path(), before));
                    continue;
                }
                if (afterState == null) {
                    error(new KnownResourceException(path, "resource.argument.parser.blockstate", section.assemblePath(before), after));
                    continue;
                }
                int previous = AbstractBlockManager.this.blockStateMappings[beforeState.registryId()];
                if (previous != -1 && previous != afterState.registryId()) {
                    error(new KnownResourceException(path, "resource.mapping.conflict", section.assemblePath(before), before, after, BlockRegistryMirror.byId(previous).toString()));
                    continue;
                }
                AbstractBlockManager.this.blockStateMappings[beforeState.registryId()] = afterState.registryId();
                Key blockOwnerId = getBlockOwnerId(beforeState);
                List<BlockStateWrapper> blockStateWrappers = AbstractBlockManager.this.blockStateArranger.computeIfAbsent(blockOwnerId, k -> new ArrayList<>());
                blockStateWrappers.add(beforeState);
                AbstractBlockManager.this.autoVisualBlockStateCandidates[beforeState.registryId()] = createVisualBlockCandidate(beforeState);
                this.count++;
            }
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

    private final class BlockParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[]{"blocks", "block"};

        @Override
        public int count() {
            return AbstractBlockManager.this.byId.size();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.BLOCK;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.BLOCK_STATE_MAPPING, LoadingStages.ITEM);
        }

        @Override
        public void postProcess() {
            AbstractBlockManager.this.internalIdAllocator.processPendingAllocations();
            for (CompletableFuture<?> future : AbstractBlockManager.this.internalIdAllocator.combinedFutures()) {
                try {
                    future.join();
                } catch (CompletionException e) {
                    if (e.getCause() instanceof IdAllocator.IdExhaustedException) {
                        continue;
                    }
                    AbstractBlockManager.this.plugin.logger().warn("Error while assigning internal block states", e);
                }
            }
            try {
                AbstractBlockManager.this.internalIdAllocator.saveToCache();
            } catch (IOException e) {
                AbstractBlockManager.this.plugin.logger().warn("Error while saving custom block states allocation", e);
            }
            AbstractBlockManager.this.visualBlockStateAllocator.processPendingAllocations();
            for (CompletableFuture<?> future : AbstractBlockManager.this.visualBlockStateAllocator.combinedFutures()) {
                try {
                    future.join();
                } catch (CompletionException e) {
                    if (e.getCause() instanceof VisualBlockStateAllocator.StateExhaustedException) {
                        continue;
                    }
                    AbstractBlockManager.this.plugin.logger().warn("Error while assigning visual block states", e);
                }
            }
            try {
                AbstractBlockManager.this.visualBlockStateAllocator.saveToCache();
            } catch (IOException e) {
                AbstractBlockManager.this.plugin.logger().warn("Error while saving visual block states allocation", e);
            }
        }

        @SuppressWarnings("DuplicatedCode")
        @Override
        public void preProcess() {
            AbstractBlockManager.this.internalIdAllocator.reset(0, Config.serverSideBlocks() - 1);
            AbstractBlockManager.this.visualBlockStateAllocator.reset();
            try {
                AbstractBlockManager.this.visualBlockStateAllocator.loadFromCache();
            } catch (IOException e) {
                AbstractBlockManager.this.plugin.logger().warn("Error while loading visual block states allocation cache", e);
            }
            try {
                AbstractBlockManager.this.internalIdAllocator.loadFromCache();
            } catch (IOException e) {
                AbstractBlockManager.this.plugin.logger().warn("Error while loading custom block states allocation cache", e);
            }
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            if (isVanillaBlock(id)) {
                parseVanillaBlock(id, section);
            } else {
                parseCustomBlock(path, id, section);
            }
        }

        private static final String[] CLIENT_BOUND_TAGS = new String[]{"client_bound_tags", "client-bound-tags"};

        private void parseVanillaBlock(Key id, ConfigSection section) {
            ConfigSection settingsSection = section.getSection("settings");
            if (settingsSection != null) {
                ConfigValue configValue = settingsSection.getValue(CLIENT_BOUND_TAGS);
                if (configValue != null) {
                    List<Key> tags = configValue.getAsList(ConfigValue::getAsIdentifier);
                    AbstractBlockManager.this.setVanillaBlockTags(id, tags);
                }
            }
        }

        private static final String[] STATE = new String[]{"state", "states"};
        private static final String[] EVENTS = new String[]{"events", "event"};
        private static final String[] AUTO_STATE = new String[]{"auto_state", "auto-state"};
        private static final String[] MODELS = new String[]{"model", "models"};
        private static final String[] ENTITY_RENDERER = new String[]{"entity_renderer", "entity-renderer", "entity_render", "entity-render"};
        private static final String[] ENTITY_CULLING = new String[]{"entity_culling", "entity-culling"};
        private static final String[] BEHAVIOR = new String[]{"behavior", "behaviors"};
        private static final String[] VIEW_DISTANCE = new String[]{"view_distance", "view-distance"};
        private static final String[] AABB_EXPANSION = new String[]{"aabb_expansion", "aabb-expansion"};
        private static final String[] RAY_TRACING = new String[]{"ray_tracing", "ray-tracing"};
        private static final String[] APPEARANCE = new String[]{"appearance", "appearances"};

        private void parseCustomBlock(Path path, Key id, ConfigSection section) {
            // 获取共享方块设置 （可异常）
            BlockSettings settings = BlockSettings.of().itemId(id);
            try {
                BlockSettings.applyModifiers(settings, section.getSection("settings"));
            } catch (KnownResourceException e) {
                error(e, path);
            }

            // 读取states区域
            ConfigSection stateSection = section.getNonNullSection(STATE);
            // 读取方块属性
            Map<String, Property<?>> properties = stateSection.getValue("properties", v -> parseBlockProperties(v.getAsSection()), Map.of());
            // 注册方块容器
            Holder.Reference<BlockDefinition> holder = ((WritableRegistry<BlockDefinition>) BuiltInRegistries.BLOCK).getOrRegisterForHolder(ResourceKey.create(BuiltInRegistries.BLOCK.key().location(), id));
            // 先绑定无效方块，防止因为后续报错导致未绑定
            holder.bindValue(new InactiveBlockDefinition(holder));
            // 根据properties生成variant provider
            BlockStateVariantProvider variantProvider = new BlockStateVariantProvider(holder, (owner, provider, propertyMap) -> {
                ImmutableBlockState blockState = new ImmutableBlockState(owner, provider, propertyMap);
                blockState.setSettings(settings);
                return blockState;
            }, properties);

            // 获取全部方块状态
            ImmutableList<ImmutableBlockState> states = variantProvider.states();
            List<CompletableFuture<Integer>> internalIdAllocators = new ArrayList<>(states.size());

            // 如果用户指定了起始id
            ConfigValue startingValue = stateSection.getValue("id");
            if (startingValue != null) {
                int startingId = startingValue.getAsInt();
                int endingId = startingId + states.size() - 1;
                if (startingId < 0) {
                    throw new KnownResourceException("number.no_less_than", startingValue.path(), "id", "0");
                }
                if (endingId >= Config.serverSideBlocks()) {
                    throw new KnownResourceException("number.less_than", startingValue.path(), "id", String.valueOf(Config.serverSideBlocks() - states.size() + 1));
                }
                // 先检测范围冲突
                List<Pair<String, Integer>> conflicts = AbstractBlockManager.this.internalIdAllocator.getFixedIdsBetween(startingId, endingId);
                if (!conflicts.isEmpty()) {
                    ExceptionCollector<KnownResourceException> exceptionCollector = new ExceptionCollector<>(KnownResourceException.class);
                    for (Pair<String, Integer> conflict : conflicts) {
                        int internalId = conflict.right();
                        int index = internalId - startingId;
                        exceptionCollector.add(new KnownResourceException(
                                "resource.block.state.real_state_conflict",
                                stateSection.path(),
                                states.get(index).toString(),
                                BlockManager.createCustomBlockKey(internalId).toString(),
                                conflict.left()
                        ));
                    }
                    exceptionCollector.throwIfPresent();
                }
                // 强行分配id
                for (ImmutableBlockState blockState : states) {
                    String blockStateId = blockState.toString();
                    internalIdAllocators.add(AbstractBlockManager.this.internalIdAllocator.assignFixedId(blockStateId, startingId++));
                }
            }
            // 未指定，则使用自动分配
            else {
                for (ImmutableBlockState blockState : states) {
                    String blockStateId = blockState.toString();
                    internalIdAllocators.add(AbstractBlockManager.this.internalIdAllocator.requestAutoId(blockStateId));
                }
            }

            // 等待完成真实id分配
            AbstractBlockManager.this.internalIdAllocator.addCombinedFuture(CompletableFutures.allOf(internalIdAllocators).whenCompleteAsync((v1, t1) -> ResourceConfigUtils.runCatching(path, section.path(), () -> {

                // 槽位用尽 （严重错误）
                if (t1 != null) {
                    if (t1 instanceof CompletionException e) {
                        Throwable cause = e.getCause();
                        // 这里不会有conflict了，因为之前已经判断过了
                        if (cause instanceof IdAllocator.IdExhaustedException) {
                            error(new KnownResourceException(path, "resource.block.state.real_state_exhausted", stateSection.path()));
                        }
                    }
                    return;
                }

                // 将自定义状态与nms状态绑定
                for (int i = 0; i < internalIdAllocators.size(); i++) {
                    CompletableFuture<Integer> future = internalIdAllocators.get(i);
                    try {
                        int internalId = future.get();
                        states.get(i).setCustomBlockState(BlockRegistryMirror.byId(internalId + AbstractBlockManager.this.vanillaBlockStateCount));
                    } catch (InterruptedException | ExecutionException e) {
                        // 严重错误
                        ThrowableUtils.sneakyThrow(e);
                    }
                }

                // 解析事件 （可异常）
                Map<EventTrigger, List<Function<Context>>> events = new EnumMap<>(EventTrigger.class);
                try {
                    CommonFunctions.parseEvents(section.getValue(EVENTS), (t, f) -> events.computeIfAbsent(t, k -> new ArrayList<>(4)).add(f));
                } catch (KnownResourceException e) {
                    error(e, path);
                }

                // 解析战利品表 （可异常）
                Loot loot = null;
                try {
                    loot = section.getValue("loot", ConfigValue::getAsLoot);
                } catch (KnownResourceException e) {
                    error(e, path);
                }

                // 创建自定义方块
                AbstractBlockDefinition customBlock = (AbstractBlockDefinition) createCustomBlock(holder, variantProvider, events, loot);

                // 读取外观设置
                Map<String, ConfigSection> appearanceConfigs;
                Map<String, CompletableFuture<BlockStateWrapper>> futureVisualStates = new HashMap<>();
                if (properties.isEmpty()) {
                    appearanceConfigs = Map.of("", stateSection);
                } else {
                    appearanceConfigs = new LinkedHashMap<>(4);
                    ConfigSection appearanceSection = stateSection.getNonNullSection(APPEARANCE);
                    for (String appearanceName : appearanceSection.keySet()) {
                        appearanceConfigs.put(appearanceName, appearanceSection.getNonNullSection(appearanceName));
                    }
                }

                // 解析外观设置，准备外观状态分配
                for (Map.Entry<String, ConfigSection> entry : appearanceConfigs.entrySet()) {
                    ConfigSection appearanceSection = entry.getValue();
                    String appearanceName = entry.getKey();
                    ConfigValue autoStateValue = appearanceSection.getValue(AUTO_STATE);
                    if (autoStateValue != null) {
                        AutoStateGroup group;
                        String cacheId;
                        if (autoStateValue.is(Map.class)) {
                            ConfigSection advancedAutoState = autoStateValue.getAsSection();
                            group = advancedAutoState.getValue("type", v -> v.getAsEnum(AutoStateGroup.class, AutoStateGroup::byId), AutoStateGroup.SOLID);
                            if (advancedAutoState.containsKey("id")) {
                                cacheId = group.id() + "[id=" + advancedAutoState.getString("id") + "]";
                            } else {
                                cacheId = appearanceName.isEmpty() ? id.asString() : id.asString() + "[appearance=" + appearanceName + "]";
                            }
                        } else {
                            group = autoStateValue.getAsEnum(AutoStateGroup.class, AutoStateGroup::byId);
                            cacheId = appearanceName.isEmpty() ? id.asString() : id.asString() + "[appearance=" + appearanceName + "]";
                        }
                        futureVisualStates.put(
                                appearanceName,
                                AbstractBlockManager.this.visualBlockStateAllocator.requestAutoState(cacheId, group)
                        );
                    } else {
                        ConfigValue stateValue = appearanceSection.getNonNullValue("state", ConfigConstants.ARGUMENT_BLOCK_STATE);
                        futureVisualStates.put(
                                appearanceName,
                                AbstractBlockManager.this.visualBlockStateAllocator.assignFixedBlockState(
                                        appearanceName.isEmpty() ? id.asString() : id.asString() + ":" + appearanceName,
                                        stateValue.getAsBlockState()
                                )
                        );
                    }
                }

                AbstractBlockManager.this.visualBlockStateAllocator.addCombinedFuture(CompletableFutures.allOf(futureVisualStates.values()).whenCompleteAsync((v2, t2) -> ResourceConfigUtils.runCatching(path, section.path(), () -> {
                    if (t2 != null) {
                        if (t2 instanceof CompletionException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof VisualBlockStateAllocator.StateExhaustedException exhausted) {
                                error(new KnownResourceException(path,
                                        "resource.block.state.visual_state_exhausted",
                                        stateSection.path(),
                                        exhausted.group().id(),
                                        String.valueOf(exhausted.group().candidateCount()),
                                        exhausted.appearance()
                                ));
                                return;
                            }
                        }
                        // 并不改变 future 结果
                        return;
                    }

                    BlockStateAppearance anyAppearance = null;
                    Map<String, BlockStateAppearance> appearances = new HashMap<>();
                    for (Map.Entry<String, ConfigSection> entry : appearanceConfigs.entrySet()) {
                        String appearanceName = entry.getKey();
                        BlockStateWrapper visualBlockState = null;
                        try {
                            visualBlockState = futureVisualStates.get(appearanceName).get();
                        } catch (InterruptedException | ExecutionException e) {
                            ThrowableUtils.sneakyThrow(e);
                        }
                        ConfigSection appearanceSection = entry.getValue();
                        if (appearanceSection.getBoolean("transparent")) {
                            AbstractBlockManager.this.isTransparentModelInUse = true;
                            this.arrangeModelForStateAndVerify(visualBlockState, EMPTY_VARIANT_MODEL, appearanceSection.path());
                        } else {
                            ConfigValue modelValue = appearanceSection.getValue(MODELS);
                            if (modelValue != null) {
                                this.arrangeModelForStateAndVerify(visualBlockState, parseBlockModel(modelValue), modelValue.path());
                            }
                        }
                        BlockStateAppearance blockStateAppearance = new BlockStateAppearance(
                                visualBlockState,
                                parseBlockEntityRender(appearanceSection.getValue(ENTITY_RENDERER)),
                                parseCullingData(section.getValue(ENTITY_CULLING))
                        );
                        appearances.put(appearanceName, blockStateAppearance);
                        if (anyAppearance == null) {
                            anyAppearance = blockStateAppearance;
                        }
                    }

                    // 不应该出现的情况
                    if (anyAppearance == null) {
                        throw new IllegalStateException("Cannot find any available appearance");
                    }

                    if (!properties.isEmpty()) {
                        ConfigSection variantsSection = stateSection.getSection("variants");
                        if (variantsSection != null) {
                            for (String nbt : variantsSection.keySet()) {
                                ConfigSection variantSection = variantsSection.getNonNullSection(nbt);

                                // 先解析nbt，找到需要修改的方块状态
                                CompoundTag tag = BlockNbtParser.deserialize(variantProvider, nbt);
                                if (tag == null) {
                                    error(new KnownResourceException(path, "resource.block.state.invalid_variant_nbt", variantsSection.path(), nbt));
                                    continue;
                                }
                                List<ImmutableBlockState> possibleStates = variantProvider.getPossibleStates(tag);

                                // 应用覆写的方块设置
                                ConfigSection settingsOverride = variantSection.getSection("settings");
                                if (settingsOverride != null) {
                                    for (ImmutableBlockState possibleState : possibleStates) {
                                        possibleState.setSettings(BlockSettings.ofFullCopyAndApply(possibleState.settings(), settingsOverride));
                                    }
                                }

                                // 绑定方块外观
                                String appearanceName = variantSection.getString(APPEARANCE);
                                if (appearanceName != null) {
                                    BlockStateAppearance appearance = appearances.get(appearanceName);
                                    if (appearance == null) {
                                        error(new KnownResourceException(path, "resource.block.state.undefined_appearance", variantSection.assemblePath("appearance"), appearanceName));
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

                    // 读取方块行为 （可异常）
                    BlockBehavior blockBehavior;
                    try {
                        blockBehavior = createBlockBehavior(customBlock, section.getValue(BEHAVIOR));
                    } catch (KnownResourceException e) {
                        error(e, path);
                        blockBehavior = createFallbackBehavior(customBlock);
                    }

                    // 获取方块实体行为
                    boolean isEntityBlock = blockBehavior.getFirst(EntityBlock.class) != null;
                    if (isEntityBlock && blockBehavior instanceof EntityBlock entityBlock) {
                        entityBlock.initControllerId(0);
                    }

                    // 绑定行为
                    for (ImmutableBlockState state : states) {

                        if (isEntityBlock) {
                            state.setHasBlockEntity();
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
                        AbstractBlockManager.this.appearanceToRealState.computeIfAbsent(appearanceId, k -> Collections.synchronizedList(new IntArrayList())).add(internalId);
                        AbstractBlockManager.this.tempVisualBlockStatesInUse.add(visualState);
                        AbstractBlockManager.this.tempVisualBlocksInUse.add(getBlockOwnerId(visualState));
                        AbstractBlockManager.this.applyPlatformSettings(customBlock, state);
                        // 生成 mod 资产
                        JsonElement model = AbstractBlockManager.this.tempVanillaBlockStateModels[appearanceId];
                        // 如果未指定模型，说明复用原版模型？但是插件目前无法得知其原版变体模型，且部分模型是多部位模型，无法使用变体解决问题
                        if (model == null) {
                            model = EMPTY_VARIANT_MODEL;
                            AbstractBlockManager.this.isTransparentModelInUse = true;
                        }
                        AbstractBlockManager.this.modBlockStateOverrides.put(index, model);
                    }

                    // 一定要到最后再绑定
                    customBlock.setBehavior(blockBehavior);
                    holder.bindValue(customBlock);

                    // 添加方块
                    AbstractBlockManager.this.byId.put(customBlock.id(), customBlock);
                }, super.errorHandler), AbstractBlockManager.this.plugin.scheduler().async()));

            }, super.errorHandler), AbstractBlockManager.this.plugin.scheduler().async()));
        }

        private CullingData parseCullingData(@Nullable ConfigValue value) {
            if (value != null) {
                if (value.is(Boolean.class) && !value.getAsBoolean()) {
                    return null;
                } else if (value.is(Map.class)) {
                    ConfigSection section = value.getAsSection();
                    return new CullingData(
                            section.getAABB("aabb", DEFAULT_BLOCK_ENTITY_AABB),
                            section.getInt(VIEW_DISTANCE, Config.entityCullingViewDistance()),
                            section.getDouble(AABB_EXPANSION, 0.5),
                            section.getBoolean(RAY_TRACING, true)
                    );
                }
            }
            return new CullingData(DEFAULT_BLOCK_ENTITY_AABB, Config.entityCullingViewDistance(), 0.5, true);
        }

        @SuppressWarnings("unchecked")
        private Optional<BlockEntityElementConfig<? extends ConstantBlockEntityElement>[]> parseBlockEntityRender(ConfigValue arguments) {
            if (arguments == null) return Optional.empty();
            List<BlockEntityElementConfig<ConstantBlockEntityElement>> configs = arguments.getAsList(v -> BlockEntityElementConfigs.fromConfig(v.getAsSection()));
            if (configs.isEmpty()) return Optional.empty();
            return Optional.of(configs.toArray(new BlockEntityElementConfig[0]));
        }

        @NotNull
        private Map<String, Property<?>> parseBlockProperties(ConfigSection section) {
            Map<String, Property<?>> properties = new LinkedHashMap<>();
            for (String propertyName : section.keySet()) {
                Property<?> property = Properties.fromConfig(propertyName, section.getNonNullSection(propertyName));
                properties.put(propertyName, property);
            }
            return properties;
        }

        @Nullable
        private JsonElement parseBlockModel(ConfigValue modelOrModels) {
            if (modelOrModels == null) return null;
            List<JsonObject> variants;
            if (modelOrModels.is(List.class)) {
                variants = modelOrModels.getAsNonEmptyList(v -> this.parseAppearanceModelSectionAsJson(v.getAsSection()));
            } else if (modelOrModels.is(Map.class)) {
                variants = List.of(this.parseAppearanceModelSectionAsJson(modelOrModels.getAsSection()));
            } else {
                variants = List.of(MiscUtils.init(new JsonObject(), j -> j.addProperty("model", modelOrModels.getAsAssetPath().asMinimalString())));
            }
            return variants.isEmpty() ? null : minimizeVariant(variants);
        }

        private JsonElement minimizeVariant(List<? extends JsonElement> jo) {
            if (jo.size() == 1) {
                return jo.getFirst();
            } else {
                JsonArray ja = new JsonArray(jo.size());
                for (JsonElement je : jo) {
                    ja.add(je);
                }
                return ja;
            }
        }

        private void arrangeModelForStateAndVerify(BlockStateWrapper blockStateWrapper, @Nullable JsonElement variant, String path) {
            if (variant == null) return;
            // 拆分方块id与属性
            String blockState = blockStateWrapper.getAsString();
            int firstIndex = blockState.indexOf('[');
            Key blockId = firstIndex == -1 ? Key.of(blockState) : Key.of(blockState.substring(0, firstIndex));
            String propertyNBT = firstIndex == -1 ? "" : blockState.substring(firstIndex + 1, blockState.lastIndexOf(']'));

            Map<String, JsonElement> overrideMap = AbstractBlockManager.this.blockStateOverrides.computeIfAbsent(blockId, k -> Collections.synchronizedMap(new HashMap<>()));
            JsonElement previous = overrideMap.get(propertyNBT);
            if (previous != null && !previous.equals(variant)) {
                throw new KnownResourceException("resource.block.state.model_conflict", path, GsonHelper.get().toJson(variant), blockState, GsonHelper.get().toJson(previous));
            }
            overrideMap.put(propertyNBT, variant);
            AbstractBlockManager.this.tempVanillaBlockStateModels[blockStateWrapper.registryId()] = variant;
        }

        private static final String[] PATH = new String[] {"path", "model"};

        private JsonObject parseAppearanceModelSectionAsJson(ConfigSection section) {
            JsonObject json = new JsonObject();
            Key modelPath = section.getNonNullIdentifier(PATH);
            json.addProperty("model", modelPath.asMinimalString());
            if (section.containsKey("x"))
                json.addProperty("x", section.getInt("x"));
            if (section.containsKey("y"))
                json.addProperty("y", section.getInt("y"));
            if (section.containsKey("z"))
                json.addProperty("z", section.getInt("z"));
            if (section.containsKey("uvlock"))
                json.addProperty("uvlock", section.getBoolean("uvlock"));
            if (section.containsKey("weight"))
                json.addProperty("weight", section.getInt("weight"));
            ConfigSection generationSection = section.getSection("generation");
            if (generationSection != null) {
                prepareModelGeneration(new ModelGenerationHolder(modelPath, ModelGeneration.of(generationSection)));
            }
            return json;
        }
    }

    public boolean isVanillaBlockState(int id) {
        return id < this.vanillaBlockStateCount && id >= 0;
    }

    public IdSectionConfigParser blockParser() {
        return this.blockParser;
    }

    public IdAllocator internalIdAllocator() {
        return this.internalIdAllocator;
    }

    public VisualBlockStateAllocator visualBlockStateAllocator() {
        return this.visualBlockStateAllocator;
    }
}
