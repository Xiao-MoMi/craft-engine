package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.pack.bedrock.block.BlockRegistrationPlanWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemIcons;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlanWriter;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

public final class BedrockPackGenerator {

    public void generate(JavaResourcePack resources, ItemRegistrationPlan plan, Path output) throws IOException {
        Path destination = output.toAbsolutePath().normalize();
        Files.createDirectories(destination.getParent());
        if (plan.hasErrors()) {
            Path report = destination.resolveSibling(destination.getFileName() + ".report.json");
            ItemRegistrationPlanWriter.write(plan, report, new BedrockItemIcons.Result(Set.of(), Map.of(), Map.of(), Set.of()));
            throw new IOException("Bedrock item plan contains errors; the existing pack was not replaced. Report: " + report);
        }
        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            Path root = fileSystem.getPath("bedrock").toAbsolutePath();
            Path metadata = root.resolve(RegistrationPlanKind.ITEM.file());
            Files.createDirectories(metadata.getParent());
            BedrockItemIcons.Result generated = new BedrockItemIcons(resources).write(plan, root);
            ItemRegistrationPlanWriter.write(plan, metadata, generated);
            BlockRegistrationPlanWriter.write(generated, root.resolve(RegistrationPlanKind.BLOCK.file()));
            GsonHelper.writeJsonFile(resources.translations(), root.resolve(BedrockPackLayout.TRANSLATIONS));
            GsonHelper.writeJsonFile(this.manifest(), root.resolve("manifest.json"));
            try (InputStream icon = resources.openCustomResource("pack.png")) {
                if (icon != null) {
                    Files.copy(icon, root.resolve("pack_icon.png"));
                }
            }
            Path temporary = Files.createTempFile(destination.getParent(), "bedrock-pack-", ".tmp");
            try {
                ZipUtils.compress(root, temporary);
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private JsonObject manifest() {
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(0);
        version.add(0);

        JsonArray engineVersion = new JsonArray();
        engineVersion.add(1);
        engineVersion.add(21);
        engineVersion.add(0);

        JsonObject header = new JsonObject();
        header.addProperty("name", Config.bedrockPackName());
        header.addProperty("description", Config.bedrockPackDescription());
        header.addProperty("uuid", UUID.randomUUID().toString());
        header.add("version", version);
        header.add("min_engine_version", engineVersion);

        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.addProperty("uuid", UUID.randomUUID().toString());
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
