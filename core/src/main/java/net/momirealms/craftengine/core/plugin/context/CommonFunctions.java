package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.function.*;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.EnumUtils;
import net.momirealms.craftengine.core.util.ExceptionCollector;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class CommonFunctions {
    public static final CommonFunctionType<CommandFunction<Context>> COMMAND = register(Key.ce("command"), CommandFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<MessageFunction<Context>> MESSAGE = register(Key.ce("message"), MessageFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<ActionBarFunction<Context>> ACTIONBAR = register(Key.ce("actionbar"), ActionBarFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<TitleFunction<Context>> TITLE = register(Key.ce("title"), TitleFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<OpenWindowFunction<Context>> OPEN_WINDOW = register(Key.ce("open_window"), OpenWindowFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<CancelEventFunction<Context>> CANCEL_EVENT = register(Key.ce("cancel_event"), CancelEventFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<RunFunction<Context>> RUN = register(Key.ce("run"), RunFunction.factory(CommonFunctions::fromConfig, CommonConditions::fromConfig));
    public static final CommonFunctionType<PlaceBlockFunction<Context>> PLACE_BLOCK = register(Key.ce("place_block"), PlaceBlockFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<UpdateBlockPropertyFunction<Context>> UPDATE_BLOCK_PROPERTY = register(Key.ce("update_block_property"), UpdateBlockPropertyFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<TransformBlockFunction<Context>> TRANSFORM_BLOCK = register(Key.ce("transform_block"), TransformBlockFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<BreakBlockFunction<Context>> BREAK_BLOCK = register(Key.ce("break_block"), BreakBlockFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<UpdateInteractionFunction<Context>> UPDATE_INTERACTION_TICK = register(Key.ce("update_interaction_tick"), UpdateInteractionFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetCountFunction<Context>> SET_COUNT = register(Key.ce("set_count"), SetCountFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<DropLootFunction<Context>> DROP_LOOT = register(Key.ce("drop_loot"), DropLootFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SwingHandFunction<Context>> SWING_HAND = register(Key.ce("swing_hand"), SwingHandFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetFoodFunction<Context>> SET_FOOD = register(Key.ce("set_food"), SetFoodFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetSaturationFunction<Context>> SET_SATURATION = register(Key.ce("set_saturation"), SetSaturationFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<PlaySoundFunction<Context>> PLAY_SOUND = register(Key.ce("play_sound"), PlaySoundFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<ParticleFunction<Context>> PARTICLE = register(Key.ce("particle"), ParticleFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<PotionEffectFunction<Context>> POTION_EFFECT = register(Key.ce("potion_effect"), PotionEffectFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<RemovePotionEffectFunction<Context>> REMOVE_POTION_EFFECT = register(Key.ce("remove_potion_effect"), RemovePotionEffectFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<LevelerExpFunction<Context>> LEVELER_EXP = register(Key.ce("leveler_exp"), LevelerExpFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetCooldownFunction<Context>> SET_COOLDOWN = register(Key.ce("set_cooldown"), SetCooldownFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetItemCooldownFunction<Context>> SET_ITEM_COOLDOWN = register(Key.ce("set_item_cooldown"), SetItemCooldownFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<RemoveCooldownFunction<Context>> REMOVE_COOLDOWN = register(Key.ce("remove_cooldown"), RemoveCooldownFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SpawnFurnitureFunction<Context>> SPAWN_FURNITURE = register(Key.ce("spawn_furniture"), SpawnFurnitureFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<RemoveFurnitureFunction<Context>> REMOVE_FURNITURE = register(Key.ce("remove_furniture"), RemoveFurnitureFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<ReplaceFurnitureFunction<Context>> REPLACE_FURNITURE = register(Key.ce("replace_furniture"), ReplaceFurnitureFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<RotateFurnitureFunction<Context>> ROTATE_FURNITURE = register(Key.ce("rotate_furniture"), RotateFurnitureFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetFurnitureVariantFunction<Context>> SET_FURNITURE_VARIANT = register(Key.ce("set_furniture_variant"), SetFurnitureVariantFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<TeleportFunction<Context>> TELEPORT = register(Key.ce("teleport"), TeleportFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetVariableFunction<Context>> SET_VARIABLE = register(Key.ce("set_variable"), SetVariableFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<ToastFunction<Context>> TOAST = register(Key.ce("toast"), ToastFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<DamageFunction<Context>> DAMAGE = register(Key.ce("damage"), DamageFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<HealFunction<Context>> HEAL = register(Key.ce("heal"), HealFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<MerchantTradeFunction<Context>> MERCHANT_TRADE = register(Key.ce("merchant_trade"), MerchantTradeFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<RemoveEntityFunction<Context>> REMOVE_ENTITY = register(Key.ce("remove_entity"), RemoveEntityFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<IfElseFunction<Context>> IF_ELSE = register(Key.ce("if_else"), IfElseFunction.factory(CommonFunctions::fromConfig, CommonConditions::fromConfig));
    public static final CommonFunctionType<IfElseFunction<Context>> ALTERNATIVES = register(Key.ce("alternatives"), IfElseFunction.factory(CommonFunctions::fromConfig, CommonConditions::fromConfig));
    public static final CommonFunctionType<WhenFunction<Context>> WHEN = register(Key.ce("when"), WhenFunction.factory(CommonFunctions::fromConfig, CommonConditions::fromConfig));
    public static final CommonFunctionType<DamageItemFunction<Context>> DAMAGE_ITEM = register(Key.ce("damage_item"), DamageItemFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<CycleBlockPropertyFunction<Context>> CYCLE_BLOCK_PROPERTY = register(Key.ce("cycle_block_property"), CycleBlockPropertyFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetExpFunction<Context>> SET_EXP = register(Key.ce("set_exp"), SetExpFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<SetLevelFunction<Context>> SET_LEVEL = register(Key.ce("set_level"), SetLevelFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<PlayTotemAnimationFunction<Context>> PLAY_TOTEM_ANIMATION = register(Key.ce("play_totem_animation"), PlayTotemAnimationFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<CloseInventoryFunction<Context>> CLOSE_INVENTORY = register(Key.ce("close_inventory"), CloseInventoryFunction.factory(CommonConditions::fromConfig));
    public static final CommonFunctionType<ClearItemFunction<Context>> CLEAR_ITEM = register(Key.ce("clear_item"), ClearItemFunction.factory(CommonConditions::fromConfig));

    private CommonFunctions() {}

    public static <T extends Function<Context>> CommonFunctionType<T> register(Key key, FunctionFactory<Context, T> factory) {
        CommonFunctionType<T> type = new CommonFunctionType<>(key, factory);
        ((WritableRegistry<CommonFunctionType<?>>) BuiltInRegistries.COMMON_FUNCTION_TYPE)
                .register(ResourceKey.create(Registries.COMMON_FUNCTION_TYPE.location(), key), type);
        return type;
    }

    public static Function<Context> fromConfig(ConfigValue value) {
        return fromConfig(value.getAsSection());
    }

    public static Function<Context> fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        CommonFunctionType<? extends Function<Context>> functionType = BuiltInRegistries.COMMON_FUNCTION_TYPE.getValue(key);
        if (functionType == null) {
            throw new KnownResourceException("function.unknown_type", section.assemblePath("type"), type);
        }
        return functionType.factory().create(section);
    }

    public static void parseEvents(ConfigValue eventValue, BiConsumer<EventTrigger, Function<Context>> consumer) {
        if (eventValue == null) {
           return;
        }

        /*

        events:
          break:
            - type: a
            - type: b

         */

        ExceptionCollector<KnownResourceException> exceptionCollector = new ExceptionCollector<>(KnownResourceException.class);

        if (eventValue.is(Map.class)) {
            ConfigSection eventsSection = eventValue.getAsSection();
            for (String eventType : eventsSection.keySet()) {
                EventTrigger eventTrigger = EventTrigger.byId(eventType);
                if (eventTrigger != null) {
                    eventsSection.getNonNullValue(eventType, ConfigConstants.ARGUMENT_SECTION).forEach(v -> {
                        exceptionCollector.runCatching(() -> {
                            consumer.accept(eventTrigger, CommonFunctions.fromConfig(v));
                        });
                    });
                } else {
                    exceptionCollector.add(new KnownResourceException(ConfigConstants.PARSE_ENUM_FAILED, eventsSection.path(), eventType, EnumUtils.toString(EventTrigger.values())));
                }
            }
        }

        /*

        events:
          - on: break:
            functions:
              - type: a
              - type: b
         */

        else if (eventValue.is(List.class)) {
            eventValue.forEach(value -> exceptionCollector.runCatching(() -> {
                ConfigSection innerSection = value.getAsSection();
                ConfigValue triggerValue = innerSection.getNonNullValue("on", ConfigConstants.ARGUMENT_STRING);
                if (triggerValue.is(List.class)) {
                    List<EventTrigger> triggers = triggerValue.getAsList(v -> v.getAsEnum(EventTrigger.class, EventTrigger::byId));
                    if (innerSection.containsKey("type")) {
                        triggers.forEach(trigger -> consumer.accept(trigger, CommonFunctions.fromConfig(triggerValue)));
                    } else if (innerSection.containsKey("functions")) {
                        triggers.forEach(trigger -> consumer.accept(trigger, RUN.factory().create(innerSection)));
                    }
                } else {
                    EventTrigger eventTrigger = triggerValue.getAsEnum(EventTrigger.class, EventTrigger::byId);
                    if (innerSection.containsKey("type")) {
                        consumer.accept(eventTrigger, CommonFunctions.fromConfig(innerSection));
                    } else if (innerSection.containsKey("functions")) {
                        consumer.accept(eventTrigger, RUN.factory().create(innerSection));
                    }
                }
            }));
        }

        exceptionCollector.throwIfPresent();
    }
}
