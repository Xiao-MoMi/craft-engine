package net.momirealms.craftengine.fabric.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("craft-engine-fabric-mod/config.yml");
    public static boolean enableNetwork = false;
    public static boolean enableCancelBlockUpdate = false;

    public static Screen getConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setSavingRunnable(ModConfig::saveConfig)
                .setTitle(Text.translatable("title.craftengine.config"));

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.craftengine.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("option.craftengine.enable_network")
                        .formatted(Formatting.WHITE),
                        enableNetwork)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> enableNetwork = newValue)
                .setTooltip(
                        Text.translatable("tooltip.craftengine.enable_network")
                                .formatted(Formatting.GRAY)
                )
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("option.craftengine.enable_cancel_block_update")
                        .formatted(Formatting.WHITE),
                        enableCancelBlockUpdate)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> enableCancelBlockUpdate = newValue)
                .setTooltip(
                        Text.translatable("tooltip.craftengine.enable_cancel_block_update")
                )
                .build()
        );

        return builder.build();
    }

    private static void saveConfig() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        var data = new java.util.HashMap<String, Object>();
        data.put("enable-network", ModConfig.enableNetwork);
        data.put("enable-cancel-block-update", ModConfig.enableCancelBlockUpdate);
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
