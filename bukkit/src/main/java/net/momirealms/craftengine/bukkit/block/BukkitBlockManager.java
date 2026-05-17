package net.momirealms.craftengine.bukkit.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.bukkit.block.behavior.*;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.BlockGenerator;
import net.momirealms.craftengine.bukkit.plugin.injector.MaterialInjector;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.setting.BlockSettings;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.sound.SoundSet;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ObjectHolder;
import net.momirealms.craftengine.core.util.Tristate;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.util.CraftMagicNumbersProxy;
import net.momirealms.craftengine.proxy.minecraft.commands.arguments.blocks.BlockStateParserProxy;
import net.momirealms.craftengine.proxy.minecraft.core.*;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.EmptyBlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.FireBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateDefinitionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.NoteBlockInstrumentProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.MapColorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.PushReactionProxy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitBlockManager extends AbstractBlockManager {
    public static final Set<Object> CLIENT_SIDE_NOTE_BLOCKS = new HashSet<>(2048, 0.6f);
    private static final Object ALWAYS_FALSE = FastNMS.INSTANCE.createAlwaysStatePredicate(false);
    private static final Object ALWAYS_TRUE = FastNMS.INSTANCE.createAlwaysStatePredicate(true);
    private static BukkitBlockManager instance;
    private final BukkitCraftEngine plugin;
    // 事件监听器
    private final BlockEventListener blockEventListener;
    // 用于缓存string形式的方块状态到原版方块状态
    private final Map<String, BlockStateWrapper> blockStateCache = new ConcurrentHashMap<>(1024);
    // 用于临时存储可燃烧自定义方块的列表
    private final List<DelegatingBlock> burnableBlocks = Collections.synchronizedList(new ArrayList<>(32));
    // 可燃烧的方块
    private Map<Object, Integer> igniteOdds;
    private Map<Object, Integer> burnOdds;
    // 自定义客户端侧原版方块标签
    private Map<Integer, List<Key>> clientBoundTags = Map.of();
    private Map<Integer, List<Key>> previousClientBoundTags = Map.of();
    // 缓存的原版方块tag包
    private List<TagUtils.TagEntry> cachedUpdateTags = List.of();
    // 被移除声音的原版方块
    private Set<Object> missingPlaceSounds = Set.of();
    private Set<Object> missingBreakSounds = Set.of();
    private Set<Object> missingHitSounds = Set.of();
    private Set<Object> missingStepSounds = Set.of();
    private Set<Key> missingInteractSoundBlocks = Set.of();

    public BukkitBlockManager(BukkitCraftEngine plugin) {
        super(plugin, RegistryUtils.currentBlockRegistrySize(), Config.serverSideBlocks());
        this.plugin = plugin;
        this.blockEventListener = new BlockEventListener(plugin, this);
        this.registerServerSideCustomBlocks(Config.serverSideBlocks());
        EmptyBlockDefinition.init();
        EmptyBlockDefinition.STATE.setBehavior(EmptyBlockBehavior.INSTANCE);
        instance = this;
    }

    @Override
    public void init() {
        super.init();
        this.initMirrorRegistry();
        this.initFireBlock();
        this.deceiveBukkitRegistry();
        this.markVanillaNoteBlocks();
        this.findViewBlockingVanillaBlocks();
        Arrays.fill(this.immutableBlockStates, EmptyBlockDefinition.INSTANCE.defaultState());
        this.registerBlockStatePacketListener(); // 一定要预先初始化一次，预防id超出上限
    }

    public static BukkitBlockManager instance() {
        return instance;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.blockEventListener, this.plugin.javaPlugin());
    }

    @Override
    public void unload() {
        super.unload();
        this.previousClientBoundTags = this.clientBoundTags;
        this.clientBoundTags = new HashMap<>();
        for (DelegatingBlock block : this.burnableBlocks) {
            this.igniteOdds.remove(block);
            this.burnOdds.remove(block);
        }
        this.burnableBlocks.clear();
        if (EmptyBlockDefinition.STATE != null)
            Arrays.fill(this.immutableBlockStates, EmptyBlockDefinition.STATE);
        for (DelegatingBlock block : this.customBlocks) {
            block.behaviorDelegate().bindValue(EmptyBlockBehavior.INSTANCE);
            block.shapeDelegate().bindValue(BukkitBlockShape.STONE);
            DelegatingBlockState state = (DelegatingBlockState) BlockProxy.INSTANCE.getDefaultBlockState(block);
            state.setBlockState(null);
            state.setBlockOwner(null);
        }
    }

    @Override
    public void disable() {
        this.unload();
        HandlerList.unregisterAll(this.blockEventListener);
    }

    @Override
    public void delayedLoad() {
        this.registerBlockStatePacketListener();
        super.delayedLoad();
    }

    @Override
    public void registerBlockStatePacketListener() {
        this.plugin.networkManager().registerBlockStatePacketListeners(this.blockStateMappings, this::isViewBlockingBlock); // 重置方块映射表
    }

    @Override
    public BlockBehavior createFallbackBehavior(BlockDefinition definition) {
        return new BukkitBlockBehavior(definition);
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlockBehavior createBlockBehavior(BlockDefinition blockDefinition, ConfigValue behaviorValue) {
        List<BlockBehavior> behaviors = new ArrayList<>(4);
        // 硬编码旋转镜像行为
        for (Property<?> property : blockDefinition.properties()) {
            String propertyName = property.name();
            var behaviorGenerator = HardcodedPropertyBlockBehavior.HARD_CODED_PROPERTY_DATA.get(propertyName);

            if (behaviorGenerator != null) {
                HardcodedPropertyBlockBehavior behavior = behaviorGenerator.apply(blockDefinition, property);
                if (behavior != null) {
                    behaviors.add(behavior);
                }
            }
        }
        // 用户自己设置的行为
        if (behaviorValue != null) {
           behaviors.addAll(behaviorValue.getAsList(v -> BlockBehaviors.fromConfig(blockDefinition, v.getAsSection())));
        }
        // 硬编码waterlogged行为
        Property<?> waterloggedProperty = blockDefinition.getProperty("waterlogged");
        if (waterloggedProperty != null && waterloggedProperty.valueClass() == Boolean.class) {
            behaviors.add(new WaterloggedBlockBehavior(blockDefinition, (Property<Boolean>) waterloggedProperty));
        }
        switch (behaviors.size()) {
            case 0 -> {
                return new BukkitBlockBehavior(blockDefinition);
            }
            case 1 -> {
                return behaviors.getFirst();
            }
            case 2 -> {
                return new DualBlockBehavior(blockDefinition, behaviors.get(0), behaviors.get(1));
            }
            default -> {
                return new CompositeBlockBehavior(blockDefinition, behaviors);
            }
        }
    }

    @Override
    protected void updateTags() {
        // if there's no change
        if (this.clientBoundTags.equals(this.previousClientBoundTags)) return;
        List<TagUtils.TagEntry> list = new ArrayList<>();
        for (Map.Entry<Integer, List<Key>> entry : this.clientBoundTags.entrySet()) {
            list.add(new TagUtils.TagEntry(entry.getKey(), entry.getValue()));
        }
        this.cachedUpdateTags = list;
    }

    @Nullable
    @Override
    public BlockStateWrapper createBlockState(String blockState) {
        ImmutableBlockState state = BlockStateParser.deserialize(blockState);
        if (state != null) {
            return state.customBlockState();
        }
        return createVanillaBlockState(blockState);
    }

    @Override
    public BlockStateWrapper createVanillaBlockState(String blockState) {
        return this.blockStateCache.computeIfAbsent(blockState, k -> {
            Object state = parseBlockState(k);
            if (state == null) return null;
            return BlockStateUtils.toBlockStateWrapper(state);
        });
    }

    @Nullable
    private Object parseBlockState(String state) {
        try {
            Object registryOrLookUp = BuiltInRegistriesProxy.BLOCK;
            if (!VersionHelper.isOrAbove1_21_2) {
                registryOrLookUp = RegistryProxy.INSTANCE.asLookup(registryOrLookUp);
            }
            Object result = BlockStateParserProxy.INSTANCE.parseForBlock(registryOrLookUp, state, false);
            return BlockStateParserProxy.BlockResultProxy.INSTANCE.getBlockState(result);
        } catch (Exception e) {
            Debugger.BLOCK.warn(() -> "Failed to create block state: " + state, e);
            return null;
        }
    }

    @Nullable
    public Object getMinecraftBlockHolder(int stateId) {
        return this.customBlockHolders[stateId - BlockStateUtils.vanillaBlockStateCount()];
    }

    @Override
    public Key getBlockOwnerId(BlockStateWrapper state) {
        return BlockStateUtils.getBlockOwnerIdFromState(state.minecraftState());
    }

    @Override
    protected Key getBlockOwnerId(int id) {
        return BlockStateUtils.getBlockOwnerIdFromState(BlockStateUtils.idToBlockState(id));
    }

    private void initFireBlock() {
        this.igniteOdds = Collections.synchronizedMap(FireBlockProxy.INSTANCE.getIgniteOdds(BlocksProxy.FIRE));
        this.burnOdds = Collections.synchronizedMap(FireBlockProxy.INSTANCE.getBurnOdds(BlocksProxy.FIRE));
    }

    @Override
    protected void applyPlatformSettings(BlockDefinition block, ImmutableBlockState state) {
        DelegatingBlockState nmsState = (DelegatingBlockState) state.customBlockState().minecraftState();
        nmsState.setBlockState(state);
        nmsState.setBlockOwner(BlockStateUtils.getBlockOwner(block.defaultState().customBlockState().minecraftState()));
        Object nmsVisualState = state.visualBlockState().minecraftState();

        BlockSettings settings = state.settings();
        try {
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setLightEmission(nmsState, settings.luminance());
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setIgnitedByLava(nmsState, settings.burnable());
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setDestroySpeed(nmsState, settings.hardness());
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setReplaceable(nmsState, settings.replaceable());
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setMapColor(nmsState, MapColorProxy.INSTANCE.byId(settings.mapColor().id));
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setInstrument(nmsState, NoteBlockInstrumentProxy.VALUES[settings.instrument().ordinal()]);
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setPushReaction(nmsState, PushReactionProxy.VALUES[settings.pushReaction().ordinal()]);
            boolean canOcclude = settings.canOcclude() == Tristate.UNDEFINED ? BlockStateUtils.isOcclude(nmsVisualState) : settings.canOcclude().asBoolean();
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setCanOcclude(nmsState, canOcclude);
            boolean useShapeForLightOcclusion = settings.useShapeForLightOcclusion() == Tristate.UNDEFINED
                    ? BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isUseShapeForLightOcclusion(nmsVisualState) : settings.useShapeForLightOcclusion().asBoolean();
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setUseShapeForLightOcclusion(nmsState, useShapeForLightOcclusion);
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setIsRedstoneConductor(nmsState, settings.isRedstoneConductor().asBoolean() ? ALWAYS_TRUE : ALWAYS_FALSE);

            boolean suffocating = settings.isSuffocating() == Tristate.UNDEFINED ? (canBlockView(state.visualBlockState())) : (settings.isSuffocating().asBoolean());
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setIsSuffocating(nmsState, suffocating ? ALWAYS_TRUE : ALWAYS_FALSE);
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setIsViewBlocking(
                    nmsState,
                    settings.isViewBlocking() == Tristate.UNDEFINED ?
                    (suffocating ? ALWAYS_TRUE : ALWAYS_FALSE) :
                    (settings.isViewBlocking().asBoolean() ? ALWAYS_TRUE : ALWAYS_FALSE)
            );

            DelegatingBlock nmsBlock = (DelegatingBlock) BlockStateUtils.getBlockOwner(nmsState);
            ObjectHolder<BlockShape> shapeHolder = nmsBlock.shapeDelegate();
            shapeHolder.bindValue(new BukkitBlockShape(nmsVisualState, Optional.ofNullable(state.settings().supportShapeBlockState()).map(it -> Objects.requireNonNull(createVanillaBlockState(it), "Illegal block state: " + it).minecraftState()).orElse(null)));
            ObjectHolder<BlockBehavior> behaviorHolder = nmsBlock.behaviorDelegate();
            behaviorHolder.bindValue(state.behavior());
            if (VersionHelper.isOrAbove1_21_2) {
                BlockBehaviourProxy.INSTANCE.setDescriptionId(nmsBlock, block.translationKey());
            } else {
                BlockProxy.INSTANCE.setDescriptionId(nmsBlock, block.translationKey());
            }

            BlockBehaviourProxy.INSTANCE.setExplosionResistance(nmsBlock, settings.resistance());
            BlockBehaviourProxy.INSTANCE.setFriction(nmsBlock, settings.friction());
            BlockBehaviourProxy.INSTANCE.setSpeedFactor(nmsBlock, settings.speedFactor());
            BlockBehaviourProxy.INSTANCE.setJumpFactor(nmsBlock, settings.jumpFactor());
            BlockBehaviourProxy.INSTANCE.setSoundType(nmsBlock, SoundUtils.toNMSSoundType(settings.sounds()));

            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.initCache(nmsState);
            boolean isConditionallyFullOpaque = canOcclude & useShapeForLightOcclusion;
            if (!VersionHelper.isOrAbove1_21_2) {
                BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setConditionallyFullOpaque(nmsState, isConditionallyFullOpaque);
            }

            if (VersionHelper.isOrAbove1_21_2) {
                int blockLight = settings.blockLight() != -1 ? settings.blockLight() : BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightDampening$0(nmsVisualState);
                BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setLightDampening(nmsState, blockLight);
                boolean propagatesSkylightDown = settings.propagatesSkylightDown() == Tristate.UNDEFINED ? BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isPropagatesSkylightDown(nmsVisualState) : settings.propagatesSkylightDown().asBoolean();
                BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setPropagatesSkylightDown(nmsState, propagatesSkylightDown);
            } else {
                Object cache = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getCache(nmsState);
                int blockLight = settings.blockLight() != -1 ? settings.blockLight() : BlockBehaviourProxy.BlockStateBaseProxy.CacheProxy.INSTANCE.getLightBlock(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getCache(nmsVisualState));
                BlockBehaviourProxy.BlockStateBaseProxy.CacheProxy.INSTANCE.setLightBlock(cache, blockLight);
                boolean propagatesSkylightDown = settings.propagatesSkylightDown() == Tristate.UNDEFINED ? BlockBehaviourProxy.BlockStateBaseProxy.CacheProxy.INSTANCE.propagatesSkylightDown(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getCache(nmsVisualState)) : settings.propagatesSkylightDown().asBoolean();
                BlockBehaviourProxy.BlockStateBaseProxy.CacheProxy.INSTANCE.setPropagatesSkylightDown(cache, propagatesSkylightDown);
                if (!isConditionallyFullOpaque) {
                    BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setOpacityIfCached(nmsState, blockLight);
                }
            }

            if (state.behavior() instanceof RandomTickBlock randomTickBlock) {
                BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setIsRandomlyTicking(nmsState, randomTickBlock.canRandomlyTick(state));
            } else {
                BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setIsRandomlyTicking(nmsState, settings.isRandomlyTicking());
            }
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.setFluidState(nmsState, settings.fluidState() ? FluidsProxy.WATER$defaultState : FluidsProxy.EMPTY$defaultState);

            Object holder = BukkitCraftEngine.instance().blockManager().getMinecraftBlockHolder(state.customBlockState().registryId());
            Set<Object> tags = new HashSet<>();
            for (Key tag : settings.tags()) {
                tags.add(TagKeyProxy.INSTANCE.create(RegistriesProxy.BLOCK, KeyUtils.toIdentifier(tag)));
            }
            HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, tags);
            if (settings.burnable()) {
                this.igniteOdds.put(nmsBlock, settings.burnChance());
                this.burnOdds.put(nmsBlock, settings.fireSpreadChance());
                this.burnableBlocks.add(nmsBlock);
            }

            Key vanillaBlockId = state.visualBlockState().ownerId();
            BlockGenerator.field$CraftEngineBlock$isNoteBlock().set(nmsBlock, vanillaBlockId.equals(BlockKeys.NOTE_BLOCK));
            BlockGenerator.field$CraftEngineBlock$isTripwire().set(nmsBlock, vanillaBlockId.equals(BlockKeys.TRIPWIRE));
            if (vanillaBlockId.equals(BlockKeys.BARRIER)) {
                state.setRestoreBlockState(createBlockState("minecraft:glass"));
            } else {
                state.setRestoreBlockState(state.visualBlockState());
            }
            // 根据客户端的状态决定其是否阻挡视线
            if (settings.isRaytraceBlocking()) {
                super.viewBlockingBlocks[state.customBlockState().registryId()] = true;
            } else {
                super.viewBlockingBlocks[state.customBlockState().registryId()] = canBlockView(state.visualBlockState());
            }
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to apply platform block settings for block state " + state, e);
        }
    }

    private void initMirrorRegistry() {
        int size = RegistryUtils.currentBlockRegistrySize();
        BlockStateWrapper[] states = new BlockStateWrapper[size];
        for (int i = 0; i < this.vanillaBlockStateCount; i++) {
            states[i] = new BukkitVanillaBlockStateWrapper(BlockStateUtils.idToBlockState(i), i);
        }
        for (int i = this.vanillaBlockStateCount; i < size; i++) {
            states[i] = new BukkitCustomBlockStateWrapper(BlockStateUtils.idToBlockState(i), i);
        }
        BlockRegistryMirror.init(states, states[BlockStateUtils.blockStateToId(BlocksProxy.STONE$defaultState)]);
    }

    // 注册服务端侧的真实方块
    private void registerServerSideCustomBlocks(int count) {
        // 这个会影响全局调色盘
        try {
            unfreezeRegistry();
            boolean injectBukkitMaterial = checkInjectBukkitMaterialAvailability();
            int length = injectBukkitMaterial ? Material.values().length : 0;
            Material[] newMaterial = injectBukkitMaterial ? Arrays.copyOf(Material.values(), length + count) : null;
            for (int i = 0; i < count; i++) {
                Key customBlockId = BlockManager.createCustomBlockKey(i);
                DelegatingBlock customBlock = BlockGenerator.generateBlock(customBlockId);
                this.customBlocks[i] = customBlock;
                Object identifier = KeyUtils.toIdentifier(customBlockId);
                Object blockHolder = RegistryProxy.INSTANCE.registerForHolder$1(BuiltInRegistriesProxy.BLOCK, identifier, customBlock);
                this.customBlockHolders[i] = blockHolder;
                HolderProxy.ReferenceProxy.INSTANCE.bindValue(blockHolder, customBlock);
                HolderProxy.ReferenceProxy.INSTANCE.setTags(blockHolder, Set.of());
                DelegatingBlockState newBlockState = (DelegatingBlockState) BlockProxy.INSTANCE.getDefaultBlockState(customBlock);
                this.customBlockStates[i] = newBlockState;
                IdMapperProxy.INSTANCE.add(BlockProxy.BLOCK_STATE_REGISTRY, newBlockState);
                if (injectBukkitMaterial) {
                    newMaterial[length + i] = MaterialInjector.createMaterial(customBlockId, length + i, customBlock);
                }
            }
            if (injectBukkitMaterial) {
                MaterialInjector.resetMaterial(newMaterial);
            }
        } finally {
            freezeRegistry();
        }
    }

    private static boolean checkInjectBukkitMaterialAvailability() {
        if (!Config.injectBukkitMaterial()) return false;
        try {
            MaterialInjector.check();
            return true;
        } catch (Throwable e) {
            Config.setInjectBukkitMaterial(false);
            CraftEngine.instance().logger().warn("Current environment does not support injecting Bukkit Material", e);
            return false;
        }
    }

    public List<TagUtils.TagEntry> cachedUpdateTags() {
        return this.cachedUpdateTags;
    }

    private void markVanillaNoteBlocks() {
        Object block = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.BLOCK, KeyUtils.toIdentifier(BlockKeys.NOTE_BLOCK));
        Object stateDefinition = BlockProxy.INSTANCE.getStateDefinition(block);
        ImmutableList<Object> states = StateDefinitionProxy.INSTANCE.getStates(stateDefinition);
        CLIENT_SIDE_NOTE_BLOCKS.addAll(states);
    }

    public boolean canBlockView(BlockStateWrapper wrapper) {
        Object blockState = wrapper.minecraftState();
        if (!BlockStateUtils.isOcclude(blockState)) {
            return false;
        }
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isCollisionShapeFullBlock(blockState, EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO);
    }

    private void findViewBlockingVanillaBlocks() {
        for (int i = 0; i < this.vanillaBlockStateCount; i++) {
            BlockStateWrapper blockState = BlockRegistryMirror.byId(i);
            if (canBlockView(blockState)) {
                this.viewBlockingBlocks[i] = true;
            }
        }
    }

    @Override
    protected void setVanillaBlockTags(Key id, List<Key> tags) {
        Object block = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.BLOCK, KeyUtils.toIdentifier(id));
        int blockId = IdMapProxy.INSTANCE.getId(BuiltInRegistriesProxy.BLOCK, block);
        if (blockId == -1) {
            throw new IllegalStateException("Block " + id + " not found");
        }
        this.clientBoundTags.put(blockId, tags);
    }

    public boolean isPlaceSoundMissing(Object sound) {
        return this.missingPlaceSounds.contains(sound);
    }

    public boolean isBreakSoundMissing(Object sound) {
        return this.missingBreakSounds.contains(sound);
    }

    public boolean isHitSoundMissing(Object sound) {
        return this.missingHitSounds.contains(sound);
    }

    public boolean isStepSoundMissing(Object sound) {
        return this.missingStepSounds.contains(sound);
    }

    public boolean isInteractSoundMissing(Key blockType) {
        return this.missingInteractSoundBlocks.contains(blockType);
    }

    private void unfreezeRegistry() {
        MappedRegistryProxy.INSTANCE.setFrozen(BuiltInRegistriesProxy.BLOCK, false);
        MappedRegistryProxy.INSTANCE.setUnregisteredIntrusiveHolders(BuiltInRegistriesProxy.BLOCK, new IdentityHashMap<>());
    }

    private void freezeRegistry() {
        MappedRegistryProxy.INSTANCE.setFrozen(BuiltInRegistriesProxy.BLOCK, true);
    }

    private void deceiveBukkitRegistry() {
        Set<String> invalid = new HashSet<>();
        for (int i = 0; i < this.customBlocks.length; i++) {
            DelegatingBlock customBlock = this.customBlocks[i];
            Material material;
            Key key = Config.deceiveBukkitMaterial(i);
            if (key != null) {
                String value = key.value();
                try {
                    material = Material.valueOf(value.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    if (invalid.add(value)) {
                        this.plugin.logger().warn("Cannot load 'deceive-bukkit-material'. '" + value + "' is an invalid bukkit material", e);
                    }
                    material = Material.BRICKS;
                }
                if (!material.isBlock()) {
                    if (invalid.add(value)) {
                        this.plugin.logger().warn("Cannot load 'deceive-bukkit-material'. '" + value + "' is an invalid bukkit block material");
                    }
                    material = Material.BRICKS;
                }
            } else {
                material = MaterialInjector.getByBlock(customBlock);
            }
            CraftMagicNumbersProxy.BLOCK_MATERIAL.put(customBlock, material);
        }
    }

    @Override
    protected boolean isVanillaBlock(Key id) {
        if (!id.namespace().equals("minecraft"))
            return false;
        if (id.value().equals("air"))
            return true;
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.BLOCK, KeyUtils.toIdentifier(id)) != BlocksProxy.AIR;
    }

    public boolean isBurnable(Object blockState) {
        Object blockOwner = BlockStateUtils.getBlockOwner(blockState);
        return this.igniteOdds.getOrDefault(blockOwner, 0) > 0;
    }

    @Override
    public int vanillaBlockStateCount() {
        return this.vanillaBlockStateCount;
    }

    @Override
    public int currentBlockRegistrySize() {
        return RegistryUtils.currentBlockRegistrySize();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void processSounds() {
        Set<Object> affectedBlockSoundTypes = new HashSet<>();
        for (BlockStateWrapper vanillaBlockState : super.tempVisualBlockStatesInUse) {
            affectedBlockSoundTypes.add(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(vanillaBlockState.minecraftState()));
        }

        Set<Object> placeSounds = new HashSet<>();
        Set<Object> breakSounds = new HashSet<>();
        Set<Object> stepSounds = new HashSet<>();
        Set<Object> hitSounds = new HashSet<>();

        for (Object soundType : affectedBlockSoundTypes) {
            placeSounds.add(SoundEventProxy.INSTANCE.getLocation(SoundTypeProxy.INSTANCE.getPlaceSound(soundType)));
            breakSounds.add(SoundEventProxy.INSTANCE.getLocation(SoundTypeProxy.INSTANCE.getBreakSound(soundType)));
            stepSounds.add(SoundEventProxy.INSTANCE.getLocation(SoundTypeProxy.INSTANCE.getStepSound(soundType)));
            hitSounds.add(SoundEventProxy.INSTANCE.getLocation(SoundTypeProxy.INSTANCE.getHitSound(soundType)));
        }

        ImmutableMap.Builder<Key, Key> soundReplacementBuilder = ImmutableMap.builder();
        for (Object soundId : placeSounds) {
            Key previousId = KeyUtils.identifierToKey(soundId);
            soundReplacementBuilder.put(previousId, Key.of(previousId.namespace(), "replaced." + previousId.value()));
        }
        for (Object soundId : breakSounds) {
            Key previousId = KeyUtils.identifierToKey(soundId);
            soundReplacementBuilder.put(previousId, Key.of(previousId.namespace(), "replaced." + previousId.value()));
        }
        for (Object soundId : stepSounds) {
            Key previousId = KeyUtils.identifierToKey(soundId);
            soundReplacementBuilder.put(previousId, Key.of(previousId.namespace(), "replaced." + previousId.value()));
        }
        for (Object soundId : hitSounds) {
            Key previousId = KeyUtils.identifierToKey(soundId);
            soundReplacementBuilder.put(previousId, Key.of(previousId.namespace(), "replaced." + previousId.value()));
        }

        this.missingPlaceSounds = placeSounds;
        this.missingBreakSounds = breakSounds;
        this.missingHitSounds = hitSounds;
        this.missingStepSounds = stepSounds;

        Set<Key> missingInteractSoundBlocks = new HashSet<>();

        for (SoundSet soundSet : SoundSet.getAllSoundSets()) {
            for (Key block : soundSet.blocks()) {
                if (super.tempVisualBlocksInUse.contains(block)) {
                    Key openSound = soundSet.openSound();
                    soundReplacementBuilder.put(openSound, Key.of(openSound.namespace(), "replaced." + openSound.value()));
                    Key closeSound = soundSet.closeSound();
                    soundReplacementBuilder.put(closeSound, Key.of(closeSound.namespace(), "replaced." + closeSound.value()));
                    missingInteractSoundBlocks.addAll(soundSet.blocks());
                    break;
                }
            }
        }

        this.missingInteractSoundBlocks = missingInteractSoundBlocks;
        this.soundReplacements = soundReplacementBuilder.buildKeepingLast();
    }

    @Override
    protected BlockDefinition createCustomBlock(@NotNull Holder.Reference<BlockDefinition> holder,
                                                @NotNull BlockStateVariantProvider variantProvider,
                                                @NotNull Map<EventTrigger, List<Function<Context>>> events,
                                                @Nullable Loot loot) {
        return new BukkitBlockDefinition(holder, variantProvider, events, loot);
    }
}
