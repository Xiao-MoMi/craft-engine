package net.momirealms.craftengine.core.plugin.config.yaml.constructor;

import org.snakeyaml.engine.v2.constructor.ConstructScalar;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

public class ConstructYamlShort extends ConstructScalar {

    @Override
    public Object construct(Node node) {
        return Short.parseShort(((ScalarNode) node).getValue());
    }
}
