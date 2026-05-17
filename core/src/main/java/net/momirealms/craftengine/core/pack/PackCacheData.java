package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public final class PackCacheData {
    private final Set<Path> externalZips;
    private final Set<Path> externalFolders;

    PackCacheData(@NotNull CraftEngine plugin) {
        this.externalFolders = Config.foldersToMerge().stream()
                .map(it -> plugin.dataFolderPath().getParent().resolve(it))
                .filter(Files::exists)
                .collect(Collectors.toSet());
        this.externalZips = Config.zipsToMerge().stream()
                .map(it -> plugin.dataFolderPath().getParent().resolve(it))
                .filter(Files::exists)
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().endsWith(".zip"))
                .collect(Collectors.toSet());
    }

    @NotNull
    public Set<Path> externalFolders() {
        return this.externalFolders;
    }

    @NotNull
    public Set<Path> externalZips() {
        return this.externalZips;
    }
}
