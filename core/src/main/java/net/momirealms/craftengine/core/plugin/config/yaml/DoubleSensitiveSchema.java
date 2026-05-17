package net.momirealms.craftengine.core.plugin.config.yaml;

import org.snakeyaml.engine.v2.api.ConstructNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.schema.Schema;

import java.util.Map;

public final class DoubleSensitiveSchema implements Schema {
    public static final DoubleSensitiveSchema INSTANCE = new DoubleSensitiveSchema();

    private DoubleSensitiveSchema() {
    }

    @Override
    public ScalarResolver getScalarResolver() {
        return DoubleSensitiveScalarResolver.SUPPORT_MERGE;
    }

    @Override
    public Map<Tag, ConstructNode> getSchemaTagConstructors() {
        return Map.of();
    }
}
