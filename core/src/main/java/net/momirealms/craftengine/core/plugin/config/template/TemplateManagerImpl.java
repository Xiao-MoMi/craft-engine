package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdObjectConfigParser;
import net.momirealms.craftengine.core.plugin.config.template.argument.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public final class TemplateManagerImpl implements TemplateManager {
    private static final ArgumentString TEMPLATE = ArgumentString.Literal.literal("template");
    private static final ArgumentString TEMPLATES = ArgumentString.Literal.literal("templates");
    private static final ArgumentString OVERRIDES = ArgumentString.Literal.literal("overrides");
    private static final ArgumentString ARGUMENTS = ArgumentString.Literal.literal("arguments");
    private static final ArgumentString MERGES = ArgumentString.Literal.literal("merges");
    private final static Set<ArgumentString> NON_TEMPLATE_ARGUMENTS = new HashSet<>(Set.of(TEMPLATE, TEMPLATES, ARGUMENTS, OVERRIDES, MERGES));

    private final Map<Key, Object> templates = new HashMap<>();
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

    public class TemplateParser extends IdObjectConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"templates", "template"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.TEMPLATE;
        }

        @Override
        public int count() {
            return TemplateManagerImpl.this.templates.size();
        }

        @Override
        public void parseObject(Pack pack, Path path, String node, Key id, Object obj) {
            if (TemplateManagerImpl.this.templates.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.template.duplicate");
            }
            // 预处理会将 string类型的键或值解析为ArgumentString，以加速模板应用。所以处理后不可能存在String类型。
            TemplateManagerImpl.this.templates.put(id, preprocessUnknownValue(obj));
        }
    }

    @Override
    public Object applyTemplates(Key id, Object input) {
        Object preprocessedInput = preprocessUnknownValue(input);
        return processUnknownValue(preprocessedInput, Map.of(
                "__NAMESPACE__", PlainStringTemplateArgument.plain(id.namespace()),
                "__ID__", PlainStringTemplateArgument.plain(id.value())
        ));
    }

    public Object preprocessUnknownValue(Object value) {
        switch (value) {
            case Map<?, ?> map -> {
                Map<String, Object> in = MiscUtils.castToMap(map, false);
                Map<ArgumentString, Object> out = new LinkedHashMap<>(map.size());
                for (Map.Entry<String, Object> entry : in.entrySet()) {
                    out.put(ArgumentString.preParse(entry.getKey()), preprocessUnknownValue(entry.getValue()));
                }
                return out;
            }
            case List<?> list -> {
                List<Object> objList = new ArrayList<>(list.size());
                for (Object o : list) {
                    objList.add(preprocessUnknownValue(o));
                }
                return objList;
            }
            case String string -> {
                return ArgumentString.preParse(string);
            }
            case null, default -> {
                return value;
            }
        }
    }

    // 对于处理map，只有input是已知map，而返回值可能并不是
    private Object processMap(Map<ArgumentString, Object> input,
                              Map<String, TemplateArgument> arguments) {
        // 传入的input是否含有template，这种情况下，返回值有可能是非map
        Object template = ResourceConfigUtils.get(input, TEMPLATE, TEMPLATES);
        if (template != null) {
            TemplateProcessingResult processingResult = processTemplates(template, input, arguments);
            List<Object> processedTemplates = processingResult.templates();
            if (!processedTemplates.isEmpty()) {
                // 先获取第一个模板的类型
                Object firstTemplate = processedTemplates.getFirst();
                // 如果是map，应当深度合并
                if (firstTemplate instanceof Map<?,?>) {
                    Map<String, Object> results = new LinkedHashMap<>();
                    for (Object processedTemplate : processedTemplates) {
                        if (processedTemplate instanceof Map<?, ?> map) {
                            MiscUtils.deepMergeMaps(results, MiscUtils.castToMap(map, false));
                        }
                    }
                    if (processingResult.overrides() instanceof Map<?, ?> overrides) {
                        results.putAll(MiscUtils.castToMap(overrides, false));
                    }
                    if (processingResult.merges() instanceof Map<?, ?> merges) {
                        MiscUtils.deepMergeMaps(results, MiscUtils.castToMap(merges, false));
                    }
                    return results;
                } else if (firstTemplate instanceof List<?>) {
                    List<Object> results = new ArrayList<>();
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
                if (processingResult.overrides() instanceof Map<?,?> overrides) {
                    Map<String, Object> output = new LinkedHashMap<>(MiscUtils.castToMap(overrides, false));
                    if (processingResult.merges() instanceof Map<?,?> merges) {
                        MiscUtils.deepMergeMaps(output, MiscUtils.castToMap(merges, false));
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
                Object key = inputEntry.getKey().get(arguments);
                // 如果key为null说明不插入此键
                if (key != null) {
                    result.put(key.toString(), processUnknownValue(inputEntry.getValue(), arguments));
                }
            }
            return result;
        }
    }

    // 处理一个类型未知的值，本方法只管将member处理好后，传递回调用者a
    @SuppressWarnings("unchecked")
    public Object processUnknownValue(Object value,
                                      Map<String, TemplateArgument> arguments) {
        switch (value) {
            case Map<?, ?> innerMap ->
            // map下面还是个map吗？这并不一定
            // 这时候并不一定是map，最终类型取决于template，那么应当根据template的结果进行调整，所以我们继续交给上方方法处理
            {
                return processMap((Map<ArgumentString, Object>) innerMap, arguments);
            }
            case List<?> innerList -> {
                List<Object> result = new ArrayList<>();
                for (Object item : innerList) {
                    result.add(processUnknownValue(item, arguments));
                }
                return result;
            }
            case ArgumentString arg -> {
                return arg.get(arguments);
            }
            case null, default -> {
                return value;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private TemplateProcessingResult processTemplates(Object rawTemplate,
                                                      Map<ArgumentString, Object> input,
                                                      Map<String, TemplateArgument> parentArguments) {
        int knownKeys = 1;
        // 先获取template节点下所有的模板
        List<ArgumentString> templateIds = MiscUtils.getAsList(rawTemplate, ArgumentString.class);
        List<Object> templateList = new ArrayList<>(templateIds.size());

        // 获取arguments
        Object argument = input.get(ARGUMENTS);
        boolean hasArgument = argument != null;
        if (hasArgument) knownKeys++;

        // 将本节点下的参数与父参数合并
        Map<String, TemplateArgument> arguments = hasArgument ? mergeArguments(
                (Map<ArgumentString, Object>) argument,
                parentArguments
        ) : parentArguments;

        // 获取处理后的template
        for (ArgumentString templateId : templateIds) {
            // 如果模板id被用了参数，则应先应用参数后再查询模板
            Object actualTemplate = templateId.get(parentArguments);
            if (actualTemplate == null) continue; // 忽略被null掉的模板
            Object template = Optional.ofNullable(((TemplateManagerImpl) INSTANCE).templates.get(Key.of(actualTemplate.toString())))
                    .orElseThrow(() -> new LocalizedResourceConfigException("warning.config.template.invalid", actualTemplate.toString()));
            Object processedTemplate = processUnknownValue(template, arguments);
            if (processedTemplate != null) templateList.add(processedTemplate);
        }

        // 获取overrides
        Object override = input.get(OVERRIDES);
        boolean hasOverrides = override != null;
        if (hasOverrides) {
            knownKeys++;
            override = processUnknownValue(override, arguments);
        }

        // 获取merges
        Object merge = input.get(MERGES);
        boolean hasMerges = merge != null;
        if (hasMerges) {
            knownKeys++;
            merge = processUnknownValue(merge, arguments);
        }

        // 有其他意外参数
        if (input.size() > knownKeys) {
            Map<String, Object> merges = new LinkedHashMap<>();
            // 会不会有一种可能，有笨比用户把模板和普通配置混合在了一起？再次遍历input后处理。
            for (Map.Entry<ArgumentString, Object> inputEntry : input.entrySet()) {
                ArgumentString inputKey = inputEntry.getKey();
                if (NON_TEMPLATE_ARGUMENTS.contains(inputKey)) continue;
                Object key = inputKey.get(parentArguments);
                if (key != null) {
                    merges.put(key.toString(), processUnknownValue(inputEntry.getValue(), arguments));
                }
            }
            if (hasMerges && merge instanceof Map<?, ?> rawMerges) {
                Map<String, Object> mergeMap = (Map<String, Object>) rawMerges;
                merges.putAll(mergeMap);
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
    private Map<String, TemplateArgument> mergeArguments(@NotNull Map<ArgumentString, Object> childArguments,
                                                                @NotNull Map<String, TemplateArgument> parentArguments) {
        Map<String, TemplateArgument> result = new LinkedHashMap<>(parentArguments);
        for (Map.Entry<ArgumentString, Object> argumentEntry : childArguments.entrySet()) {
            Object placeholderObj = argumentEntry.getKey().get(result);
            if (placeholderObj == null) continue;
            String placeholder = placeholderObj.toString();
            // 父亲参数最大
            if (result.containsKey(placeholder)) continue;
            Object processedPlaceholderValue = processUnknownValue(argumentEntry.getValue(), result);
            switch (processedPlaceholderValue) {
                case Map<?, ?> map -> result.put(placeholder, TemplateArguments.fromMap(MiscUtils.castToMap(map, false)));
                case List<?> listArgument -> result.put(placeholder, ListTemplateArgument.list((List<Object>) listArgument));
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
    ) {}
}
