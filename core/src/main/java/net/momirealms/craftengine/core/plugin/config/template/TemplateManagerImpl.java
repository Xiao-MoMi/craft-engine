package net.momirealms.craftengine.core.plugin.config.template;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.config.template.argument.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TemplateManagerImpl implements TemplateManager {
    private static final ArgumentString TEMPLATE = ArgumentString.Literal.literal("template");
    private static final ArgumentString TEMPLATES = ArgumentString.Literal.literal("templates");
    private static final ArgumentString OVERRIDES = ArgumentString.Literal.literal("overrides");
    private static final ArgumentString ARGUMENTS = ArgumentString.Literal.literal("arguments");
    private static final ArgumentString MERGES = ArgumentString.Literal.literal("merges");
    private final static Set<ArgumentString> NON_TEMPLATE_ARGUMENTS = new HashSet<>(Set.of(TEMPLATE, TEMPLATES, ARGUMENTS, OVERRIDES, MERGES));

    private final Map<Key, Object> templates = new ConcurrentHashMap<>(256, 0.5f);
    private final TemplateParser templateParser;

    TemplateManagerImpl() {
        this.templateParser = new TemplateParser();
    }

    @Override
    public void unload() {
        this.templates.clear();
    }

    @Override
    public ConfigParser parser() {
        return this.templateParser;
    }

    public final class TemplateParser extends IdValueConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[]{"templates", "template"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return TemplateManagerImpl.this.templates.size();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.TEMPLATE;
        }

        @Override
        public void parseValue(Pack pack, Path filePath, Key id, ConfigValue value) {
            TemplateManagerImpl.this.templates.put(id, preprocessUnknownValue(value));
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        // 覆写父类逻辑，禁止应用模板
        @Override
        protected Object createConfigValue(Key id, ConfigValue value) {
            return value.value();
        }
    }

    @Override
    public Object applyTemplates(Key id, ConfigValue input) {
        if (input == null) return null;
        Object preprocessedInput = preprocessUnknownValue(input);
        return processUnknownValue(input.path(), preprocessedInput, Map.of(
                "__NAMESPACE__", PlainStringTemplateArgument.plain(id.namespace()),
                "__ID__", PlainStringTemplateArgument.plain(id.value())
        ));
    }

    public Object preprocessUnknownValue(ConfigValue value) {
        if (value == null) return null;
        if (value.is(Map.class)) {
            ConfigSection in = value.getAsSection();
            Map<ArgumentString, Object> out = new LinkedHashMap<>(MiscUtils.ceil(in.size() * 1.5));
            for (String key : in.keySet()) {
                out.put(ArgumentString.preParse(value.path(), key), preprocessUnknownValue(in.getValue(key)));
            }
            return out;
        } else if (value.is(List.class)) {
            return value.getAsList(this::preprocessUnknownValue);
        } else if (value.is(String.class)) {
            return ArgumentString.preParse(value.path(), value.getAsString());
        } else {
            return value.value();
        }
    }

    // 对于处理map，只有input是已知map，而返回值可能并不是
    private Object processMap(
            String node,
            Map<ArgumentString, Object> input,
            Map<String, TemplateArgument> arguments
    ) {
        // 传入的input是否含有template，这种情况下，返回值有可能是非map
        Object template = input.get(TEMPLATE);
        if (template == null) {
            template = input.get(TEMPLATES);
        }
        if (template != null) {
            TemplateProcessingResult processingResult = processTemplates(node, template, input, arguments);
            List<Object> processedTemplates = processingResult.templates();
            if (!processedTemplates.isEmpty()) {
                // 先获取第一个模板的类型
                Object firstTemplate = processedTemplates.getFirst();
                // 如果是map，应当深度合并
                if (firstTemplate instanceof Map<?, ?>) {
                    Map<String, Object> results = new LinkedHashMap<>();
                    for (Object processedTemplate : processedTemplates) {
                        if (processedTemplate instanceof Map<?, ?> map) {
                            deepMergeMaps(results, MiscUtils.castToMap(map));
                        }
                    }
                    if (processingResult.overrides() instanceof Map<?, ?> overrides) {
                        results.putAll(MiscUtils.castToMap(overrides));
                    }
                    if (processingResult.merges() instanceof Map<?, ?> merges) {
                        deepMergeMaps(results, MiscUtils.castToMap(merges));
                    }
                    return results;
                } else if (firstTemplate instanceof List<?>) {
                    List<Object> results = new ObjectArrayList<>();
                    // 仅仅合并list
                    for (Object processedTemplate : processedTemplates) {
                        if (processedTemplate instanceof List<?> anotherList) {
                            results.addAll(anotherList);
                        }
                    }
                    if (processingResult.overrides() instanceof List<?> overrides) {
                        results.clear();
                        results.addAll(overrides);
                    }
                    if (processingResult.merges() instanceof List<?> merges) {
                        results.addAll(merges);
                    }
                    return results;
                } else {
                    // 有覆写用覆写，无覆写返回最后一个模板值
                    if (processingResult.overrides() != null) {
                        return processingResult.overrides();
                    }
                    if (processingResult.merges() != null) {
                        return processingResult.merges();
                    }
                    return processedTemplates.getLast();
                }
            } else {
                // 模板为空啦，如果是map，则合并
                if (processingResult.overrides() instanceof Map<?, ?> overrides) {
                    Map<String, Object> output = new LinkedHashMap<>(MiscUtils.castToMap(overrides));
                    if (processingResult.merges() instanceof Map<?, ?> merges) {
                        deepMergeMaps(output, MiscUtils.castToMap(merges));
                    }
                    return output;
                } else if (processingResult.overrides() instanceof List<?> overrides) {
                    List<Object> output = new ArrayList<>(overrides);
                    if (processingResult.merges() instanceof List<?> merges) {
                        output.addAll(merges);
                    }
                    return output;
                }
                // 否则有overrides就返回overrides
                if (processingResult.overrides() != null) {
                    return processingResult.overrides();
                }
                // 否则有merges就返回merges
                if (processingResult.merges() != null) {
                    return processingResult.merges();
                }
                return null;
            }
        } else {
            // 如果不是模板，则返回值一定是map
            // 依次处理map下的每个参数
            Map<String, Object> result = new LinkedHashMap<>(input.size());
            for (Map.Entry<ArgumentString, Object> inputEntry : input.entrySet()) {
                ArgumentString argumentKey = inputEntry.getKey();
                String newNode = node + "." + argumentKey.rawValue();
                Object key = argumentKey.get(newNode, arguments);
                // 如果key为null说明不插入此键
                if (key != null) {
                    result.put(key.toString(), processUnknownValue(newNode, inputEntry.getValue(), arguments));
                }
            }
            return result;
        }
    }

    // 处理一个类型未知的值，本方法只管将member处理好后，传递回调用者a
    @SuppressWarnings("unchecked")
    public Object processUnknownValue(String node,
                                      Object value,
                                      Map<String, TemplateArgument> arguments) {
        switch (value) {
            case Map<?, ?> input ->
            // map下面还是个map吗？这并不一定
            // 这时候并不一定是map，最终类型取决于template，那么应当根据template的结果进行调整，所以我们继续交给上方方法处理
            {
                return processMap(node, (Map<ArgumentString, Object>) input, arguments);
            }
            case List<?> input -> {
                List<Object> result = new ObjectArrayList<>();
                for (int i = 0, size = input.size(); i < size; i++) {
                    result.add(processUnknownValue(node + "[" + i + "]", input.get(i), arguments));
                }
                return result;
            }
            case ArgumentString arg -> {
                return arg.get(node, arguments);
            }
            case null, default -> {
                return value;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private TemplateProcessingResult processTemplates(String node,
                                                      Object rawTemplate,
                                                      Map<ArgumentString, Object> input,
                                                      Map<String, TemplateArgument> parentArguments) {
        int inputKeys = input.size() - 1;
        // 先获取template节点下所有的模板
        List<ArgumentString> templateIds = MiscUtils.getAsList(rawTemplate, ArgumentString.class);
        List<Object> templateList = new ObjectArrayList<>(templateIds.size());

        // 获取arguments
        Map<String, TemplateArgument> arguments;
        if (inputKeys > 0) {
            Object argument = input.get(ARGUMENTS);
            if (argument != null) {
                inputKeys--;
                // 将本节点下的参数与父参数合并
                arguments = mergeArguments(node + ".arguments", (Map<ArgumentString, Object>) argument, parentArguments);
            } else {
                arguments = parentArguments;
            }
        } else {
            arguments = parentArguments;
        }

        // 获取处理后的template
        for (int i = 0, size = templateIds.size(); i < size; i++) {
            ArgumentString templateId = templateIds.get(i);
            String newNode = node + ".template[" + i + "]";
            Object parsedTemplateId = templateId.get(newNode, parentArguments);
            if (parsedTemplateId == null) continue; // 忽略被null掉的模板
            Object template = ((TemplateManagerImpl) INSTANCE).templates.get(Key.of(parsedTemplateId.toString()));
            if (template == null) {
                throw new KnownResourceException("resource.template.invalid_template", newNode, parsedTemplateId.toString());
            }
            Object processedTemplate = processUnknownValue(newNode, template, arguments);
            if (processedTemplate != null) {
                templateList.add(processedTemplate);
            }
        }

        // 获取overrides
        Object override = null;
        Object merge = null;
        if (inputKeys > 0) {
            override = input.get(OVERRIDES);
            if (override != null) {
                inputKeys--;
                override = processUnknownValue(node + ".overrides", override, arguments);
            }
            if (inputKeys > 0) {
                // 获取merges
                merge = input.get(MERGES);
                if (merge != null) {
                    inputKeys--;
                    merge = processUnknownValue(node + ".merges", merge, arguments);
                }
            }
        }

        // 有其他意外参数
        if (inputKeys > 0) {
            Map<String, Object> merges = new LinkedHashMap<>();
            // 会不会有一种可能，有笨比用户把模板和普通配置混合在了一起？再次遍历input后处理。
            for (Map.Entry<ArgumentString, Object> inputEntry : input.entrySet()) {
                ArgumentString inputKey = inputEntry.getKey();
                if (NON_TEMPLATE_ARGUMENTS.contains(inputKey)) continue;
                String newNode = node + "." + inputKey.rawValue();
                Object key = inputKey.get(newNode, parentArguments);
                if (key != null) {
                    merges.put(key.toString(), processUnknownValue(newNode, inputEntry.getValue(), arguments));
                }
            }
            if (merge instanceof Map<?, ?> rawMerges) {
                merges.putAll(MiscUtils.castToMap(rawMerges));
            }
            return new TemplateProcessingResult(
                    templateList,
                    override,
                    merges,
                    arguments
            );
        } else {
            return new TemplateProcessingResult(
                    templateList,
                    override,
                    merge,
                    arguments
            );
        }
    }

    // 合并参数
    @SuppressWarnings("unchecked")
    private Map<String, TemplateArgument> mergeArguments(@NotNull String node,
                                                         @NotNull Map<ArgumentString, Object> childArguments,
                                                         @NotNull Map<String, TemplateArgument> parentArguments) {
        Map<String, TemplateArgument> result = new LinkedHashMap<>(parentArguments);
        for (Map.Entry<ArgumentString, Object> argumentEntry : childArguments.entrySet()) {
            ArgumentString key = argumentEntry.getKey();
            Object placeholderObj = key.get(node, result);
            if (placeholderObj == null) continue;
            String placeholder = placeholderObj.toString();
            // 父亲参数最大
            if (result.containsKey(placeholder)) continue;
            Object processedPlaceholderValue = processUnknownValue(node, argumentEntry.getValue(), result);
            switch (processedPlaceholderValue) {
                case Map<?, ?> map -> {
                     result.put(placeholder, TemplateArguments.fromConfig(ConfigSection.of(node + "." + key.rawValue(), map)));
                }
                case List<?> listArgument -> {
                    result.put(placeholder, ListTemplateArgument.list((List<Object>) listArgument));
                }
                case null -> result.put(placeholder, NullTemplateArgument.INSTANCE);
                default -> result.put(placeholder, ObjectTemplateArgument.object(processedPlaceholderValue));
            }
        }
        return result;
    }

    private record TemplateProcessingResult(
            List<Object> templates,
            Object overrides,
            Object merges,
            Map<String, TemplateArgument> arguments
    ) {
    }

    @SuppressWarnings("unchecked")
    private static void deepMergeMaps(Map<String, Object> baseMap, Map<String, Object> mapToMerge) {
        for (Map.Entry<String, Object> entry : mapToMerge.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.length() > 2 && key.charAt(0) == '$' && key.charAt(1) == '$') {
                baseMap.put(key.substring(1), value);
            } else {
                if (baseMap.containsKey(key)) {
                    Object existingValue = baseMap.get(key);
                    if (existingValue instanceof Map && value instanceof Map) {
                        deepMergeMaps(MiscUtils.castToMap(existingValue), MiscUtils.castToMap(value));
                    } else if (existingValue instanceof List && value instanceof List) {
                        List<Object> existingList = (List<Object>) existingValue;
                        List<Object> newList = (List<Object>) value;
                        existingList.addAll(newList);
                    } else {
                        baseMap.put(key, value);
                    }
                } else {
                    baseMap.put(key, value);
                }
            }
        }
    }
}
