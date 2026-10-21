package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlanWriter;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.ZipUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class BedrockPackGenerator {

    public void generate(JavaResourcePack resources, ItemRegistrationPlan plan, Path output) throws IOException {
        Path destination = output.toAbsolutePath().normalize();
        Files.createDirectories(destination.getParent());
        if (plan.hasErrors()) {
            Path report = destination.resolveSibling(destination.getFileName() + ".report.json");
            ItemRegistrationPlanWriter.write(plan, report);
            throw new IOException("Bedrock item plan contains errors; the existing pack was not replaced. Report: " + report);
        }
        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            Path root = fileSystem.getPath("bedrock").toAbsolutePath();
            Path metadata = root.resolve(BedrockPackLayout.ITEM_REGISTRATION_PLAN);
            Files.createDirectories(metadata.getParent());
            ItemRegistrationPlanWriter.write(plan, metadata);
            GsonHelper.writeJsonFile(this.manifest(HashUtils.sha1(metadata)), root.resolve("manifest.json"));
            Path temporary = Files.createTempFile(destination.getParent(), "bedrock-pack-", ".tmp");
            try {
                ZipUtils.compress(root, temporary);
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private JsonObject manifest(String identity) {
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(0);
        version.add(0);
        JsonArray engineVersion = new JsonArray();
        engineVersion.add(1);
        engineVersion.add(21);
        engineVersion.add(0);
        JsonObject header = new JsonObject();
        header.addProperty("name", "CraftEngine Bedrock resources");
        header.addProperty("description", "Generated from the Java resource pack");
        header.addProperty("uuid", UUID.nameUUIDFromBytes(("craftengine:bedrock:1:" + identity).getBytes(StandardCharsets.UTF_8)).toString());
        header.add("version", version);
        header.add("min_engine_version", engineVersion);
        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.addProperty("uuid", UUID.nameUUIDFromBytes(("craftengine:bedrock:resources:1:" + identity).getBytes(StandardCharsets.UTF_8)).toString());
        module.add("version", version);
        JsonArray modules = new JsonArray();
        modules.add(module);
        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);
        manifest.add("header", header);
        manifest.add("modules", modules);
        return manifest;
    }
}
