package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class StringKeyConstructor extends SafeConstructor {
    private final Path path;
    private static final String VERSION_PREFIX = "$$";
    private static final String DEEP_KEY_SEPARATOR = "::";

    public StringKeyConstructor(Path path, LoaderOptions loaderOptions) {
        super(loaderOptions);
        this.path = path;
    }

    private boolean isVersionMatch(String versionSpec) {
        int index = versionSpec.indexOf('~');
        // 没有范围值
        if (index == -1) {
            char firstChar = versionSpec.charAt(0);
            if (firstChar == '>') {
                int version = VersionHelper.parseVersionToInteger(versionSpec);
                return versionSpec.charAt(1) == '=' ? VersionHelper.version() >= version : VersionHelper.version() > version;
            } else if (firstChar == '<') {
                int version = VersionHelper.parseVersionToInteger(versionSpec);
                return versionSpec.charAt(1) == '=' ? VersionHelper.version() <= version : VersionHelper.version() < version;
            } else {
                return VersionHelper.parseVersionToInteger(versionSpec) == VersionHelper.version();
            }
        } else {
            int min = VersionHelper.parseVersionToInteger(versionSpec.substring(0, index));
            int max = VersionHelper.parseVersionToInteger(versionSpec.substring(index + 1));
            return VersionHelper.version() >= min && VersionHelper.version() <= max;
        }
    }

    /**
     * Dispatcher: 决定一个节点是应该被解析为“版本化值”还是一个普通的Map。
     */
    @Override
    public Object constructObject(Node node) {
        if (node instanceof MappingNode mappingNode && isValueSelectorNode(mappingNode)) {
            return constructVersionedValue(mappingNode);
        }
        // 对于所有其他情况 (包括需要合并的Map)，使用默认的构造逻辑
        // super.constructObject 会最终调用我们重写的 constructMapping
        return super.constructObject(node);
    }


    /**
     * 场景A (块合并与路径展开): 构造一个Map，同时处理其中的版本化块合并和 `::` 分隔的深层键。
     */
    @Override
    protected Map<Object, Object> constructMapping(MappingNode node) {
        Map<Object, Object> map = new LinkedHashMap<>();

        for (NodeTuple tuple : node.getValue()) {
            Node keyNode = tuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode)) continue;
            Node valueNode = tuple.getValueNode();

            String key = constructScalar((ScalarNode) keyNode);

            // 处理 版本化块.
            if (key.startsWith(VERSION_PREFIX)) processVersionedBlock(map, key, valueNode);
            // 处理 深层键 -> {a::b::c: value} 和 {a::b: {c: value}}.
            else if (key.contains(DEEP_KEY_SEPARATOR)) processDeepKey(map, key, valueNode, keyNode);
            // 处理 正常键.
            else processRegularKey(map, key, valueNode, keyNode);
        }

        return map;
    }


    // 处理版本化块合并
    private void processVersionedBlock(Map<Object, Object> targetMap, String key, Node valueNode) {
        String versionSpec = key.substring(VERSION_PREFIX.length());

        if (isVersionMatch(versionSpec)) {
            if (valueNode instanceof MappingNode mappingNode) {
                Map<Object, Object> versionedMap = constructMapping(mappingNode);
                mergeMap(targetMap, versionedMap, "", valueNode);
            } else {
                logWarning("versioned_key_not_a_map", key, valueNode);
            }
        }
    }

    // 处理深层键
    @SuppressWarnings("unchecked")
    private void processDeepKey(Map<Object, Object> rootMap, String fullKey, Node valueNode, Node keyNode) {
        // 分割出不同的层级
        String[] keyParts = fullKey.split(DEEP_KEY_SEPARATOR);
        Map<Object, Object> currentMap = rootMap;

        // 创建必要的的中间层级(最后一个key不应遍历, 如aa::bb::cc, 只应创建aa和bb.)
        for (int i = 0; i < keyParts.length - 1; i++) {
            String keyPart = keyParts[i];
            Object existingValue = currentMap.get(keyPart);

            // 路径中的值
            if (existingValue instanceof Map) {
                currentMap = (Map<Object, Object>) existingValue;
                continue;
            }

            // 如果路径中存在一个非map的值, 这意味着
            // 当存在了 {aa: bb}, 又想要写入 {aa::bb::c: value} 时, 会触发这个警告, 然后会覆盖之前的.
            if (existingValue != null) logWarning("inconsistent_value_type", keyPart, keyNode);

            // 创建层级
            Map<Object, Object> newMap = new LinkedHashMap<>();
            currentMap.put(keyPart, newMap);
            currentMap = newMap;
        }

        // 这里再处理最后的 cc key.
        String finalKey = keyParts[keyParts.length - 1];
        Object newValue = constructObject(valueNode);
        String keyPath = String.join(DEEP_KEY_SEPARATOR, keyParts); // 构建完整的键路径字符串

        setValueWithDuplicationCheck(currentMap, finalKey, newValue, keyPath, keyNode);
    }

    // 处理普通键
    private void processRegularKey(Map<Object, Object> targetMap, String key, Node valueNode, Node keyNode) {
        Object newValue = constructObject(valueNode);
        setValueWithDuplicationCheck(targetMap, key, newValue, key, keyNode);
    }


    // 设置值并检查重复键
    @SuppressWarnings("unchecked")
    private void setValueWithDuplicationCheck(Map<Object, Object> targetMap, String key, Object newValue, String fullKeyPath, Node keyNode) {
        Object existingValue = targetMap.get(key);

        if (existingValue == null) {
            // 键不存在，直接设置.
            targetMap.put(key, newValue);
        } else if (existingValue instanceof Map && newValue instanceof Map) {
            // 两个都是Map，直接合并.
            mergeMap((Map<Object, Object>) existingValue, (Map<Object, Object>) newValue, fullKeyPath, keyNode);
        } else {
            // 存在重复键（至少一个不是Map）
            logWarning("duplicated_key", fullKeyPath, keyNode);
            targetMap.put(key, newValue);
        }
    }


    // 合并两个Map并检查重复键
    @SuppressWarnings("unchecked")
    private void mergeMap(Map<Object, Object> target, Map<Object, Object> source, String parentPath, Node sourceNode) {
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            String key = entry.getKey().toString();
            Object sourceValue = entry.getValue();
            Object targetValue = target.get(key);
            String currentPath = parentPath.isEmpty() ? key : parentPath + DEEP_KEY_SEPARATOR + key;

            // Map不存在该键，直接添加喵.
            if (targetValue == null) target.put(key, sourceValue);
            // 两个值都是Map，还需继续合并.
            else if (targetValue instanceof Map && sourceValue instanceof Map)
                mergeMap((Map<Object, Object>) targetValue, (Map<Object, Object>) sourceValue, currentPath, sourceNode);
            // 发现重复的键, 爆炸了喵.
            else {
                logWarning("duplicated_key", currentPath, sourceNode);
                target.put(key, sourceValue);
            }
        }
    }


    /**
     * 检查一个MappingNode是否是“值选择器”（即所有键都以 '$$' 开头）。
     */
    private boolean isValueSelectorNode(MappingNode node) {
        if (node.getValue().isEmpty()) {
            return false;
        }
        for (NodeTuple tuple : node.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode scalarNode) {
                String key = scalarNode.getValue();
                if (!key.startsWith(VERSION_PREFIX)) {
                    return false; // 发现一个普通键，因此它不是值选择器
                }
            } else {
                return false; // 键不是一个简单的字符串，不可能是值选择器
            }
        }
        return true; // 所有键都是版本化的
    }

    /**
     * 场景B (值选择): 从“值选择器”节点中解析出最终的单个值。
     */
    private Object constructVersionedValue(MappingNode node) {
        Object fallbackValue = null;
        Object matchedValue = null;
        // 遍历所有版本键，寻找匹配项
        for (NodeTuple tuple : node.getValue()) {
            String key = ((ScalarNode) tuple.getKeyNode()).getValue();
            String versionSpec = key.substring(VERSION_PREFIX.length());
            if ("fallback".equals(versionSpec)) {
                // 找到备用值，先存起来
                fallbackValue = constructObject(tuple.getValueNode());
                continue;
            }
            if (isVersionMatch(versionSpec)) {
                // 找到一个匹配项，因为YAML是顺序的，后面的会覆盖前面的
                matchedValue = constructObject(tuple.getValueNode());
            }
        }

        // 如果有精确匹配的值，则使用它；否则，使用备用值
        return matchedValue != null ? matchedValue : fallbackValue;
    }

    private void logWarning(String keyInLocale, String configKey, Node node) {
        if (this.path == null) return;
        TranslationManager.instance().log("warning.config.yaml." + keyInLocale,
                this.path.toAbsolutePath().toString(),
                configKey,
                String.valueOf(node.getStartMark().getLine() + 1));
    }
}