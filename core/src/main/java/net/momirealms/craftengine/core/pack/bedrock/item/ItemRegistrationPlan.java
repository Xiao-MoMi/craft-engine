package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ItemRegistrationPlan(List<Entry> entries, List<Diagnostic> diagnostics) {

    public ItemRegistrationPlan {
        entries = List.copyOf(entries);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean hasErrors() {
        return this.diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == Severity.ERROR);
    }

    public record Entry(Key bedrockId,  // SHA1 后的 craftengine id
                        Key javaItemType,
                        Key definitionId,
                        ItemDefinitionBinding definition,
                        ItemRegistrationProperties properties,
                        ResolvedItemBinding source) {
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
