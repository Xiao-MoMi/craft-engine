package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.PreConditions;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import static net.momirealms.craftengine.core.util.MiscUtils.castToList;
import static net.momirealms.craftengine.core.util.MiscUtils.castToMap;

public class TemplateManagerImpl implements TemplateManager {
    private final CraftEngine plugin;
    private final Map<Key, Object> templates = new HashMap<>();
    private final static Set<String> blacklistedKeys = new HashSet<>(Set.of(TEMPLATE, ARGUMENTS, OVERRIDES));

    public TemplateManagerImpl(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void unload() {
        this.templates.clear();
    }

    @Override
    public void parseSection(Pack pack,
                             Path path,
                             Key id,
                             Map<String, Object> section) {
        if (PreConditions.runIfTrue(this.templates.containsKey(id), () -> this.plugin.logger().warn(path, "Template duplicates: " + id))) return;
        this.templates.put(id, section);
    }

    @Override
    public Map<String, Object> applyTemplates(Map<String, Object> input) {
        Objects.requireNonNull(input, "Input must not be null");
        Map<String, Object> result = new LinkedHashMap<>();
        applyTemplatesRecursive("", input, result, Collections.emptyMap());
        return result;
    }

    private void applyTemplatesRecursive(String currentPath,
                                         Map<String, Object> input,
                                         Map<String, Object> result,
                                         Map<String, Supplier<Object>> parentArguments) {
        if (input.containsKey("template")) {
            TemplateProcessingResult processingResult = processTemplates(input, parentArguments);
            List<Object> templates = processingResult.templates();
            Object overrides = processingResult.overrides();
            Map<String, Supplier<Object>> arguments = mergeArguments(parentArguments, processingResult.arguments());

            for (Object template : templates) {
                if (template instanceof Map<?, ?> mapTemplate) {
                    handleMapTemplate(currentPath, castToMap(mapTemplate, false), overrides, arguments, input, result);
                } else if (template instanceof List<?> listTemplate) {
                    handleListTemplate(currentPath, castToList(listTemplate, false), overrides, arguments, result);
                } else if (template instanceof String stringTemplate) {
                    result.put(currentPath, applyArgument(stringTemplate, arguments));
                } else {
                    result.put(currentPath, template);
                }
            }
        } else {
            Map<String, Object> innerResult = currentPath.isEmpty() ? result : new LinkedHashMap<>();
            if (!currentPath.isEmpty()) {
                result.put(currentPath, innerResult);
            }

            input.forEach((key, value) -> processValue(
                    value,
                    processedValue -> innerResult.put(key, processedValue),
                    parentArguments
            ));
        }
    }

    private TemplateProcessingResult processTemplates(Map<String, Object> input, Map<String, Supplier<Object>> parentArguments) {
        List<String> templateIds = MiscUtils.getAsStringList(input.get("template"));
        List<Object> templateList = new ArrayList<>();
        for (String templateId : templateIds) {
            Object template = Optional.ofNullable(templates.get(Key.of(templateId)))
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
            templateList.add(template);
        }
        Map<String, Supplier<Object>> arguments = getArguments(
                castToMap(input.getOrDefault("arguments", Collections.emptyMap()), false)
        );
        arguments.putAll(parentArguments);
        return new TemplateProcessingResult(
                templateList,
                input.get("overrides"),
                arguments
        );
    }

    private Map<String, Supplier<Object>> getArguments(@NotNull Map<String, Object> argumentMap) {
        Map<String, Supplier<Object>> result = new HashMap<>();
        argumentMap.forEach((key, value) -> {
            String placeholder = "{" + key + "}";
            if (value instanceof Map<?, ?> nestedMap) {
                result.put(placeholder, TemplateArguments.fromMap(castToMap(nestedMap, false)));
            } else if (value instanceof List<?> nestedList) {
                result.put(placeholder, new ListTemplateArgument(castToList(nestedList, false)));
            } else {
                result.put(placeholder, value::toString);
            }
        });
        return result;
    }

    private Object applyArgument(String input, Map<String, Supplier<Object>> arguments) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = PATTERN.matcher(input);
        boolean first = true;
        while (matcher.find()) {
            String placeholder = matcher.group();
            Supplier<Object> replacer = arguments.get(placeholder);
            if (replacer != null) {
                if (first) {
                    first = false;
                    if (input.length() == placeholder.length()) {
                        return replacer.get();
                    } else {
                        matcher.appendReplacement(result, replacer.get().toString());
                    }
                } else {
                    matcher.appendReplacement(result, replacer.get().toString());
                }
            } else {
                throw new IllegalArgumentException("Missing template argument: " + placeholder);
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void processValue(Object value,
                              Consumer<Object> resultConsumer,
                              Map<String, Supplier<Object>> arguments) {
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> nestedResult = new LinkedHashMap<>();
            applyTemplatesRecursive("", castToMap(mapValue, false), nestedResult, arguments);
            resultConsumer.accept(nestedResult);
        } else if (value instanceof List<?> listValue) {
            List<Object> nestedList = new ArrayList<>();
            processList(castToList(listValue, false), nestedList, arguments);
            resultConsumer.accept(nestedList);
        } else if (value instanceof String strValue) {
            resultConsumer.accept(applyArgument(strValue, arguments));
        } else {
            resultConsumer.accept(value);
        }
    }

    private void processList(List<Object> inputList, List<Object> resultList, Map<String, Supplier<Object>> arguments) {
        for (Object item : inputList) {
            processValue(item, resultList::add, arguments);
        }
    }

    private void handleMapTemplate(String currentPath,
                                   Map<String, Object> template,
                                   Object overrides,
                                   Map<String, Supplier<Object>> arguments,
                                   Map<String, Object> input,
                                   Map<String, Object> result) {
        Map<String, Object> merged = new LinkedHashMap<>(template);
        if (overrides != null) {
            merged.putAll(castToMap(overrides, false));
        }
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (!blacklistedKeys.contains(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        applyTemplatesRecursive(currentPath, merged, result, arguments);
    }

    private void handleListTemplate(String currentPath,
                                    List<Object> template,
                                    Object overrides,
                                    Map<String, Supplier<Object>> arguments,
                                    Map<String, Object> result) {
        List<Object> merged = (overrides instanceof List<?> overrideList && !overrideList.isEmpty())
                ? castToList(overrideList, false)
                : template;
        List<Object> processedList = new ArrayList<>();
        processList(merged, processedList, arguments);
        result.put(currentPath, processedList);
    }

    private record TemplateProcessingResult(
            List<Object> templates,
            Object overrides,
            Map<String, Supplier<Object>> arguments
    ) {}

    private static Map<String, Supplier<Object>> mergeArguments(
            Map<String, Supplier<Object>> parent,
            Map<String, Supplier<Object>> child
    ) {
        Map<String, Supplier<Object>> merged = new HashMap<>(parent);
        merged.putAll(child);
        return merged;
    }
}
