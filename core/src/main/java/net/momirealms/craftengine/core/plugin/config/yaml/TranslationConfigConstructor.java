package net.momirealms.craftengine.core.plugin.config.yaml;

import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.constructor.StandardConstructor;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class TranslationConfigConstructor extends StandardConstructor {

    public TranslationConfigConstructor(LoadSettings settings) {
        super(settings);
    }

    @Override
    protected Map<Object, Object> constructMapping(MappingNode node) {
        Map<Object, Object> map = new LinkedHashMap<>();

        for (NodeTuple tuple : node.getValue()) {
            Node keyNode = tuple.getKeyNode();
            Node valueNode = tuple.getValueNode();
            if (!(keyNode instanceof ScalarNode scalarKey)) {
                continue;
            }

            String key = scalarKey.getValue();
            Object value = construct(valueNode);

            if (value instanceof List<?> list) {
                StringJoiner stringJoiner = new StringJoiner("<reset><newline>");
                for (Object str : list) {
                    stringJoiner.add(String.valueOf(str));
                }
                map.put(key, stringJoiner.toString());
            } else if (value != null) {
                map.put(key, value.toString());
            } else {
                map.put(key, null);
            }
        }
        return map;
    }
}