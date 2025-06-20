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
        if (node instanceof MappingNode mappingNode) {
            if (isValueSelectorNode(mappingNode)) {
                // 场景B: 这是一个值选择器，解析它以获得单个值
                return constructVersionedValue(mappingNode);
            }
        }
        // 对于所有其他情况 (包括需要合并的Map)，使用默认的构造逻辑
        // super.constructObject 会最终调用我们重写的 constructMapping
        return super.constructObject(node);
    }

    /**
     * 场景A (块合并): 构造一个Map，同时处理其中的版本化块合并。
     */
    @Override
    protected Map<Object, Object> constructMapping(MappingNode node) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (NodeTuple tuple : node.getValue()) {
            Node keyNode = tuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode)) continue;

            String key = constructScalar((ScalarNode) keyNode);

            if (key.startsWith(VERSION_PREFIX)) {
                String versionSpec = key.substring(VERSION_PREFIX.length());
                if (isVersionMatch(versionSpec)) {
                    Node valueNode = tuple.getValueNode();
                    if (valueNode instanceof MappingNode) {
                        map.putAll(constructMapping((MappingNode) valueNode));
                    } else {
                        logWarning("versioned_key_not_a_map", key, valueNode);
                    }
                }
            } else {
                Node valueNode = tuple.getValueNode();
                Object value = constructObject(valueNode);
                Object previous = map.put(key, value);
                if (previous != null) {
                    logWarning("duplicated_key", key, keyNode);
                }
            }
        }
        return map;
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