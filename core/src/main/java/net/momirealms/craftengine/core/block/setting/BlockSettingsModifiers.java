package net.momirealms.craftengine.core.block.setting;

import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BlockSettingsModifiers {
    public static final BlockSettingsModifierType<BlockSettingsModifier> ITEM = register(Key.ce("item"), value -> {
        Key itemId = value.getAsIdentifier();
        return settings -> settings.itemId(itemId);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> NAME = register(Key.ce("name"), value -> {
        String name = value.getAsString();
        return settings -> settings.name(name);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> SUPPORT_SHAPE = register(Key.ce("support_shape"), value -> {
        String name = value.getAsString();
        return settings -> settings.supportShapeBlockState(name);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> LUMINANCE = register(Key.ce("luminance"), value -> {
        int luminance = value.getAsInt();
        return settings -> settings.luminance(luminance);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> MAP_COLOR = register(Key.ce("map_color"), value -> {
        int color = value.getAsInt();
        return settings -> settings.mapColor(MapColor.get(color));
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> BURN_CHANCE = register(Key.ce("burn_chance"), value -> {
        int burnChance = value.getAsInt();
        return settings -> settings.burnChance(burnChance);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> FIRE_SPREAD_CHANCE = register(Key.ce("fire_spread_chance"), value -> {
        int fireSpreadChance = value.getAsInt();
        return settings -> settings.fireSpreadChance(fireSpreadChance);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> BLOCK_LIGHT = register(Key.ce("block_light"), value -> {
        int blockLight = value.getAsInt();
        return settings -> settings.blockLight(blockLight);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> LIGHT_BLOCK = register(Key.ce("light_block"), value -> {
        int blockLight = value.getAsInt();
        return settings -> settings.blockLight(blockLight);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> LIGHT_DAMPENING = register(Key.ce("light_dampening"), value -> {
        int blockLight = value.getAsInt();
        return settings -> settings.blockLight(blockLight);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> HARDNESS = register(Key.ce("hardness"), value -> {
        float hardness = value.getAsFloat();
        return settings -> settings.hardness(hardness);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> FRICTION = register(Key.ce("friction"), value -> {
        float friction = value.getAsFloat();
        return settings -> settings.friction(friction);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> SPEED_FACTOR = register(Key.ce("speed_factor"), value -> {
        float speedFactor = value.getAsFloat();
        return settings -> settings.speedFactor(speedFactor);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> JUMP_FACTOR = register(Key.ce("jump_factor"), value -> {
        float jumpFactor = value.getAsFloat();
        return settings -> settings.jumpFactor(jumpFactor);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> RESISTANCE = register(Key.ce("resistance"), value -> {
        float resistance = value.getAsFloat();
        return settings -> settings.resistance(resistance);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> INCORRECT_TOOL_DIG_SPEED = register(Key.ce("incorrect_tool_dig_speed"), value -> {
        float speed = value.getAsFloat();
        return settings -> settings.incorrectToolSpeed(speed);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> REPLACEABLE = register(Key.ce("replaceable"), value -> {
        boolean replaceable = value.getAsBoolean();
        return settings -> settings.replaceable(replaceable);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> IS_REDSTONE_CONDUCTOR = register(Key.ce("is_redstone_conductor"), value -> {
        boolean is = value.getAsBoolean();
        return settings -> settings.isRedstoneConductor(is);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> IS_SUFFOCATING = register(Key.ce("is_suffocating"), value -> {
        boolean is = value.getAsBoolean();
        return settings -> settings.isSuffocating(is);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> IS_RANDOMLY_TICKING = register(Key.ce("is_randomly_ticking"), value -> {
        boolean is = value.getAsBoolean();
        return settings -> settings.isRandomlyTicking(is);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> IS_VIEW_BLOCKING = register(Key.ce("is_view_blocking"), value -> {
        boolean is = value.getAsBoolean();
        return settings -> settings.isViewBlocking(is);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> PROPAGATE_SKYLIGHT = register(Key.ce("propagate_skylight"), value -> {
        boolean propagatesSkylightDown = value.getAsBoolean();
        return settings -> settings.propagatesSkylightDown(propagatesSkylightDown);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> BURNABLE = register(Key.ce("burnable"), value -> {
        boolean burnable = value.getAsBoolean();
        return settings -> settings.burnable(burnable);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> PUSH_REACTION = register(Key.ce("push_reaction"), value -> {
        PushReaction pushReaction = value.getAsEnum(PushReaction.class);
        return settings -> settings.pushReaction(pushReaction);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> INSTRUMENT = register(Key.ce("instrument"), value -> {
        Instrument instrument = value.getAsEnum(Instrument.class);
        return settings -> settings.instrument(instrument);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> SOUNDS = register(Key.ce("sounds"), value -> {
        BlockSounds blockSounds = BlockSounds.fromConfig(value.getAsSection());
        return settings -> settings.sounds(blockSounds);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> FLUID_STATE = register(Key.ce("fluid_state"), value -> {
        String state = value.getAsString();
        return settings -> settings.fluidState(state.equals("water"));
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> CAN_OCCLUDE = register(Key.ce("can_occlude"), value -> {
        boolean can = value.getAsBoolean();
        return settings -> settings.canOcclude(can);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> REQUIRE_CORRECT_TOOLS = register(Key.ce("require_correct_tools"), value -> {
        boolean require = value.getAsBoolean();
        return settings -> settings.requireCorrectTool(require);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> RESPECT_TOOL_COMPONENT = register(Key.ce("respect_tool_component"), value -> {
        boolean respect = value.getAsBoolean();
        return settings -> settings.respectToolComponent(respect);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> USE_SHAPE_FOR_LIGHT_OCCLUSION = register(Key.ce("use_shape_for_light_occlusion"), value -> {
        boolean use = value.getAsBoolean();
        return settings -> settings.useShapeForLightOcclusion(use);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> TAGS = register(Key.ce("tags"), value -> {
        List<Key> tags = value.getAsList(v -> {
            String tagString = v.getAsString();
            if (tagString.charAt(0) == '#') {
                return Key.of(tagString.substring(1));
            } else {
                return v.getAsIdentifier();
            }
        });
        return settings -> settings.tags(Set.copyOf(tags));
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> CORRECT_TOOLS = register(Key.ce("correct_tools"), value -> {
        List<String> tools = value.getAsStringList();
        LazyReference<Set<Key>> correctTools = LazyReference.lazyReference(() -> {
            Set<Key> ids = new HashSet<>();
            for (String tool : tools) {
                if (tool.charAt(0) == '#') ids.addAll(CraftEngine.instance().itemManager().itemIdsByTag(Key.of(tool.substring(1))).stream().map(UniqueKey::key).toList());
                else ids.add(Key.of(tool));
            }
            return ids;
        });
        return settings -> settings.correctTools(correctTools);
    });
    public static final BlockSettingsModifierType<BlockSettingsModifier> IS_RAYTRACE_BLOCKING = register(Key.ce("block_raytrace"), value -> {
        boolean block = value.getAsBoolean();
        return settings -> settings.isRaytraceBlocking(block);
    });

    private BlockSettingsModifiers() {}

    public static void init() {}

    public static <M extends BlockSettingsModifier> BlockSettingsModifierType<M> register(Key id, BlockSettingsModifierFactory<M> factory) {
        BlockSettingsModifierType<M> type = new BlockSettingsModifierType<>(id, factory);
        ((WritableRegistry<BlockSettingsModifierType<? extends BlockSettingsModifier>>) BuiltInRegistries.BLOCK_SETTINGS_TYPE)
                .register(ResourceKey.create(Registries.BLOCK_SETTINGS_TYPE.location(), id), type);
        return type;
    }
}
