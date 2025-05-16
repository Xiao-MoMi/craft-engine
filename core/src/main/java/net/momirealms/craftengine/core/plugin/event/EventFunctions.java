package net.momirealms.craftengine.core.plugin.event;

import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EventFunctions {

    static {
        register(CommonFunctions.COMMAND, new CommandFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.MESSAGE, new MessageFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.ACTIONBAR, new ActionBarFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.TITLE, new TitleFunction.FactoryImpl<>(EventConditions::fromMap));
        register(CommonFunctions.CANCEL_EVENT, new CancelEventFunction.FactoryImpl<>(EventConditions::fromMap));
    }

    public static void register(Key key, FunctionFactory<PlayerOptionalContext> factory) {
        Holder.Reference<FunctionFactory<PlayerOptionalContext>> holder = ((WritableRegistry<FunctionFactory<PlayerOptionalContext>>) BuiltInRegistries.EVENT_FUNCTION_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.EVENT_FUNCTION_FACTORY.location(), key));
        holder.bindValue(factory);
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

    public static EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> parseEvents(Object eventsObj) {
        EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events = new EnumMap<>(EventTrigger.class);
        if (eventsObj instanceof Map<?, ?> eventsSection) {
            Map<String, Object> eventsSectionMap = MiscUtils.castToMap(eventsSection, false);
            for (Map.Entry<String, Object> eventEntry : eventsSectionMap.entrySet()) {
                try {
                    EventTrigger eventTrigger = EventTrigger.byName(eventEntry.getKey());
                    if (eventEntry.getValue() instanceof List<?> list) {
                        if (list.size() == 1) {
                            events.put(eventTrigger, List.of(EventFunctions.fromMap(MiscUtils.castToMap(list.get(0), false))));
                        } else if (list.size() == 2) {
                            events.put(eventTrigger, List.of(
                                    EventFunctions.fromMap(MiscUtils.castToMap(list.get(0), false)),
                                    EventFunctions.fromMap(MiscUtils.castToMap(list.get(1), false))
                            ));
                        } else {
                            List<Function<PlayerOptionalContext>> eventsList = new ArrayList<>();
                            for (Object event : list) {
                                eventsList.add(EventFunctions.fromMap(MiscUtils.castToMap(event, false)));
                            }
                            events.put(eventTrigger, eventsList);
                        }
                    } else if (eventEntry.getValue() instanceof Map<?, ?> eventSection) {
                        events.put(eventTrigger, List.of(EventFunctions.fromMap(MiscUtils.castToMap(eventSection, false))));
                    }
                } catch (IllegalArgumentException e) {
                    throw new LocalizedResourceConfigException("warning.config.event.invalid_trigger", eventEntry.getKey());
                }
            }
        } else if (eventsObj instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> eventsList = (List<Map<String, Object>>) list;
            for (Map<String, Object> eventSection : eventsList) {
                Object onObj = eventSection.get("on");
                if (onObj == null) {
                    throw new LocalizedResourceConfigException("warning.config.event.missing_trigger");
                }
                try {
                    EventTrigger eventTrigger = EventTrigger.byName(onObj.toString());
                    if (eventSection.containsKey("type")) {
                        Function<PlayerOptionalContext> function = EventFunctions.fromMap(eventSection);
                        events.computeIfAbsent(eventTrigger, k -> new ArrayList<>(4)).add(function);
                    } else if (eventSection.containsKey("functions")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> functionList = (List<Map<String, Object>>) eventSection.get("functions");
                        for (Map<String, Object> function : functionList) {
                            events.computeIfAbsent(eventTrigger, k -> new ArrayList<>(4)).add(EventFunctions.fromMap(function));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new LocalizedResourceConfigException("warning.config.event.invalid_trigger", onObj.toString());
                }
            }
        }
        return events;
    }
}
