package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;

public class TemplateManagerImpl implements TemplateManager {
    private final Map<Key, Object> templates = new HashMap<>();
    private final static Set<String> NON_TEMPLATE_KEY = new HashSet<>(Set.of(TEMPLATE, ARGUMENTS, OVERRIDES));
    private final TemplateParser templateParser;

    public TemplateManagerImpl() {
        this.templateParser = new TemplateParser();
    }

    public class TemplateParser implements ConfigParser {
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
        public boolean supportsParsingObject() {
            return true;
        }

        @Override
        public void parseObject(Pack pack, Path path, Key id, Object obj) {
            if (templates.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.template.duplicate", path.toString(), id.toString());
            }
            templates.put(id, obj);
        }
    }

    @Override
    public void unload() {
        this.templates.clear();
    }

    @Override
    public ConfigParser parser() {
        return this.templateParser;
    }

    @Override
    public Map<String, Object> applyTemplates(Map<String, Object> input) {
        Objects.requireNonNull(input, "Input must not be null");
        Map<String, Object> result = new LinkedHashMap<>();
        processMap(input, Collections.emptyMap(), (obj) -> {
            // 当前位于根节点下，如果下一级就是模板，则应把模板结果与当前map合并
            // 如果模板结果不是map，则为非法值，因为不可能出现类似于下方的配置
            // items:
            //   test:invalid: 111
            if (obj instanceof Map<?,?> mapResult) {
                result.putAll(MiscUtils.castToMap(mapResult, false));
            } else {
                throw new IllegalArgumentException("Invalid template used. Input: " + GsonHelper.get().toJson(input) + ". Template: " + GsonHelper.get().toJson(obj));
            }
        });
        return result;
    }

