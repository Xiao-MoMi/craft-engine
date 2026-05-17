package net.momirealms.craftengine.core.block.setting;

import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BlockSettings {
    boolean isRandomlyTicking;
    boolean burnable;
    int burnChance;
    int fireSpreadChance;
    int blockLight = -1;
    boolean replaceable;
    float hardness = 2f;
    float resistance = 2f;
    Tristate canOcclude = Tristate.UNDEFINED;
    boolean fluidState;
    boolean requireCorrectTools;
    boolean respectToolComponent;
    Tristate isRedstoneConductor = Tristate.UNDEFINED;
    Tristate isSuffocating = Tristate.UNDEFINED;
    Tristate isViewBlocking = Tristate.UNDEFINED;
    Tristate useShapeForLightOcclusion = Tristate.UNDEFINED;
    Tristate propagatesSkylightDown = Tristate.UNDEFINED;
    boolean isRaytraceBlocking = false;
    MapColor mapColor = MapColor.CLEAR;
    PushReaction pushReaction = PushReaction.NORMAL;
    int luminance;
    Instrument instrument = Instrument.HARP;
    BlockSounds sounds = BlockSounds.EMPTY;
    @Nullable
    Key itemId;
    Set<Key> tags = Set.of();
    float incorrectToolSpeed = 0.3f;
    LazyReference<Set<Key>> correctTools = LazyReference.lazyReference(Set::of);
    String name;
    String supportShapeBlockState;
    float friction = 0.6f;
    float speedFactor = 1f;
    float jumpFactor = 1f;
    Map<CustomDataType<?>, Object> customData = new IdentityHashMap<>(4);

    private BlockSettings() {}

    public static BlockSettings of() {
        return new BlockSettings();
    }

    public static BlockSettings fromConfig(ConfigSection section) {
        BlockSettings blockSettings = BlockSettings.of();
        if (section == null) return blockSettings;
        applyModifiers(blockSettings, section);
        return blockSettings;
    }

    public static BlockSettings ofFullCopyAndApply(BlockSettings settings, ConfigSection section) {
        BlockSettings copied = ofFullCopy(settings);
        applyModifiers(copied, section);
        return copied;
    }

    public static void applyModifiers(BlockSettings settings, ConfigSection section) {
        ExceptionCollector<KnownResourceException> collector = new ExceptionCollector<>(KnownResourceException.class);
        if (section != null) {
            for (String type : section.keySet()) {
                ConfigValue value = section.getValue(type);
                if (value == null) continue;
                String key = StringUtils.normalizeSettingsType(type);
                collector.runCatching(() -> {
                    Optional.ofNullable(BuiltInRegistries.BLOCK_SETTINGS_TYPE.getValue(Key.ce(key)))
                            .ifPresent(modifierType ->
                                    modifierType.factory().create(value).apply(settings));
                });
            }
        }
        collector.throwIfPresent();
    }

    public static BlockSettings ofFullCopy(BlockSettings settings) {
        BlockSettings newSettings = of();
        newSettings.canOcclude = settings.canOcclude;
        newSettings.hardness = settings.hardness;
        newSettings.resistance = settings.resistance;
        newSettings.isRandomlyTicking = settings.isRandomlyTicking;
        newSettings.burnable = settings.burnable;
        newSettings.replaceable = settings.replaceable;
        newSettings.mapColor = settings.mapColor;
        newSettings.pushReaction = settings.pushReaction;
        newSettings.luminance = settings.luminance;
        newSettings.instrument = settings.instrument;
        newSettings.sounds = settings.sounds;
        newSettings.itemId = settings.itemId;
        newSettings.tags = settings.tags;
        newSettings.burnChance = settings.burnChance;
        newSettings.requireCorrectTools = settings.requireCorrectTools;
        newSettings.respectToolComponent = settings.respectToolComponent;
        newSettings.fireSpreadChance = settings.fireSpreadChance;
        newSettings.isRedstoneConductor = settings.isRedstoneConductor;
        newSettings.isSuffocating = settings.isSuffocating;
        newSettings.isViewBlocking = settings.isViewBlocking;
        newSettings.correctTools = settings.correctTools;
        newSettings.fluidState = settings.fluidState;
        newSettings.blockLight = settings.blockLight;
        newSettings.name = settings.name;
        newSettings.incorrectToolSpeed = settings.incorrectToolSpeed;
        newSettings.supportShapeBlockState = settings.supportShapeBlockState;
        newSettings.propagatesSkylightDown = settings.propagatesSkylightDown;
        newSettings.useShapeForLightOcclusion = settings.useShapeForLightOcclusion;
        newSettings.speedFactor = settings.speedFactor;
        newSettings.jumpFactor = settings.jumpFactor;
        newSettings.friction = settings.friction;
        newSettings.isRaytraceBlocking = settings.isRaytraceBlocking;
        newSettings.customData = new IdentityHashMap<>(settings.customData);
        return newSettings;
    }

    @SuppressWarnings("unchecked")
    public <T> T getCustomData(CustomDataType<T> type) {
        return (T) this.customData.get(type);
    }

    public void clearCustomData() {
        this.customData.clear();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T removeCustomData(CustomDataType<?> type) {
        return (T) this.customData.remove(type);
    }

    public <T> void addCustomData(CustomDataType<T> key, T value) {
        this.customData.put(key, value);
    }

    public Set<Key> tags() {
        return tags;
    }

    public Key itemId() {
        return itemId;
    }

    public BlockSounds sounds() {
        return sounds;
    }

    public float resistance() {
        return resistance;
    }

    public boolean fluidState() {
        return fluidState;
    }

    public boolean isRandomlyTicking() {
        return isRandomlyTicking;
    }

    public boolean burnable() {
        return burnable;
    }

    public boolean replaceable() {
        return replaceable;
    }

    public float hardness() {
        return hardness;
    }

    public float friction() {
        return friction;
    }

    public float jumpFactor() {
        return jumpFactor;
    }

    public float speedFactor() {
        return speedFactor;
    }

    public Tristate canOcclude() {
        return canOcclude;
    }

    public float incorrectToolSpeed() {
        return incorrectToolSpeed;
    }

    public boolean requireCorrectTool() {
        return requireCorrectTools || !correctTools.get().isEmpty();
    }

    public String name() {
        return name;
    }

    public MapColor mapColor() {
        return mapColor;
    }

    public PushReaction pushReaction() {
        return pushReaction;
    }

    public int luminance() {
        return luminance;
    }

    public Instrument instrument() {
        return instrument;
    }

    public int burnChance() {
        return burnChance;
    }

    public int fireSpreadChance() {
        return fireSpreadChance;
    }

    public Tristate isRedstoneConductor() {
        return isRedstoneConductor;
    }

    public Tristate isSuffocating() {
        return isSuffocating;
    }

    public Tristate isViewBlocking() {
        return isViewBlocking;
    }

    public int blockLight() {
        return blockLight;
    }

    public boolean isCorrectTool(Key key) {
        return this.correctTools.get().contains(key);
    }

    public boolean respectToolComponent() {
        return respectToolComponent;
    }

    public String supportShapeBlockState() {
        return supportShapeBlockState;
    }

    public Tristate propagatesSkylightDown() {
        return propagatesSkylightDown;
    }

    public Tristate useShapeForLightOcclusion() {
        return useShapeForLightOcclusion;
    }

    public boolean isRaytraceBlocking() {
        return isRaytraceBlocking;
    }

    public BlockSettings isRaytraceBlocking(boolean value) {
        this.isRaytraceBlocking = value;
        return this;
    }

    public BlockSettings correctTools(LazyReference<Set<Key>> correctTools) {
        this.correctTools = correctTools;
        return this;
    }

    public BlockSettings burnChance(int burnChance) {
        this.burnChance = burnChance;
        return this;
    }

    public BlockSettings name(String name) {
        this.name = name;
        return this;
    }

    public BlockSettings fireSpreadChance(int fireSpreadChance) {
        this.fireSpreadChance = fireSpreadChance;
        return this;
    }

    public BlockSettings friction(float friction) {
        this.friction = friction;
        return this;
    }

    public BlockSettings speedFactor(float speedFactor) {
        this.speedFactor = speedFactor;
        return this;
    }

    public BlockSettings jumpFactor(float jumpFactor) {
        this.jumpFactor = jumpFactor;
        return this;
    }

    public BlockSettings tags(Set<Key> tags) {
        this.tags = tags;
        return this;
    }

    public BlockSettings itemId(Key itemId) {
        this.itemId = itemId;
        return this;
    }

    public BlockSettings sounds(BlockSounds sounds) {
        this.sounds = sounds;
        return this;
    }

    public BlockSettings instrument(Instrument instrument) {
        this.instrument = instrument;
        return this;
    }

    public BlockSettings luminance(int luminance) {
        this.luminance = luminance;
        return this;
    }

    public BlockSettings pushReaction(PushReaction pushReaction) {
        this.pushReaction = pushReaction;
        return this;
    }

    public BlockSettings mapColor(MapColor mapColor) {
        this.mapColor = mapColor;
        return this;
    }

    public BlockSettings hardness(float hardness) {
        this.hardness = hardness;
        return this;
    }

    public BlockSettings resistance(float resistance) {
        this.resistance = resistance;
        return this;
    }

    public BlockSettings canOcclude(boolean canOcclude) {
        this.canOcclude = canOcclude ? Tristate.TRUE : Tristate.FALSE;
        return this;
    }

    public BlockSettings requireCorrectTool(boolean requireCorrectTool) {
        this.requireCorrectTools = requireCorrectTool;
        return this;
    }

    public BlockSettings respectToolComponent(boolean respectToolComponent) {
        this.respectToolComponent = respectToolComponent;
        return this;
    }

    public BlockSettings incorrectToolSpeed(float incorrectToolSpeed) {
        this.incorrectToolSpeed = incorrectToolSpeed;
        return this;
    }

    public BlockSettings isRandomlyTicking(boolean isRandomlyTicking) {
        this.isRandomlyTicking = isRandomlyTicking;
        return this;
    }

    public BlockSettings burnable(boolean burnable) {
        this.burnable = burnable;
        return this;
    }

    public BlockSettings propagatesSkylightDown(boolean propagatesSkylightDown) {
        this.propagatesSkylightDown = propagatesSkylightDown ? Tristate.TRUE : Tristate.FALSE;
        return this;
    }

    public BlockSettings blockLight(int intValue) {
        this.blockLight = intValue;
        return this;
    }

    public BlockSettings isRedstoneConductor(boolean isRedstoneConductor) {
        this.isRedstoneConductor = isRedstoneConductor ? Tristate.TRUE : Tristate.FALSE;
        return this;
    }

    public BlockSettings isSuffocating(boolean isSuffocating) {
        this.isSuffocating = isSuffocating ? Tristate.TRUE : Tristate.FALSE;
        return this;
    }

    public BlockSettings isViewBlocking(boolean isViewBlocking) {
        this.isViewBlocking = isViewBlocking ? Tristate.TRUE : Tristate.FALSE;
        return this;
    }

    public BlockSettings replaceable(boolean replaceable) {
        this.replaceable = replaceable;
        return this;
    }

    public BlockSettings fluidState(boolean state) {
        this.fluidState = state;
        return this;
    }

    public BlockSettings supportShapeBlockState(String supportShapeBlockState) {
        this.supportShapeBlockState = supportShapeBlockState;
        return this;
    }

    public BlockSettings useShapeForLightOcclusion(boolean useShapeForLightOcclusion) {
        this.useShapeForLightOcclusion = useShapeForLightOcclusion ? Tristate.TRUE : Tristate.FALSE;
        return this;
    }
}

