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

package net.momirealms.craftengine.bukkit.entity.furniture;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.BiConsumer;

public class DismountListener1_20 implements Listener {
    private final BiConsumer<Player, Entity> consumer;

    public DismountListener1_20(BiConsumer<Player, Entity> consumer) {
        this.consumer = consumer;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDismount(org.spigotmc.event.entity.EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.consumer.accept(player, event.getDismounted());
        }
    }
}
