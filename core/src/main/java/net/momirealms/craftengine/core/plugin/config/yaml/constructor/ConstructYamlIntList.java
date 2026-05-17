package net.momirealms.craftengine.core.plugin.config.yaml.constructor;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.snakeyaml.engine.v2.api.ConstructNode;
import org.snakeyaml.engine.v2.exceptions.ConstructorException;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

import java.util.List;

public class ConstructYamlIntList extends ConstructYamlInt implements ConstructNode {

    @Override
    public Object construct(Node node) {
        if (!(node instanceof SequenceNode seqNode)) {
            throw new ConstructorException("while constructing an int list", node.getStartMark(),
                    "expected a sequence but found " + node.getNodeType(), node.getEndMark());
        }

        List<Node> valueList = seqNode.getValue();
        IntArrayList result = new IntArrayList(valueList.size());

        for (int i = 0; i < valueList.size(); i++) {
            Node item = valueList.get(i);
            if (!(item instanceof ScalarNode)) {
                throw new ConstructorException("while constructing an int list", node.getStartMark(),
                        "expected a scalar value at index " + i, item.getStartMark());
            }

            String value = constructScalar(item);
            if (value.isEmpty()) {
                throw new ConstructorException("while constructing an int list", node.getStartMark(),
                        "found empty value", node.getStartMark());
            }

            result.add(createIntNumber(value).intValue());
        }
        return result;
    }
}