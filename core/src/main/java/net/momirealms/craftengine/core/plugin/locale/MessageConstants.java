/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    TranslatableComponent.Builder COMMAND_ITEM_GET_SUCCESS = Component.translatable().key("command.item.get.success");
    TranslatableComponent.Builder COMMAND_ITEM_GET_FAILURE_NOT_EXIST = Component.translatable().key("command.item.get.failure.not_exist");
    TranslatableComponent.Builder COMMAND_ITEM_GIVE_SUCCESS_SINGLE = Component.translatable().key("command.item.give.success.single");
    TranslatableComponent.Builder COMMAND_ITEM_GIVE_SUCCESS_MULTIPLE = Component.translatable().key("command.item.give.success.multiple");
    TranslatableComponent.Builder COMMAND_ITEM_GIVE_FAILURE_NOT_EXIST = Component.translatable().key("command.item.give.failure.not_exist");
}
