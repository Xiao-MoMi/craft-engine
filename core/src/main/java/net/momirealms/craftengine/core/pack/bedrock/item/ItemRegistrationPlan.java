package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ItemRegistrationPlan(List<Entry> entries, List<Diagnostic> diagnostics) {

    public ItemRegistrationPlan {
        entries = List.copyOf(entries);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean hasErrors() {
        return this.diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == Severity.ERROR);
    }

    public record Entry(Key bedrockId,
                        String icon,
                        Key javaItemType,
                        Key definitionId,
                        ItemDefinitionBinding definition,
                        ItemRegistrationProperties properties,
                        Map<Key, JsonElement> componentOverrides,
                        Set<Key> removedComponents,
                        ResolvedItemBinding source) {

        public Entry {
            componentOverrides = Map.copyOf(componentOverrides);
            removedComponents = Set.copyOf(removedComponents);
        }
    }

    public record Diagnostic(Severity severity,
                             String code,
                             Key javaItemType,
                             @Nullable Key definitionId,
                             List<Key> itemIds,
                             List<String> details) {

        public Diagnostic {
            itemIds = List.copyOf(itemIds);
            details = List.copyOf(details);
        }
    }

    public enum Severity {
        INFO, WARNING, ERROR
    }
}
