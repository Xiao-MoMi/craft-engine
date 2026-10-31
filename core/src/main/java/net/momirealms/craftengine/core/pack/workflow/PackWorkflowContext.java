package net.momirealms.craftengine.core.pack.workflow;

import net.momirealms.craftengine.core.pack.PackGenerationOptions;
import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.nio.file.Path;

public interface PackWorkflowContext {

    CraftEngine plugin();

    Path resolvePath(String path);

    Path generatedPackPath();

    void generate(PackGenerationOptions options) throws Exception;

    void loadZip(String path) throws Exception;

    void export(String path) throws Exception;

    void generateBedrockPack(String input, String output, String dataOutput) throws Exception;

    void validatePack() throws Exception;

    void optimize() throws Exception;

    default void zip(String path, boolean protection) throws Exception {
        zip(path, protection, false);
    }

    void zip(String path, boolean protection, boolean storePng) throws Exception;

    void upload(String pack, String path) throws Exception;

    void sendPack(String pack) throws Exception;
}
