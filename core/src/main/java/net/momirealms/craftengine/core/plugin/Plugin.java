package net.momirealms.craftengine.core.plugin;

import net.momirealms.craftengine.core.advancement.AdvancementManager;
import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.entity.culling.EntityCullingManager;
import net.momirealms.craftengine.core.entity.furniture.FurnitureManager;
import net.momirealms.craftengine.core.entity.projectile.ProjectileManager;
import net.momirealms.craftengine.core.entity.seat.SeatManager;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.recipe.RecipeManager;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.pack.PackManager;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.plugin.compatibility.CompatibilityManager;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.context.GlobalVariableManager;
import net.momirealms.craftengine.core.plugin.dependency.DependencyManager;
import net.momirealms.craftengine.core.plugin.gui.GuiManager;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManager;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.plugin.network.NetworkManager;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import net.momirealms.craftengine.core.sound.SoundManager;
import net.momirealms.craftengine.core.world.WorldManager;
import net.momirealms.craftengine.core.world.score.TeamManager;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public interface Plugin {

    InputStream resourceStream(String filePath);

    PluginLogger logger();

    ClassPathAppender sharedClassPathAppender();

    ClassPathAppender privateClassPathAppender();

    File dataFolderFile();

    Path dataFolderPath();

    boolean isReloading();

    boolean isInitializing();

    boolean isStopping();

    boolean isDisabled();

    DependencyManager dependencyManager();

    SchedulerAdapter scheduler();

    void saveResource(String filePath);

    String pluginVersion();

    String serverVersion();

    ItemManager itemManager();

    BlockManager blockManager();

    NetworkManager networkManager();

    FontManager fontManager();

    AdvancementManager advancementManager();

    Config config();

    TranslationManager translationManager();

    TemplateManager templateManager();

    FurnitureManager furnitureManager();

    PackManager packManager();

    RecipeManager recipeManager();

    <P extends Plugin, C> SenderFactory<P, C> senderFactory();

    WorldManager worldManager();

    ItemBrowserManager itemBrowserManager();

    GuiManager guiManager();

    SoundManager soundManager();

    LootManager lootManager();

    CompatibilityManager compatibilityManager();

    GlobalVariableManager globalVariableManager();

    ProjectileManager projectileManager();

    EntityCullingManager entityCullingManager();

    TeamManager teamManager();

    SeatManager seatManager();

    Platform platform();
}
