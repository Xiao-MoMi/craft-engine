package net.momirealms.craftengine.core.plugin;

import net.momirealms.craftengine.core.entity.furniture.AbstractExternalModel;
import net.momirealms.craftengine.core.entity.player.Player;

import java.util.UUID;

public interface CompatibilityManager {

    void onLoad();

    void onEnable();

    void onDelayedEnable();

    AbstractExternalModel createModelEngineModel(String id);

    AbstractExternalModel createBetterModelModel(String id);

    int interactionToBaseEntity(int id);

    boolean hasPlaceholderAPI();

    boolean isPluginEnabled(String plugin);

    boolean hasPlugin(String plugin);

    String parse(Player player, String text);

    String parse(Player player1, Player player2, String text);

    int getPlayerProtocolVersion(UUID uuid);
}
