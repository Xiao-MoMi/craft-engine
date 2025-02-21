package net.momirealms.craftengine.core.plugin.locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public interface MessageConstants {
    TranslatableComponent.Builder COMMAND_RELOAD_FAILURE_IS_LOADING = Component.translatable().key("command.reload.failure.is_reloading");
    TranslatableComponent.Builder COMMAND_RELOAD_CONFIG_SUCCESS = Component.translatable().key("command.reload.config.success");
    TranslatableComponent.Builder COMMAND_RELOAD_CONFIG_FAILURE = Component.translatable().key("command.reload.config.failure");
    TranslatableComponent.Builder COMMAND_RELOAD_PACK_SUCCESS = Component.translatable().key("command.reload.pack.success");
    TranslatableComponent.Builder COMMAND_RELOAD_PACK_FAILURE = Component.translatable().key("command.reload.pack.failure");
    TranslatableComponent.Builder COMMAND_RELOAD_ALL_SUCCESS = Component.translatable().key("command.reload.all.success");
    TranslatableComponent.Builder COMMAND_RELOAD_ALL_FAILURE = Component.translatable().key("command.reload.all.failure");
}