    // 对于处理map，只有input是已知map，而返回值可能并不是
    private void processMap(Map<String, Object> input,
                            Map<String, TemplateArgument> parentArguments,
                            // 只有当前为模板的时候，才会调用callback
                            Consumer<Object> processCallBack) {
        // 传入的input是否含有template，这种情况下，返回值有可能是非map
        if (input.containsKey(TEMPLATE)) {
            TemplateProcessingResult processingResult = processTemplates(input, parentArguments);
            List<Object> templates = processingResult.templates();
            // 你敢保证template里没有template吗？
            List<Object> processedTemplates = new ArrayList<>();
            // 先递归处理后再合并
            for (Object template : templates) {
                processUnknownTypeMember(template, processingResult.arguments(), processedTemplates::add);
            }
            if (processedTemplates.isEmpty()) {
                return;
            }
            Object firstTemplate = processedTemplates.get(0);
            // 对于map和list，应当对多模板合并
            if (firstTemplate instanceof Map<?,?>) {
                Map<String, Object> results = new LinkedHashMap<>();
                // 仅仅合并list
                for (Object processedTemplate : processedTemplates) {
                    if (processedTemplate instanceof Map<?, ?> anotherMap) {
                        results.putAll(MiscUtils.castToMap(anotherMap, false));
                    }
                }
                results.putAll(processingResult.overrides());
                processCallBack.accept(results);
            } else if (firstTemplate instanceof List<?>) {
                List<Object> results = new ArrayList<>();
                // 仅仅合并list
                for (Object processedTemplate : processedTemplates) {
                    if (processedTemplate instanceof List<?> anotherList) {
                        results.addAll(anotherList);
                    }
                }
                processCallBack.accept(results);
            } else {
                // 其他情况下应当忽略其他的template
                processCallBack.accept(firstTemplate);
            }
        } else {
            // 如果不是模板，则返回值一定是map
            // 依次处理map下的每个参数
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> inputEntry : input.entrySet()) {
                processUnknownTypeMember(inputEntry.getValue(), parentArguments, (processed) -> result.put(inputEntry.getKey(), processed));
            }
            processCallBack.accept(result);
        }
    }

    // 处理一个类型未知的值，本方法只管将member处理好后，传递回调用者
    private void processUnknownTypeMember(Object member,
                                          Map<String, TemplateArgument> parentArguments,
                                          Consumer<Object> processCallback) {
        if (member instanceof Map<?,?> innerMap) {
            // map下面还是个map吗？这并不一定
            // 比如
            // a:
            //   template: xxx
            // 这时候a并不一定是map，最终类型取决于template，那么应当根据template的结果进行调整，所以我们继续交给上方方法处理
            processMap(MiscUtils.castToMap(innerMap, false), parentArguments, processCallback);
        } else if (member instanceof List<?> innerList) {
            // map 下面是个list，那么对下面的每个成员再次处理
            List<Object> result = new ArrayList<>();
            for (Object item : innerList) {
                // 处理完以后，加入到list内
                processUnknownTypeMember(item, parentArguments, result::add);
            }
            processCallback.accept(result);
        } else if (member instanceof String possibleArgument) {
            // 如果是个string，其可能是 {xxx} 的参数，那么就尝试应用参数后再返回
            processCallback.accept(applyArgument(possibleArgument, parentArguments));
        } else {
            // 对于其他值，直接处理
            processCallback.accept(member);
        }
    }

    private TemplateProcessingResult processTemplates(Map<String, Object> input,
                                                      Map<String, TemplateArgument> parentArguments) {
        // 先获取template节点下所有的模板
        List<String> templateIds = MiscUtils.getAsStringList(input.get(TEMPLATE));
        List<Object> templateList = new ArrayList<>();
        for (String templateId : templateIds) {
            // 如果模板id被用了参数，则应先应用参数后再查询模板
            Object actualTemplate = templateId.contains(LEFT_BRACKET) && templateId.contains(RIGHT_BRACKET) ? applyArgument(templateId, parentArguments) : templateId;
            if (actualTemplate == null) continue; // 忽略被null掉的模板
            Object template = Optional.ofNullable(templates.get(Key.of(actualTemplate.toString())))
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + actualTemplate));
            templateList.add(template);
        }
        // 将本节点下的参数与父参数合并
        Map<String, TemplateArgument> arguments = mergeArguments(
                MiscUtils.castToMap(input.getOrDefault(ARGUMENTS, Collections.emptyMap()), false),
                parentArguments
        );
        // 对overrides参数应用 本节点 + 父节点 参数
        Map<String, Object> overrides = new LinkedHashMap<>();
        processMap(MiscUtils.castToMap(input.getOrDefault(OVERRIDES, Map.of()), false), arguments, (obj) -> {
            // 如果overrides的下一级就是一个模板，则模板必须为map类型
            if (obj instanceof Map<?,?> mapResult) {
                overrides.putAll(MiscUtils.castToMap(mapResult, false));
            } else {
                throw new IllegalArgumentException("Invalid template used. Input: " + GsonHelper.get().toJson(input) + ". Template: " + GsonHelper.get().toJson(obj));
            }
        });
        // 会不会有一种可能，有笨比用户不会使用overrides，把模板和普通配置混合在了一起？再次遍历input后处理
        for (Map.Entry<String, Object> inputEntry : input.entrySet()) {
            String inputKey = inputEntry.getKey();
            if (NON_TEMPLATE_KEY.contains(inputKey)) continue;
            // 处理那些overrides
            processUnknownTypeMember(inputEntry.getValue(), arguments, (processed) -> overrides.put(inputKey, processed));
        }
        // 返回处理结果
        return new TemplateProcessingResult(
                templateList,
                overrides,
                arguments
        );
    }

    // 合并参数
    private Map<String, TemplateArgument> mergeArguments(@NotNull Map<String, Object> rawChildArguments,
                                                         @NotNull Map<String, TemplateArgument> parentArguments) {
        Map<String, TemplateArgument> result = new HashMap<>(parentArguments);
        // 我们遍历一下当前节点下的所有参数，这些参数可能含有内嵌参数。所以需要对参数map先处理一次后再合并
        // arguments:
        //   argument_1: "{parent_argument}"
        for (Map.Entry<String, Object> argumentEntry : rawChildArguments.entrySet()) {
            // 获取最终的string形式参数
            String placeholder = LEFT_BRACKET + argumentEntry.getKey() + RIGHT_BRACKET;
            // 父亲参数最大
            if (result.containsKey(placeholder)) continue;
            Object rawArgument = argumentEntry.getValue();
            if (rawArgument instanceof Map<?,?> mapArgument) {
                // 此参数是一个map，那么对map应用模板，然后再根据map是否含有type等参数，判别其是否为带名特殊参数
                Map<String, Object> nestedResult = new LinkedHashMap<>();
                processMap(MiscUtils.castToMap(mapArgument, false), parentArguments, (obj) -> {
                    // 如果有人往arguments下塞了一个模板，则模板类型应为map
                    if (obj instanceof Map<?,?> mapResult) {
                        nestedResult.putAll(MiscUtils.castToMap(mapResult, false));
                    } else {
                        throw new IllegalArgumentException("Invalid template used. Input: " + GsonHelper.get().toJson(mapArgument) + ". Template: " + GsonHelper.get().toJson(obj));
                    }
                });
                result.put(placeholder, TemplateArguments.fromMap(nestedResult));
            } else if (rawArgument instanceof List<?> listArgument) {
                // 此参数是一个list，那么只需要应用模板即可
                List<Object> nestedResult = new ArrayList<>();
                for (Object item : listArgument) {
                    processUnknownTypeMember(item, parentArguments, nestedResult::add);
                }
                result.put(placeholder, new ListTemplateArgument(nestedResult));
            } else if (rawArgument == null) {
                // 使用 null 覆写其父参数内容
                result.put(placeholder, NullTemplateArgument.INSTANCE);
            } else if (rawArgument instanceof Number number) {
                result.put(placeholder, new ObjectTemplateArgument(number));
            } else {
                // 将参数字符串化后，应用参数再放入
                Object applied = applyArgument(rawArgument.toString(), parentArguments);
                result.put(placeholder, new ObjectTemplateArgument(applied));
            }
        }
        return result;
    }

    // 将某个输入变成最终的结果，可以是string->string，也可以是string->map/list
    private Object applyArgument(String input, Map<String, TemplateArgument> arguments) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = ARGUMENT_PATTERN.matcher(input);
        boolean first = true;
        while (matcher.find()) {
            String placeholder = matcher.group();
            Supplier<Object> replacer = arguments.get(placeholder);
            if (replacer == null) {
                matcher.appendReplacement(result, placeholder);
                continue;
            }
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
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private record TemplateProcessingResult(
            List<Object> templates,
            Map<String, Object> overrides,
            Map<String, TemplateArgument> arguments
    ) {}
}
