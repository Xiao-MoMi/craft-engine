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

package net.momirealms.craftengine.core.plugin.network;

import io.netty.channel.Channel;
import net.momirealms.craftengine.core.entity.player.Player;

import java.util.Collection;

public interface NetworkManager {
    void setUser(Channel channel, NetWorkUser user);

    NetWorkUser getUser(Channel channel);

    NetWorkUser removeUser(Channel channel);

    Channel getChannel(Player player);

    Collection<? extends NetWorkUser> onlineUsers();

    void init();

    void enable();

    void shutdown();
}
