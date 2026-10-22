package net.momirealms.craftengine.bedrock.geyser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserRegistrationPlanReader {

    private GeyserRegistrationPlanReader() {
    }

    @Nullable
    public static JsonObject readPack(Path path, RegistrationPlanKind kind) throws IOException {
        try (ZipFile archive = new ZipFile(path.toFile())) {
            ZipEntry entry = archive.getEntry(kind.file());
            if (entry == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonObject plan = JsonParser.parseReader(reader).getAsJsonObject();
                if (!plan.get(KIND).getAsString().equals(kind.id()) || plan.get(FORMAT_VERSION).getAsInt() != kind.formatVersion()) {
                    throw new IllegalArgumentException("Unsupported registration plan format: " + kind.file());
                }
                return plan;
            }
        }
    }

}
