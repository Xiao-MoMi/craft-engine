package net.momirealms.craftengine.core.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.core.block.properties.Properties;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.model.generation.AbstractModelGenerator;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.context.event.EventFunctions;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public abstract class AbstractBlockManager extends AbstractModelGenerator implements BlockManager {
    protected final BlockParser blockParser;
    // CraftEngine objects
    protected final Map<Key, CustomBlock> byId = new HashMap<>();
    // Cached command suggestions
    protected final List<Suggestion> cachedSuggestions = new ArrayList<>();
    // Cached Namespace
    protected final Set<String> namespacesInUse = new HashSet<>();
    // for mod, real block id -> state models
    protected final Map<Key, JsonElement> modBlockStates = new HashMap<>();
    // A temporary map that stores the model path of a certain vanilla block state
    protected final Map<Integer, JsonElement> tempVanillaBlockStateModels = new Int2ObjectOpenHashMap<>();
    // A temporary map used to detect whether the same block state corresponds to multiple models.
    protected final Map<Integer, Key> tempRegistryIdConflictMap = new Int2ObjectOpenHashMap<>();
    // A temporary map that converts the custom block registered on the server to the vanilla block ID.
    protected final Map<Integer, Integer> tempBlockAppearanceConvertor = new Int2IntOpenHashMap();
    // Used to store override information of json files
    protected final Map<Key, Map<String, JsonElement>> blockStateOverrides = new HashMap<>();
    // a reverted mapper
    protected final Map<Integer, List<Integer>> appearanceToRealState = new Int2ObjectOpenHashMap<>();

    // client side block tags
    protected Map<Integer, List<String>> clientBoundTags = Map.of();
    protected Map<Integer, List<String>> previousClientBoundTags = Map.of();
    // Used to automatically arrange block states for strings such as minecraft:note_block:0
    protected Map<Key, List<Integer>> blockAppearanceArranger;
    protected Map<Key, List<Integer>> realBlockArranger;
    protected Map<Key, Integer> internalId2StateId;
    protected Map<Key, DelegatingBlock> registeredBlocks;

    protected AbstractBlockManager(CraftEngine plugin) {
        super(plugin);
        this.blockParser = new BlockParser();
    }

    @Override
    public void unload() {
        super.clearModelsToGenerate();
        this.clearCache();
        this.cachedSuggestions.clear();
        this.blockStateOverrides.clear();
        this.modBlockStates.clear();
        this.byId.clear();
        this.previousClientBoundTags = this.clientBoundTags;
        this.clientBoundTags = new HashMap<>();
        this.appearanceToRealState.clear();
    }

    @Override
    public void delayedLoad() {
        this.initSuggestions();
        this.clearCache();
        this.resendTags();
    }

    @Override
    public Map<Key, CustomBlock> blocks() {
        return Collections.unmodifiableMap(this.byId);
    }

    @Override
    public Optional<CustomBlock> blockById(Key id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    protected void addBlockInternal(Key id, CustomBlock customBlock) {
        this.byId.put(id, customBlock);
        // generate mod assets
        if (Config.generateModAssets()) {
            for (ImmutableBlockState state : customBlock.variantProvider().states()) {
                this.modBlockStates.put(getBlockOwnerId(state.customBlockState()), this.tempVanillaBlockStateModels.get(state.vanillaBlockState().registryId()));
            }
        }
    }

    @Override
    public ConfigParser parser() {
        return this.blockParser;
    }

    @Override
    public Map<Key, JsonElement> modBlockStates() {
        return Collections.unmodifiableMap(this.modBlockStates);
    }

    @Override
    public Map<Key, Map<String, JsonElement>> blockOverrides() {
        return Collections.unmodifiableMap(this.blockStateOverrides);
    }

    @Override
    public Collection<Suggestion> cachedSuggestions() {
        return Collections.unmodifiableCollection(this.cachedSuggestions);
    }

    public Set<String> namespacesInUse() {
        return Collections.unmodifiableSet(this.namespacesInUse);
    }

    protected void clearCache() {
        this.tempRegistryIdConflictMap.clear();
        this.tempBlockAppearanceConvertor.clear();
        this.tempVanillaBlockStateModels.clear();
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

    protected abstract void resendTags();

    protected abstract boolean isVanillaBlock(Key id);

    protected abstract int getBlockRegistryId(Key id);

    protected abstract String stateRegistryIdToStateSNBT(int id);

    protected abstract Key getBlockOwnerId(int id);

    protected abstract CustomBlock.Builder platformBuilder(Key id);

    public class BlockParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[]{"blocks", "block"};

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
            if (isVanillaBlock(id)) {
                parseVanillaBlock(pack, path, id, section);
            } else {
                // check duplicated config
                if (AbstractBlockManager.this.byId.containsKey(id)) {
                    throw new LocalizedResourceConfigException("warning.config.block.duplicate");
                }
                parseCustomBlock(pack, path, id, section);
            }
        }

        private void parseVanillaBlock(Pack pack, Path path, Key id, Map<String, Object> section) {
            Map<String, Object> settings = MiscUtils.castToMap(section.get("settings"), true);
            if (settings != null) {
                Object clientBoundTags = settings.get("client-bound-tags");
                if (clientBoundTags instanceof List<?> list) {
                    List<String> clientSideTags = MiscUtils.getAsStringList(list).stream().filter(ResourceLocation::isValid).toList();
                    AbstractBlockManager.this.clientBoundTags.put(getBlockRegistryId(id), clientSideTags);
                }
            }
        }

        private void parseCustomBlock(Pack pack, Path path, Key id, Map<String, Object> section) {
            // 获取方块设置
            BlockSettings settings = BlockSettings.fromMap(id, MiscUtils.castToMap(section.get("settings"), true));
            // 读取基础外观配置
            Map<String, Property<?>> properties;
            Map<String, Integer> appearances;
            Map<String, BlockStateVariant> variants;
            // 读取states区域
            Map<String, Object> stateSection = MiscUtils.castToMap(ResourceConfigUtils.requireNonNullOrThrow(
                    ResourceConfigUtils.get(section, "state", "states"), "warning.config.block.missing_state"), true);
            boolean singleState = !stateSection.containsKey("properties");
            // 单方块状态
            if (singleState) {
                int internalId = ResourceConfigUtils.getAsInt(ResourceConfigUtils.requireNonNullOrThrow(
                        stateSection.get("id"), "warning.config.block.state.missing_real_id"), "id");
                // 获取原版外观的注册表id
                int appearanceId = pluginFormattedBlockStateToRegistryId(ResourceConfigUtils.requireNonEmptyStringOrThrow(
                        stateSection.get("state"), "warning.config.block.state.missing_state"));
                // 为原版外观赋予外观模型并检查模型冲突
                this.arrangeModelForStateAndVerify(appearanceId, ResourceConfigUtils.get(stateSection, "model", "models"));
                // 设置参数
                properties = Map.of();
                appearances = Map.of("", appearanceId);
                variants = Map.of("", new BlockStateVariant("", settings, getInternalBlockId(internalId, appearanceId)));
            }
            // 多方块状态
            else {
                properties = parseBlockProperties(ResourceConfigUtils.getAsMap(ResourceConfigUtils.requireNonNullOrThrow(stateSection.get("properties"), "warning.config.block.state.missing_properties"), "properties"));
                appearances = parseBlockAppearances(ResourceConfigUtils.getAsMap(ResourceConfigUtils.requireNonNullOrThrow(stateSection.get("appearances"), "warning.config.block.state.missing_appearances"), "appearances"));
                variants = parseBlockVariants(
                        ResourceConfigUtils.getAsMap(ResourceConfigUtils.requireNonNullOrThrow(stateSection.get("variants"), "warning.config.block.state.missing_variants"), "variants"),
                        it -> appearances.getOrDefault(it, -1), settings
                );
            }

            addBlockInternal(id, platformBuilder(id)
                    .appearances(appearances)
                    .variantMapper(variants)
                    .properties(properties)
                    .settings(settings)
                    .lootTable(LootTable.fromMap(ResourceConfigUtils.getAsMapOrNull(section.get("loot"), "loot")))
                    .behavior(MiscUtils.getAsMapList(ResourceConfigUtils.get(section, "behavior", "behaviors")))
                    .events(EventFunctions.parseEvents(ResourceConfigUtils.get(section, "events", "event")))
                    .build());
        }

        private Map<String, BlockStateVariant> parseBlockVariants(Map<String, Object> variantsSection,
                                                                  Function<String, Integer> appearanceValidator,
                                                                  BlockSettings parentSettings) {
            Map<String, BlockStateVariant> variants = new HashMap<>();
            for (Map.Entry<String, Object> entry : variantsSection.entrySet()) {
                Map<String, Object> variantSection = ResourceConfigUtils.getAsMap(entry.getValue(), entry.getKey());
                String variantNBT = entry.getKey();
                String appearance = ResourceConfigUtils.requireNonEmptyStringOrThrow(variantSection.get("appearance"), "warning.config.block.state.variant.missing_appearance");
                int appearanceId = appearanceValidator.apply(appearance);
                if (appearanceId == -1) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.variant.invalid_appearance", variantNBT, appearance);
                }
                int internalId = getInternalBlockId(ResourceConfigUtils.getAsInt(ResourceConfigUtils.requireNonNullOrThrow(variantSection.get("id"), "warning.config.block.state.missing_real_id"), "id"), appearanceId);
                Map<String, Object> anotherSetting = ResourceConfigUtils.getAsMapOrNull(variantSection.get("settings"), "settings");
                variants.put(variantNBT, new BlockStateVariant(appearance, anotherSetting == null ? parentSettings : BlockSettings.ofFullCopy(parentSettings, anotherSetting), internalId));
            }
            return variants;
        }

        private int getInternalBlockId(int internalId, int appearanceId) {
            Key baseBlock = getBlockOwnerId(appearanceId);
            Key internalBlockId = Key.of(Key.DEFAULT_NAMESPACE, baseBlock.value() + "_" + internalId);
            int internalBlockRegistryId = Optional.ofNullable(AbstractBlockManager.this.internalId2StateId.get(internalBlockId)).orElse(-1);
            if (internalBlockRegistryId == -1) {
                throw new LocalizedResourceConfigException("warning.config.block.state.invalid_real_id", internalBlockId.toString(), String.valueOf(availableAppearances(baseBlock) - 1));
            }
            return internalBlockRegistryId;
        }

        private Map<String, Integer> parseBlockAppearances(Map<String, Object> appearancesSection) {
            Map<String, Integer> appearances = new HashMap<>();
            for (Map.Entry<String, Object> entry : appearancesSection.entrySet()) {
                Map<String, Object> appearanceSection = ResourceConfigUtils.getAsMap(entry.getValue(), entry.getKey());
                int appearanceId = pluginFormattedBlockStateToRegistryId(ResourceConfigUtils.requireNonEmptyStringOrThrow(
                        appearanceSection.get("state"), "warning.config.block.state.missing_state"));
                this.arrangeModelForStateAndVerify(appearanceId, ResourceConfigUtils.get(appearanceSection, "model", "models"));
                appearances.put(entry.getKey(), appearanceId);
            }
            return appearances;
        }

        @NotNull
        private Map<String, Property<?>> parseBlockProperties(Map<String, Object> propertiesSection) {
            Map<String, Property<?>> properties = new HashMap<>();
            for (Map.Entry<String, Object> entry : propertiesSection.entrySet()) {
                Property<?> property = Properties.fromMap(entry.getKey(), ResourceConfigUtils.getAsMap(entry.getValue(), entry.getKey()));
                properties.put(entry.getKey(), property);
            }
            return properties;
        }

        private void arrangeModelForStateAndVerify(int registryId, Object modelOrModels) {
            // 如果没有配置models
            if (modelOrModels == null) {
                return;
            }
            // 获取variants
            List<JsonObject> variants;
            if (modelOrModels instanceof String model) {
                JsonObject json = new JsonObject();
                json.addProperty("model", model);
                variants = Collections.singletonList(json);
            } else {
                variants = ResourceConfigUtils.parseConfigAsList(modelOrModels, this::parseAppearanceModelSectionAsJson);
                if (variants.isEmpty()) {
                    return;
                }
            }
            // 拆分方块id与属性
            String blockState = stateRegistryIdToStateSNBT(registryId);
            Key blockId = Key.of(blockState.substring(blockState.indexOf('{') + 1, blockState.lastIndexOf('}')));
            String propertyNBT = blockState.substring(blockState.indexOf('[') + 1, blockState.lastIndexOf(']'));
            // 结合variants
            JsonElement combinedVariant = GsonHelper.combine(variants);
            Map<String, JsonElement> overrideMap = AbstractBlockManager.this.blockStateOverrides.computeIfAbsent(blockId, k -> new HashMap<>());
            JsonElement previous = overrideMap.get(propertyNBT);
            if (previous != null && !previous.equals(combinedVariant)) {
                throw new LocalizedResourceConfigException("warning.config.block.state.model.conflict", GsonHelper.get().toJson(combinedVariant), blockState, GsonHelper.get().toJson(previous));
            }
            overrideMap.put(propertyNBT, combinedVariant);
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
            if (section.containsKey("uvlock")) json.addProperty("uvlock", ResourceConfigUtils.getAsBoolean(section.get("uvlock"), "uvlock"));
            if (section.containsKey("weight"))
                json.addProperty("weight", ResourceConfigUtils.getAsInt(section.get("weight"), "weight"));
            Map<String, Object> generationMap = MiscUtils.castToMap(section.get("generation"), true);
            if (generationMap != null) {
                prepareModelGeneration(ModelGeneration.of(Key.of(modelPath), generationMap));
            }
            return json;
        }

        // 从方块外观的state里获取其原版方块的state id
        private int pluginFormattedBlockStateToRegistryId(String blockState) {
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
            int registryId;
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
                    List<Integer> arranger = blockAppearanceArranger.get(block);
                    if (arranger == null) {
                        throw new LocalizedResourceConfigException("warning.config.block.state.unavailable_vanilla", blockState);
                    }
                    if (arrangerIndex >= arranger.size()) {
                        throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla_id", blockState, String.valueOf(arranger.size() - 1));
                    }
                    registryId = arranger.get(arrangerIndex);
                } catch (NumberFormatException e) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", e, blockState);
                }
            } else {
                // 其他情况则是完整的方块
                BlockStateWrapper packedBlockState = createPackedBlockState(blockState);
                if (packedBlockState == null || !packedBlockState.isVanillaBlock()) {
                    throw new LocalizedResourceConfigException("warning.config.block.state.invalid_vanilla", blockState);
                }
                registryId = packedBlockState.registryId();
            }
            return registryId;
        }
    }
}
