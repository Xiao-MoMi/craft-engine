package net.momirealms.craftengine.core.plugin.config.yaml.constructor;

import org.snakeyaml.engine.v2.constructor.ConstructScalar;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

public class ConstructYamlFloat extends ConstructScalar {

    @Override
    public Object construct(Node node) {
        String value = ((ScalarNode) node).getValue();
        if (".inf".equals(value)) {
            return Float.POSITIVE_INFINITY;
        } else if ("-.inf".equals(value)) {
            return Float.NEGATIVE_INFINITY;
        } else if (".nan".equals(value)) {
            return Float.NaN;
        } else {
            return Double.valueOf(value).floatValue();
        }
    }
}
