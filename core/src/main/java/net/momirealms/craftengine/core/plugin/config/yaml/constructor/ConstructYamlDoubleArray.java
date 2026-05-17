package net.momirealms.craftengine.core.plugin.config.yaml.constructor;

import org.snakeyaml.engine.v2.api.ConstructNode;
import org.snakeyaml.engine.v2.exceptions.ConstructorException;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

import java.util.List;

public class ConstructYamlDoubleArray extends ConstructYamlDouble implements ConstructNode {

    @Override
    public Object construct(Node node) {
        if (!(node instanceof SequenceNode seqNode)) {
            throw new ConstructorException("while constructing a double array", node.getStartMark(),
                    "expected a sequence but found " + node.getNodeType(), node.getEndMark());
        }

        List<Node> valueList = seqNode.getValue();
        double[] result = new double[valueList.size()];

        for (int i = 0; i < valueList.size(); i++) {
            Node item = valueList.get(i);
            if (!(item instanceof ScalarNode)) {
                throw new ConstructorException("while constructing a double array", node.getStartMark(),
                        "expected a scalar value in the sequence at index " + i, item.getStartMark());
            }

            String value = constructScalar(item);
            if (value.isEmpty()) {
                throw new ConstructorException("while constructing a double array", node.getStartMark(),
                        "found empty value at index " + i, item.getStartMark());
            }

            result[i] = parseDouble(value);
        }

        return result;
    }
}