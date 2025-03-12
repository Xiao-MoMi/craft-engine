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

package net.momirealms.craftengine.core.item;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.minimessage.*;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemBuildContext implements MiniMessageTextContext {
    public static final ItemBuildContext EMPTY = new ItemBuildContext(null, ContextHolder.EMPTY);
    private final Player player;
    private final ContextHolder contexts;
    private TagResolver[] tagResolvers;

    public ItemBuildContext(@Nullable Player player, @NotNull ContextHolder contexts) {
        this.player = player;
        this.contexts = contexts;
    }

    @NotNull
    public static ItemBuildContext of(@Nullable Player player, @NotNull ContextHolder contexts) {
        return new ItemBuildContext(player, contexts);
    }

    @Nullable
    public Player player() {
        return this.player;
    }

    @NotNull
    public ContextHolder contexts() {
        return this.contexts;
    }

    @NotNull
    public TagResolver[] tagResolvers() {
        if (this.tagResolvers == null) {
            this.tagResolvers = new TagResolver[]{ShiftTag.INSTANCE, ImageTag.INSTANCE, new PlaceholderTag(this.player), new I18NTag(this), new NamedArgumentTag(this)};
        }
        return this.tagResolvers;
    }
}
