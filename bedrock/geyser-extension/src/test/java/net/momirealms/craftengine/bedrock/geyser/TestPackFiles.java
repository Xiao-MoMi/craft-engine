package net.momirealms.craftengine.bedrock.geyser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class TestPackFiles {

    private TestPackFiles() {
    }

    public static JsonObject plan(RegistrationPlanKind kind, String entries) {
        JsonObject plan = new JsonObject();
        plan.addProperty("kind", kind.id());
        plan.addProperty("format_version", kind.formatVersion());
        plan.addProperty("source_version", "1.21.4");
        plan.add("entries", JsonParser.parseString(entries));
        return plan;
    }

    public static void writePlan(Path path, RegistrationPlanKind kind, String entries) throws IOException {
        writeArchive(path, Map.of(kind.file(), plan(kind, entries).toString()));
    }

    public static void writeArchive(Path path, Map<String, String> entries) throws IOException {
        Files.createDirectories(path.getParent());
        try (ZipOutputStream archive = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                archive.putNextEntry(new ZipEntry(entry.getKey()));
                archive.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                archive.closeEntry();
            }
        }
    }
}
