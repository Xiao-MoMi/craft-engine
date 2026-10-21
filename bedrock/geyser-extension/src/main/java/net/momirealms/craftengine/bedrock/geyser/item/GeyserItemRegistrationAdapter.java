package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.CustomItemIcons;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserItemRegistrationAdapter {
    private final int sourceVersion;

    public GeyserItemRegistrationAdapter(String sourceVersion) {
        String[] parts = sourceVersion.split("\\.");
        this.sourceVersion = Integer.parseInt(parts[0]) * 10000 + Integer.parseInt(parts[1]) * 100 + (parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
        if (this.sourceVersion < 12104) {
            throw new IllegalArgumentException("Legacy model mapping is not implemented");
        }
    }

    @Nullable
    public static JsonObject readPack(Path path) throws IOException {
        try (ZipFile archive = new ZipFile(path.toFile())) {
            ZipEntry entry = archive.getEntry(ITEM_REGISTRATION_PLAN);
            if (entry == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonObject plan = JsonParser.parseReader(reader).getAsJsonObject();
                if (!plan.get(KIND).getAsString().equals(ITEM_REGISTRATION_PLAN_KIND) || plan.get(FORMAT_VERSION).getAsInt() != ITEM_REGISTRATION_PLAN_FORMAT_VERSION) {
                    throw new IllegalArgumentException("Unsupported registration plan format");
                }
                return plan;
            }
        }
    }

    public CustomItemDefinition convert(JsonObject entry) {
        Identifier bedrockId = Identifier.of(entry.get(BEDROCK_IDENTIFIER).getAsString());
        Identifier model = Identifier.of(entry.get(MODEL).getAsString());
        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockId, model);
        builder.bedrockOptions(
                CustomItemBedrockOptions.builder()
                        .icons(
                                CustomItemIcons.builder()
                                        .defaultIcon(entry.getAsJsonObject(RESOURCES).get(ICON).getAsString())
                                        .build()
                        )
        );
        for (Map.Entry<String, JsonElement> component : entry.getAsJsonObject(COMPONENTS).entrySet()) {
            JsonElement value = component.getValue().isJsonNull() ? null : component.getValue();
            GeyserItemComponentConverter.apply(builder, component.getKey(), value, this.sourceVersion);
        }
        return builder.build();
    }
}
