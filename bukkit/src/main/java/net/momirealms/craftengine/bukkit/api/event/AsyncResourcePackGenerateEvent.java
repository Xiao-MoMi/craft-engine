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

package net.momirealms.craftengine.bukkit.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class AsyncResourcePackGenerateEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Path generatedPackPath;
    private final Path zipFilePath;

    public AsyncResourcePackGenerateEvent(@NotNull Path generatedPackPath,
                                          @NotNull Path zipFilePath) {
        super(true);
        this.generatedPackPath = generatedPackPath;
        this.zipFilePath = zipFilePath;
    }

    @NotNull
    public Path resourcePackFolder() {
        return generatedPackPath;
    }

    @NotNull
    public Path zipFilePath() {
        return zipFilePath;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}
