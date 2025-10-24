package net.momirealms.craftengine.core.plugin.context.event;

import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.*;

public class EventFunctions {

    static {
        register(CommonFunctions.COMMAND, new CommandFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.MESSAGE, new MessageFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.ACTIONBAR, new ActionBarFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.TITLE, new TitleFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.OPEN_WINDOW, new OpenWindowFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.CANCEL_EVENT, new CancelEventFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.RUN, new RunFunction.FactoryImpl<>(EventFunctions::fromMap, EventConditions::fromMap));
        register(CommonFunctions.PLACE_BLOCK, new PlaceBlockFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.UPDATE_BLOCK_PROPERTY, new UpdateBlockPropertyFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.TRANSFORM_BLOCK, new TransformBlockFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.BREAK_BLOCK, new BreakBlockFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.UPDATE_INTERACTION_TICK, new UpdateInteractionFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.SET_COUNT, new SetCountFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.DROP_LOOT, new DropLootFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.SWING_HAND, new SwingHandFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.SET_FOOD, new SetFoodFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.SET_SATURATION, new SetSaturationFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.PLAY_SOUND, new PlaySoundFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.PARTICLE, new ParticleFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.POTION_EFFECT, new PotionEffectFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.REMOVE_POTION_EFFECT, new RemovePotionEffectFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.LEVELER_EXP, new LevelerExpFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.SET_COOLDOWN, new SetCooldownFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.REMOVE_COOLDOWN, new RemoveCooldownFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.SPAWN_FURNITURE, new SpawnFurnitureFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.REMOVE_FURNITURE, new RemoveFurnitureFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.REPLACE_FURNITURE, new ReplaceFurnitureFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.MYTHIC_MOBS_SKILL, new MythicMobsSkillFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.TELEPORT, new TeleportFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.SET_VARIABLE, new SetVariableFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.TOAST, new ToastFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.DAMAGE, new DamageFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.MERCHANT_TRADE, new MerchantTradeFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.REMOVE_ENTITY, new RemoveEntityFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.IF_ELSE, new IfElseFunction.FactoryImpl<>(EventConditions::fromMap, EventFunctions::fromMap));
        register(CommonFunctions.ALTERNATIVES, new IfElseFunction.FactoryImpl<>(EventConditions::fromMap, EventFunctions::fromMap));
        register(CommonFunctions.WHEN, new WhenFunction.FactoryImpl<>(EventConditions::fromMap, EventFunctions::fromMap));
        register(CommonFunctions.DAMAGE_ITEM, new DamageItemFunction.FactoryImpl<>(EventConditions::fromMap));
    }

    public static void register(Key key, FunctionFactory<PlayerOptionalContext> factory) {
        ((WritableRegistry<FunctionFactory<PlayerOptionalContext>>) BuiltInRegistries.EVENT_FUNCTION_FACTORY)
                .register(ResourceKey.create(Registries.EVENT_FUNCTION_FACTORY.location(), key), factory);
    }

    public static Function<PlayerOptionalContext> fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.function.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        FunctionFactory<PlayerOptionalContext> factory = BuiltInRegistries.EVENT_FUNCTION_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.function.invalid_type", type);
        }
        return factory.create(map);
    }

    public static Map<EventTrigger, List<Function<PlayerOptionalContext>>> parseEvents(Object eventsObj) {
        if (eventsObj == null) return Map.of();
        EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events = new EnumMap<>(EventTrigger.class);
        if (eventsObj instanceof Map<?, ?> eventsSection) {
            Map<String, Object> eventsSectionMap = MiscUtils.castToMap(eventsSection, false);
            for (Map.Entry<String, Object> eventEntry : eventsSectionMap.entrySet()) {
                try {
                    EventTrigger eventTrigger = EventTrigger.byName(eventEntry.getKey());
                    events.put(eventTrigger, ResourceConfigUtils.parseConfigAsList(eventEntry.getValue(), EventFunctions::fromMap));
                } catch (IllegalArgumentException e) {
                    throw new LocalizedResourceConfigException("warning.config.event.invalid_trigger", eventEntry.getKey());
                }
            }
        } else if (eventsObj instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> eventsList = (List<Map<String, Object>>) list;
            for (Map<String, Object> eventSection : eventsList) {
                String on = ResourceConfigUtils.requireNonEmptyStringOrThrow(eventSection.get("on"), "warning.config.event.missing_trigger");
                try {
                    EventTrigger eventTrigger = EventTrigger.byName(on);
                    if (eventSection.containsKey("type")) {
                        Function<PlayerOptionalContext> function = EventFunctions.fromMap(eventSection);
                        events.computeIfAbsent(eventTrigger, k -> new ArrayList<>(4)).add(function);
                    } else if (eventSection.containsKey("functions")) {
                        events.computeIfAbsent(eventTrigger, k -> new ArrayList<>(4)).add(Objects.requireNonNull(BuiltInRegistries.EVENT_FUNCTION_FACTORY.getValue(CommonFunctions.RUN)).create(eventSection));
                    }
                } catch (IllegalArgumentException e) {
                    throw new LocalizedResourceConfigException("warning.config.event.invalid_trigger", on);
                }
            }
        }
        return events;
    }
}
