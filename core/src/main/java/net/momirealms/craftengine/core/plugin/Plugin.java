package net.momirealms.craftengine.core.plugin;

import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.entity.furniture.FurnitureManager;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.recipe.RecipeManager;
import net.momirealms.craftengine.core.pack.PackManager;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.dependency.DependencyManager;
import net.momirealms.craftengine.core.plugin.gui.GuiManager;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManager;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.plugin.network.NetworkManager;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import net.momirealms.craftengine.core.world.WorldManager;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Supplier;

public interface Plugin extends Reloadable {

    InputStream resourceStream(String filePath);

    PluginLogger logger();

    ClassPathAppender classPathAppender();

    File dataFolderFile();

    Path dataFolderPath();

    DependencyManager dependencyManager();

    <W> SchedulerAdapter<W> scheduler();

    void saveResource(String filePath);

    String pluginVersion();

    String serverVersion();

    <T> ItemManager<T> itemManager();

    BlockManager blockManager();

    NetworkManager networkManager();

    FontManager fontManager();

    ConfigManager configManager();

    TranslationManager translationManager();

    TemplateManager templateManager();

    FurnitureManager furnitureManager();

    PackManager packManager();

    <T> RecipeManager<T> recipeManager();

    SenderFactory<? extends Plugin, ?> senderFactory();

    WorldManager worldManager();

    ItemBrowserManager itemBrowserManager();

    GuiManager guiManager();

    void debug(Supplier<String> message);

    boolean isPluginEnabled(String plugin);

    String parse(Player player, String text);
}
