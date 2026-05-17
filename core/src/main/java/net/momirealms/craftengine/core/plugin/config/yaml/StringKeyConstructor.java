package net.momirealms.craftengine.core.plugin.config.yaml;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.yaml.constructor.*;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.snakeyaml.engine.v2.api.ConstructNode;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.constructor.ConstructYamlNull;
import org.snakeyaml.engine.v2.constructor.StandardConstructor;
import org.snakeyaml.engine.v2.constructor.core.ConstructYamlCoreBool;
import org.snakeyaml.engine.v2.constructor.json.ConstructUuidClass;
import org.snakeyaml.engine.v2.constructor.json.ConstructYamlBinary;
import org.snakeyaml.engine.v2.nodes.*;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StringKeyConstructor extends StandardConstructor {
    private final Path path;
    private static final String VERSION_PREFIX = "$$";
    private static final String DEEP_KEY_SEPARATOR = "::";

    private static final String PREFIX = "tag:yaml.org,2002:";
    public static final Tag BYTE = new Tag(PREFIX + "byte");
    public static final Tag SHORT = new Tag(PREFIX + "short");
    public static final Tag LONG = new Tag(PREFIX + "long");
    public static final Tag DOUBLE = new Tag(PREFIX + "double");
    public static final Tag BYTE_ARRAY = new Tag(PREFIX + "ByteArray");
    public static final Tag INT_ARRAY = new Tag(PREFIX + "IntArray");
    public static final Tag DOUBLE_ARRAY = new Tag(PREFIX + "DoubleArray");
    public static final Tag LONG_ARRAY = new Tag(PREFIX + "LongArray");
    public static final Tag INT_LIST = new Tag(PREFIX + "IntList");
    public static final Tag DOUBLE_LIST = new Tag(PREFIX + "DoubleList");
    public static final Tag LONG_LIST = new Tag(PREFIX + "LongList");
    public static final Tag UUID = new Tag(UUID.class);

    public StringKeyConstructor(LoadSettings settings, Path path) {
        super(settings);
        this.path = path;

        this.tagConstructors.put(Tag.NULL, new ConstructYamlNull());
        this.tagConstructors.put(Tag.BOOL, new ConstructYamlCoreBool());
        this.tagConstructors.put(Tag.INT, new ConstructYamlInt());
        this.tagConstructors.put(Tag.FLOAT, new ConstructYamlFloat());
        this.tagConstructors.put(Tag.BINARY, new ConstructYamlBinary());
        this.tagConstructors.put(UUID, new ConstructUuidClass());
        this.tagConstructors.put(DOUBLE, new ConstructYamlDouble());
        this.tagConstructors.put(BYTE, new ConstructYamlByte());
        this.tagConstructors.put(SHORT, new ConstructYamlShort());
        this.tagConstructors.put(LONG, new ConstructYamlLong());
        this.tagConstructors.put(INT_ARRAY, new ConstructYamlIntArray());
        this.tagConstructors.put(BYTE_ARRAY, new ConstructYamlByteArray());
        this.tagConstructors.put(LONG_ARRAY, new ConstructYamlLongArray());
        this.tagConstructors.put(DOUBLE_ARRAY, new ConstructYamlDoubleArray());
        this.tagConstructors.put(INT_LIST, new ConstructYamlIntList());
        this.tagConstructors.put(LONG_LIST, new ConstructYamlLongList());
        this.tagConstructors.put(DOUBLE_LIST, new ConstructYamlDoubleList());
    }

    @Override
    protected Optional<ConstructNode> findConstructorFor(Node node) {
        Tag tag = node.getTag();
        ConstructNode constructNode = super.tagConstructors.get(tag);
        if (constructNode != null) {
            return Optional.of(constructNode);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Object construct(Node node) {
        if (node instanceof MappingNode mappingNode && isValueSelectorNode(mappingNode)) {
            return constructVersionedValue(mappingNode);
        }
        return super.construct(node);
    }

    @Override
    protected Map<Object, Object> constructMapping(MappingNode node) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (NodeTuple tuple : node.getValue()) {
            Node keyNode = tuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode scalarKey)) continue;

            String key = scalarKey.getValue();
            Node valueNode = tuple.getValueNode();

            if (key.startsWith(VERSION_PREFIX)) {
                processVersionedBlock(map, key, valueNode);
            } else if (key.contains(DEEP_KEY_SEPARATOR)) {
                processDeepKey(map, key, valueNode, keyNode);
            } else {
                processRegularKey(map, key, valueNode, keyNode);
            }
        }
        return map;
    }

    private void processVersionedBlock(Map<Object, Object> targetMap, String key, Node valueNode) {
        String versionSpec = key.substring(VERSION_PREFIX.length());
        if (isVersionMatch(versionSpec)) {
            if (valueNode instanceof MappingNode mappingNode) {
                Map<Object, Object> versionedMap = constructMapping(mappingNode);
                mergeMap(targetMap, versionedMap, "", valueNode);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processDeepKey(Map<Object, Object> rootMap, String fullKey, Node valueNode, Node keyNode) {
        String[] keyParts = fullKey.split(DEEP_KEY_SEPARATOR);
        Map<Object, Object> currentMap = rootMap;

        for (int i = 0; i < keyParts.length - 1; i++) {
            String keyPart = keyParts[i];
            Object existingValue = currentMap.get(keyPart);

            if (existingValue instanceof Map) {
                currentMap = (Map<Object, Object>) existingValue;
                continue;
            }

            if (existingValue != null) logWarning("inconsistent_value_type", keyPart, keyNode);

            Map<Object, Object> newMap = new LinkedHashMap<>();
            currentMap.put(keyPart, newMap);
            currentMap = newMap;
        }

        String finalKey = keyParts[keyParts.length - 1];
        Object newValue = construct(valueNode);
        setValueWithDuplicationCheck(currentMap, finalKey, newValue, fullKey, keyNode);
    }

    private void processRegularKey(Map<Object, Object> targetMap, String key, Node valueNode, Node keyNode) {
        Object newValue = construct(valueNode);
        setValueWithDuplicationCheck(targetMap, key, newValue, key, keyNode);
    }

    @SuppressWarnings("unchecked")
    private void setValueWithDuplicationCheck(Map<Object, Object> targetMap, String key, Object newValue, String fullKeyPath, Node keyNode) {
        Object existingValue = targetMap.get(key);
        if (existingValue == null) {
            targetMap.put(key, newValue);
        } else if (existingValue instanceof Map && newValue instanceof Map) {
            mergeMap((Map<Object, Object>) existingValue, (Map<Object, Object>) newValue, fullKeyPath, keyNode);
        } else {
            logWarning("duplicated_key", fullKeyPath, keyNode);
            targetMap.put(key, newValue);
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeMap(Map<Object, Object> target, Map<Object, Object> source, String parentPath, Node sourceNode) {
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            String key = entry.getKey().toString();
            Object sourceValue = entry.getValue();
            Object targetValue = target.get(key);
            String currentPath = parentPath.isEmpty() ? key : parentPath + DEEP_KEY_SEPARATOR + key;

            if (targetValue == null) {
                target.put(key, sourceValue);
            } else if (targetValue instanceof Map && sourceValue instanceof Map) {
                mergeMap((Map<Object, Object>) targetValue, (Map<Object, Object>) sourceValue, currentPath, sourceNode);
            } else {
                logWarning("duplicated_key", currentPath, sourceNode);
                target.put(key, sourceValue);
            }
        }
    }

    private boolean isVersionMatch(String versionSpec) {
        int index = versionSpec.indexOf('~');
        if (index == -1) {
            char firstChar = versionSpec.charAt(0);
            if (firstChar == '>') {
                int version = VersionHelper.parseVersionToInteger(versionSpec);
                return versionSpec.charAt(1) == '=' ? VersionHelper.version >= version : VersionHelper.version > version;
            } else if (firstChar == '<') {
                int version = VersionHelper.parseVersionToInteger(versionSpec);
                return versionSpec.charAt(1) == '=' ? VersionHelper.version <= version : VersionHelper.version < version;
            } else {
                return VersionHelper.parseVersionToInteger(versionSpec) == VersionHelper.version;
            }
        } else {
            int min = VersionHelper.parseVersionToInteger(versionSpec.substring(0, index));
            int max = VersionHelper.parseVersionToInteger(versionSpec.substring(index + 1));
            return VersionHelper.version >= min && VersionHelper.version <= max;
        }
    }

    private boolean isValueSelectorNode(MappingNode node) {
        if (node.getValue().isEmpty()) return false;
        for (NodeTuple tuple : node.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode scalarNode) {
                if (!scalarNode.getValue().startsWith(VERSION_PREFIX)) return false;
            } else return false;
        }
        return true;
    }

    private Object constructVersionedValue(MappingNode node) {
        Object fallbackValue = null;
        Object matchedValue = null;
        for (NodeTuple tuple : node.getValue()) {
            String key = ((ScalarNode) tuple.getKeyNode()).getValue();
            String versionSpec = key.substring(VERSION_PREFIX.length());
            if ("fallback".equals(versionSpec)) {
                fallbackValue = construct(tuple.getValueNode());
                continue;
            }
            if (isVersionMatch(versionSpec)) {
                matchedValue = construct(tuple.getValueNode());
            }
        }
        return matchedValue != null ? matchedValue : fallbackValue;
    }

    private void logWarning(String keyInLocale, String configKey, Node node) {
        if (this.path == null) return;
        int line = node.getStartMark().map(m -> m.getLine() + 1).orElse(-1);
        CraftEngine.instance().logger().warn(TranslationManager.instance().plainTranslation("yaml." + keyInLocale,
                this.path.toAbsolutePath().toString(),
                configKey,
                String.valueOf(line)
        ));
    }
}