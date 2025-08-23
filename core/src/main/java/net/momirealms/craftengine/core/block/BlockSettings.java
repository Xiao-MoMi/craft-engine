package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class BlockSettings {
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

    private BlockSettings() {}

    public static BlockSettings of() {
        return new BlockSettings();
    }

    public static BlockSettings fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return BlockSettings.of();
        return applyModifiers(BlockSettings.of(), map);
    }

    public static BlockSettings fromMap(Key id, Map<String, Object> map) {
        if (map == null || map.isEmpty()) return BlockSettings.of().itemId(id);
        return applyModifiers(BlockSettings.of().itemId(id), map);
    }

    public static BlockSettings ofFullCopy(BlockSettings settings, Map<String, Object> map) {
        return applyModifiers(ofFullCopy(settings), map);
    }

    public static BlockSettings applyModifiers(BlockSettings settings, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Modifier.Factory factory = Modifiers.FACTORIES.get(entry.getKey());
            if (factory != null) {
                factory.createModifier(entry.getValue()).apply(settings);
            } else {
                throw new LocalizedResourceConfigException("warning.config.block.settings.unknown", entry.getKey());
            }
        }
        return settings;
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
        return newSettings;
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

    public BlockSettings canOcclude(Tristate canOcclude) {
        this.canOcclude = canOcclude;
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

    public BlockSettings propagatesSkylightDown(Tristate propagatesSkylightDown) {
        this.propagatesSkylightDown = propagatesSkylightDown;
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

    public BlockSettings useShapeForLightOcclusion(Tristate useShapeForLightOcclusion) {
        this.useShapeForLightOcclusion = useShapeForLightOcclusion;
        return this;
    }

    @FunctionalInterface
    public interface Modifier {

        void apply(BlockSettings settings);

        @FunctionalInterface
        interface Factory {

            Modifier createModifier(Object value);
        }
    }

    public static class Modifiers {
        private static final Map<String, Modifier.Factory> FACTORIES = new HashMap<>();

        static {
            registerFactory("luminance", (value -> {
                int intValue = ResourceConfigUtils.getAsInt(value, "luminance");
                return settings -> settings.luminance(intValue);
            }));
            registerFactory("block-light", (value -> {
                int intValue = ResourceConfigUtils.getAsInt(value,  "block-light");
                return settings -> settings.blockLight(intValue);
            }));
            registerFactory("hardness", (value -> {
                float floatValue = ResourceConfigUtils.getAsFloat(value, "hardness");
                return settings -> settings.hardness(floatValue);
            }));
            registerFactory("friction", (value -> {
                float floatValue = ResourceConfigUtils.getAsFloat(value, "friction");
                return settings -> settings.friction(floatValue);
            }));
            registerFactory("speed-factor", (value -> {
                float floatValue = ResourceConfigUtils.getAsFloat(value, "speed-factor");
                return settings -> settings.speedFactor(floatValue);
            }));
            registerFactory("jump-factor", (value -> {
                float floatValue = ResourceConfigUtils.getAsFloat(value, "jump-factor");
                return settings -> settings.jumpFactor(floatValue);
            }));
            registerFactory("resistance", (value -> {
                float floatValue = ResourceConfigUtils.getAsFloat(value, "resistance");
                return settings -> settings.resistance(floatValue);
            }));
            registerFactory("is-randomly-ticking", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "is-randomly-ticking");
                return settings -> settings.isRandomlyTicking(booleanValue);
            }));
            registerFactory("propagate-skylight", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "propagate-skylight");
                return settings -> settings.propagatesSkylightDown(booleanValue ? Tristate.TRUE : Tristate.FALSE);
            }));
            registerFactory("push-reaction", (value -> {
                PushReaction reaction = PushReaction.valueOf(value.toString().toUpperCase(Locale.ENGLISH));
                return settings -> settings.pushReaction(reaction);
            }));
            registerFactory("map-color", (value -> {
                int intValue = ResourceConfigUtils.getAsInt(value, "map-color");
                return settings -> settings.mapColor(MapColor.get(intValue));
            }));
            registerFactory("burnable", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "burnable");
                return settings -> settings.burnable(booleanValue);
            }));
            registerFactory("instrument", (value -> {
                Instrument instrument = Instrument.valueOf(value.toString().toUpperCase(Locale.ENGLISH));
                return settings -> settings.instrument(instrument);
            }));
            registerFactory("item", (value -> {
                Key item = Key.of(value.toString());
                return settings -> settings.itemId(item);
            }));
            registerFactory("tags", (value -> {
                List<String> tags = MiscUtils.getAsStringList(value);
                return settings -> settings.tags(tags.stream().map(it -> {
                    if (it.charAt(0) == '#') return Key.of(it.substring(1));
                    else return Key.of(it);
                }).collect(Collectors.toSet()));
            }));
            registerFactory("burn-chance", (value -> {
                int intValue = ResourceConfigUtils.getAsInt(value, "burn-chance");
                return settings -> settings.burnChance(intValue);
            }));
            registerFactory("fire-spread-chance", (value -> {
                int intValue = ResourceConfigUtils.getAsInt(value, "fire-spread-chance");
                return settings -> settings.fireSpreadChance(intValue);
            }));
            registerFactory("replaceable", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "replaceable");
                return settings -> settings.replaceable(booleanValue);
            }));
            registerFactory("is-redstone-conductor", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "is-redstone-conductor");
                return settings -> settings.isRedstoneConductor(booleanValue);
            }));
            registerFactory("is-suffocating", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "is-suffocating");
                return settings -> settings.isSuffocating(booleanValue);
            }));
            registerFactory("is-view-blocking", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "is-view-blocking");
                return settings -> settings.isViewBlocking(booleanValue);
            }));
            registerFactory("sounds", (value -> {
                Map<String, Object> soundMap = MiscUtils.castToMap(value, false);
                return settings -> settings.sounds(BlockSounds.fromMap(soundMap));
            }));
            registerFactory("fluid-state", (value -> {
                String state = value.toString();
                return settings -> settings.fluidState(state.equals("water"));
            }));
            registerFactory("can-occlude", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "can-occlude");
                return settings -> settings.canOcclude(booleanValue ? Tristate.TRUE : Tristate.FALSE);
            }));
            registerFactory("correct-tools", (value -> {
                List<String> tools = MiscUtils.getAsStringList(value);
                LazyReference<Set<Key>> correctTools = LazyReference.lazyReference(() -> {
                    Set<Key> ids = new HashSet<>();
                    for (String tool : tools) {
                        if (tool.charAt(0) == '#') ids.addAll(CraftEngine.instance().itemManager().itemIdsByTag(Key.of(tool.substring(1))).stream().map(UniqueKey::key).toList());
                        else ids.add(Key.of(tool));
                    }
                    return ids;
                });
                return settings -> settings.correctTools(correctTools);
            }));
            registerFactory("require-correct-tools", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "require-correct-tools");
                return settings -> settings.requireCorrectTool(booleanValue);
            }));
            registerFactory("respect-tool-component", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "respect-tool-component");
                return settings -> settings.respectToolComponent(booleanValue);
            }));
            registerFactory("use-shape-for-light-occlusion", (value -> {
                boolean booleanValue = ResourceConfigUtils.getAsBoolean(value, "use-shape-for-light-occlusion");
                return settings -> settings.useShapeForLightOcclusion(booleanValue ? Tristate.TRUE : Tristate.FALSE);
            }));
            registerFactory("incorrect-tool-dig-speed", (value -> {
                float floatValue = ResourceConfigUtils.getAsFloat(value, "incorrect-tool-dig-speed");
                return settings -> settings.incorrectToolSpeed(floatValue);
            }));
            registerFactory("name", (value -> {
                String name = value.toString();
                return settings -> settings.name(name);
            }));
            registerFactory("support-shape", (value -> {
                String shape = value.toString();
                return settings -> settings.supportShapeBlockState(shape);
            }));
        }

        private static void registerFactory(String id, Modifier.Factory factory) {
            FACTORIES.put(id, factory);
        }
    }
}

