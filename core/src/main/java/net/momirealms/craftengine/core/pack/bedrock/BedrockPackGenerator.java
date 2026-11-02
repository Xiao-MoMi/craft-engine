package net.momirealms.craftengine.core.pack.bedrock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.pack.bedrock.block.BedrockWorldBlocks;
import net.momirealms.craftengine.core.pack.bedrock.block.BlockRegistrationPlanWriter;
import net.momirealms.craftengine.core.pack.bedrock.block.WorldBlockSource;
import net.momirealms.craftengine.core.pack.bedrock.font.BedrockFontGenerator;
import net.momirealms.craftengine.core.pack.bedrock.gui.BedrockGuiGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlanWriter;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public final class BedrockPackGenerator {

    private BedrockPackGenerator() {
    }

    public static void generate(JavaResourcePack resources, ItemRegistrationPlan plan, List<WorldBlockSource> blocks, Path packOutput, Path dataOutput) throws IOException {
        Path packDestination = packOutput.toAbsolutePath().normalize();
        Path dataDestination = dataOutput.toAbsolutePath().normalize();
        Files.createDirectories(packDestination.getParent());
        Files.createDirectories(dataDestination.getParent());

        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            Path root = fileSystem.getPath("bedrock").toAbsolutePath();
            Path itemPlanPath = root.resolve(RegistrationPlanKind.ITEM.file());
            Files.createDirectories(itemPlanPath.getParent());

            JavaModelResources modelResources = new JavaModelResources(resources);
            // 物品
            BedrockItemResourceGenerator.Result generatedItems = BedrockItemResourceGenerator.write(modelResources, plan, root);
            ItemRegistrationPlanWriter.write(plan, itemPlanPath, generatedItems);
            // 方块
            Path blockPlanPath = root.resolve(RegistrationPlanKind.BLOCK.file());
            JsonArray worldBlockEntries = BedrockWorldBlocks.write(modelResources, blocks, root);
            BlockRegistrationPlanWriter.write(generatedItems, worldBlockEntries, blockPlanPath);
            // 翻译文件
            GsonHelper.writeJsonFile(resources.translations(), root.resolve(BedrockPackLayout.TRANSLATIONS));
            // 字体文件
            BedrockFontGenerator.write(resources, root);
            // 容器
            BedrockGuiGenerator.write(resources, root);

            // 元数据
            JsonObject manifest = manifest();
            GsonHelper.writeJsonFile(manifest, root.resolve("manifest.json"));

            // 复制 pack 图标
            try (InputStream icon = resources.openCustomResource("pack.png")) {
                if (icon != null) {
                    Files.copy(icon, root.resolve("pack_icon.png"));
                }
            }

            // 最终打包
            Path temporaryPack = Files.createTempFile(packDestination.getParent(), "bedrock-pack-", ".tmp");
            Path temporaryData = Files.createTempFile(dataDestination.getParent(), "bedrock-data-", ".tmp");
            try {
                // 将扩展数据复制到新文件夹打包
                Path data = fileSystem.getPath("extension-data").toAbsolutePath();
                for (String file : BedrockPackFiles.SERVER_FILES) {
                    Path target = data.resolve(file);
                    Files.createDirectories(target.getParent());
                    Files.move(root.resolve(file), target);
                }
                GsonHelper.writeJsonFile(manifest, data.resolve("manifest.json"));
                ZipUtils.compress(root, temporaryPack);
                ZipUtils.compress(data, temporaryData);
                Files.move(temporaryPack, packDestination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temporaryData, dataDestination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporaryPack);
                Files.deleteIfExists(temporaryData);
            }
        }
    }

    private static JsonObject manifest() {
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
