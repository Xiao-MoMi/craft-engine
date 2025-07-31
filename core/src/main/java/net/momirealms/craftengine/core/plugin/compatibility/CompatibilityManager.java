package net.momirealms.craftengine.core.plugin.compatibility;

import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.entity.player.Player;

import java.util.UUID;

public interface CompatibilityManager {

    void onLoad();

    void onEnable();

    void onDelayedEnable();

    void registerLevelerProvider(String plugin, LevelerProvider provider);

    void addLevelerExp(Player player, String plugin, String target, double value);

    int getLevel(Player player, String plugin, String target);

    ExternalModel createModel(String plugin, String id);

    int interactionToBaseEntity(int id);

    boolean hasPlaceholderAPI();

    boolean isPluginEnabled(String plugin);

    boolean hasPlugin(String plugin);

    String parse(Player player, String text);

    String parse(Player player1, Player player2, String text);

    int getPlayerProtocolVersion(UUID uuid);

    void executeMMSkill(String skill, float power, Player player);
}
