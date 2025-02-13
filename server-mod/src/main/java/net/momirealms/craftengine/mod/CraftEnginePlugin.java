package net.momirealms.craftengine.mod;

import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public final class CraftEnginePlugin implements IMixinConfigPlugin {
    public static final Logger LOGGER = Logger.getLogger(CraftEnginePlugin.class.getName());
    private static int vanillaRegistrySize;
    private static boolean isSuccessfullyRegistered = false;
    private static int maxChainUpdate = 32;

    public static void setVanillaRegistrySize(int vanillaRegistrySize) {
        CraftEnginePlugin.vanillaRegistrySize = vanillaRegistrySize;
    }

    public static void setIsSuccessfullyRegistered(boolean isSuccessfullyRegistered) {
        CraftEnginePlugin.isSuccessfullyRegistered = isSuccessfullyRegistered;
    }

    public static int maxChainUpdate() {
        return maxChainUpdate;
    }

    public static void setMaxChainUpdate(int maxChainUpdate) {
        CraftEnginePlugin.maxChainUpdate = maxChainUpdate;
    }

    @Override
    public void onLoad(final @NotNull String mixinPackage) {
    }

    @Override
    public @Nullable String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(@NotNull String targetClassName,
                                    @NotNull String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(@NotNull Set<String> myTargets,
                              @NotNull Set<String> otherTargets) {
    }

    @Override
    public @Nullable List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(@NotNull String targetClassName,
                         @NotNull ClassNode targetClass,
                         @NotNull String mixinClassName,
                         @NotNull IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(@NotNull String targetClassName,
                          @NotNull ClassNode targetClass,
                          @NotNull String mixinClassName,
                          @NotNull IMixinInfo mixinInfo) {
    }

    public static Path getPluginFolderPath() {
        ProtectionDomain protectionDomain = CraftEnginePlugin.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL jarUrl = codeSource.getLocation();
        try {
            return Paths.get(jarUrl.toURI()).getParent().getParent().resolve("plugins");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Path getCraftEngineMappingsPath() {
        return getPluginFolderPath()
                .resolve("CraftEngine")
                .resolve("mappings.yml");
    }

    public static Path getCraftEngineAdditionalBlocksPath() {
        return getPluginFolderPath()
                .resolve("CraftEngine")
                .resolve("additional-real-blocks.yml");
    }
}
