package net.momirealms.craftengine.core.plugin.config.yaml.constructor;

import org.snakeyaml.engine.v2.constructor.ConstructScalar;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

public class ConstructYamlDouble extends ConstructScalar {

    @Override
    public Object construct(Node node) {
        String value = ((ScalarNode) node).getValue();
        return parseDouble(value);
    }

    protected double parseDouble(String value) {
        if (".inf".equals(value)) {
            return Double.POSITIVE_INFINITY;
        } else if ("-.inf".equals(value)) {
            return Double.NEGATIVE_INFINITY;
        } else if (".nan".equals(value)) {
            return Double.NaN;
        } else {
            return Double.parseDouble(value);
        }
    }
}
