package net.momirealms.craftengine.bedrock.geyser.item;

import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;

import java.util.List;

public record GeyserItemRegistrationResult(List<Registration> registrations, List<Diagnostic> diagnostics) {

    public GeyserItemRegistrationResult {
        registrations = List.copyOf(registrations);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean hasErrors() {
        return this.diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity().equals("ERROR"));
    }

    public record Registration(Identifier javaItem, CustomItemDefinition definition) {
    }

    public record Diagnostic(String severity, String bedrockId, String model, String definitionFile, String message) {
    }
}
